package com.dubreuia.core.component;

import com.dubreuia.model.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.PsiErrorElementUtil;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.dubreuia.core.component.SaveActionManager.LOGGER;
import static com.dubreuia.model.Action.noActionIfCompileErrors;

public class PsiFileValidator {

    private final Project project;
    private final Storage storage;

    private PsiFileValidator(Project project, Storage storage) {
        this.project = project;
        this.storage = storage;
    }

    public static PsiFileValidator of(Project project, Storage storage) {
        return new PsiFileValidator(project, storage);
    }

    public boolean isPsiFileEligible(PsiFile psiFile) {
        return psiFile != null
                && isProjectValid(project)
                && isPsiFileInProject(project, psiFile)
                && isPsiFileHasErrors(project, psiFile)
                && isPsiFileIncluded(psiFile)
                && isPsiFileFresh(psiFile)
                && isPsiFileValid(psiFile);
    }

    private boolean isProjectValid(Project project) {
        return project.isInitialized()
                && !project.isDisposed();
    }

    private boolean isPsiFileInProject(Project project, PsiFile file) {
        boolean inProject = ProjectRootManager.getInstance(project).getFileIndex().isInContent(file.getVirtualFile());
        if (!inProject) {
            LOGGER.debug("File " + file.getVirtualFile().getCanonicalPath() + " not in current project " + project);
        }
        return inProject;
    }

    private boolean isPsiFileHasErrors(Project project, PsiFile psiFile) {
        if (storage.isEnabled(noActionIfCompileErrors)) {
            return !PsiErrorElementUtil.hasErrors(project, psiFile.getVirtualFile());
        }
        return true;
    }

    private boolean isPsiFileIncluded(PsiFile psiFile) {
        String canonicalPath = psiFile.getVirtualFile().getCanonicalPath();
        Set<String> inclusions = storage.getInclusions();
        Set<String> exclusions = storage.getExclusions();
        return isIncludedAndNotExcluded(canonicalPath, inclusions, exclusions);
    }

    private boolean isPsiFileFresh(PsiFile psiFile) {
        return psiFile.getModificationStamp() != 0;
    }

    private boolean isPsiFileValid(PsiFile psiFile) {
        return psiFile.isValid();
    }

    private boolean isIncludedAndNotExcluded(String path, Set<String> inclusions, Set<String> exclusions) {
        return isIncluded(inclusions, path) && !isExcluded(exclusions, path);
    }

    private boolean isExcluded(Set<String> exclusions, String path) {
        boolean psiFileExcluded = atLeastOneMatch(path, exclusions);
        if (psiFileExcluded) {
            LOGGER.debug("File " + path + " excluded in " + exclusions);
        }
        return psiFileExcluded;
    }

    private boolean isIncluded(Set<String> inclusions, String path) {
        if (inclusions.isEmpty()) {
            // If no inclusion are defined, all files are allowed
            return true;
        }
        boolean psiFileIncluded = atLeastOneMatch(path, inclusions);
        if (psiFileIncluded) {
            LOGGER.debug("File " + path + " included in " + inclusions);
        }
        return psiFileIncluded;
    }

    private boolean atLeastOneMatch(String psiFileUrl, Set<String> patterns) {
        for (String pattern : patterns) {
            try {
                String REGEX_STARTS_WITH_ANY_STRING = ".*?";
                Matcher matcher = Pattern.compile(REGEX_STARTS_WITH_ANY_STRING + pattern).matcher(psiFileUrl);
                if (matcher.matches()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                // invalid patterns are ignored
                return false;
            }
        }
        return false;
    }

}
