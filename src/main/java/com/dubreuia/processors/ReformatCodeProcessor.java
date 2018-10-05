package com.dubreuia.processors;

import com.dubreuia.model.Storage;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import javax.swing.*;

import static com.dubreuia.core.component.SaveActionManager.LOGGER;
import static com.dubreuia.model.Action.reformat;
import static com.dubreuia.model.Action.reformatChangedCode;

class ReformatCodeProcessor implements Processor {

    private static final String NAME_CHANGED_TEXT = "ReformatChangedText";
    private static final String NAME_ALL_TEXT = "ReformatAllText";

    private final Project myProject;
    private final Storage storage;
    private final PsiFile myPsiFile;

    ReformatCodeProcessor(Project project, PsiFile psiFile, Storage storage) {
        this.myProject = project;
        this.storage = storage;
        this.myPsiFile = psiFile;
    }

    @Override
    public void run() {
        if (storage.isEnabled(reformat)) {
            try {
                ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
                AnAction action = actionManager.getAction(IdeActions.ACTION_EDITOR_REFORMAT);
                FileEditor editor = FileEditorManager.getInstance(myProject).getSelectedEditor(myPsiFile.getVirtualFile());
                if (editor == null){
                    throw new Exception("Editor for file not found");
                }
                JComponent component = editor.getComponent();
                DataContext dataContext = DataManager.getInstance().getDataContext(component);

                AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext);
                action.beforeActionPerformedUpdate(event);

                actionManager.fireBeforeActionPerformed(action, event.getDataContext(), event);
                action.actionPerformed(event);
                actionManager.fireAfterActionPerformed(action, event.getDataContext(), event);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public String toString() {
        return toString(storage.isEnabled(reformatChangedCode) ? NAME_CHANGED_TEXT : NAME_ALL_TEXT,
                storage.isEnabled(reformat));
    }

}