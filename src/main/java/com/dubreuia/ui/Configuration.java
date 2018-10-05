package com.dubreuia.ui;

import com.dubreuia.model.Action;
import com.dubreuia.model.Storage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dubreuia.model.Action.activate;
import static com.dubreuia.model.Action.activateOnShortcut;
import static com.dubreuia.model.Action.customUnqualifiedStaticMemberAccess;
import static com.dubreuia.model.Action.reformat;
import static com.dubreuia.model.Action.reformatChangedCode;
import static com.dubreuia.model.Action.unqualifiedStaticMemberAccess;


public class Configuration implements Configurable {

    private static final String TEXT_DISPLAY_NAME = "Save Actions";

    private final Storage storage;

    private final Set<String> exclusions = new HashSet<>();
    private final Set<String> inclusions = new HashSet<>();
    private final Map<Action, JCheckBox> checkboxes = new HashMap<>();
    private final ActionListener checkboxActionListener = this::updateCheckboxEnabled;

    private GeneralPanel generalPanel;
    private FormattingPanel formattingPanel;

    public Configuration(Project project) {
        storage = ServiceManager.getService(project, Storage.class);
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = initComponent();
        initFirstLaunch();
        initActionListeners();
        return panel;
    }

    private void initFirstLaunch() {
        if (storage.isFirstLaunch()) {
            updateSelectedStateOfCheckboxes(Action.getDefaults());
            storage.stopFirstLaunch();
        }
    }

    private void initActionListeners() {
        for (Map.Entry<Action, JCheckBox> checkbox : checkboxes.entrySet()) {
            checkbox.getValue().addActionListener(checkboxActionListener);
        }
    }

    @Override
    public boolean isModified() {
        for (Map.Entry<Action, JCheckBox> checkbox : checkboxes.entrySet()) {
            if (storage.isEnabled(checkbox.getKey()) != checkbox.getValue().isSelected()) {
                return true;
            }
        }
        return !storage.getExclusions().equals(exclusions)
                || !storage.getInclusions().equals(inclusions);
    }

    @Override
    public void apply() {
        for (Map.Entry<Action, JCheckBox> checkbox : checkboxes.entrySet()) {
            storage.setEnabled(checkbox.getKey(), checkbox.getValue().isSelected());
        }
        storage.setExclusions(new HashSet<>(exclusions));
        storage.setInclusions(new HashSet<>(inclusions));
        updateSelectedStateOfCheckboxes(storage.getActions());
        updateCheckboxEnabled(null);
    }

    @Override
    public void reset() {
        updateSelectedStateOfCheckboxes(storage.getActions());
        updateCheckboxEnabled(null);
        updateExclusions();
        updateInclusions();
    }

    private void updateSelectedStateOfCheckboxes(Set<Action> selectedActions) {
        for (Map.Entry<Action, JCheckBox> checkbox : checkboxes.entrySet()) {
            boolean isSelected = selectedActions.contains(checkbox.getKey());
            checkbox.getValue().setSelected(isSelected);
        }
    }

    @Override
    public void disposeUIResources() {
        checkboxes.clear();
        exclusions.clear();
        inclusions.clear();
        generalPanel = null;
        formattingPanel = null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return TEXT_DISPLAY_NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    private JPanel initComponent() {
        for (Action action : Action.values()) {
            checkboxes.put(action, new JCheckBox(action.getText()));
        }
        generalPanel = new GeneralPanel(checkboxes);
        formattingPanel = new FormattingPanel(checkboxes);
        return initRootPanel(
                generalPanel.getPanel(),
                formattingPanel.getPanel()
        );
    }

    private JPanel initRootPanel(JPanel general, JPanel actions) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0;
        panel.add(general, c);
        c.gridy = 1;
        panel.add(actions, c);

        c.gridy = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, c);

        return panel;
    }

    private void updateInclusions() {
        inclusions.clear();
        inclusions.addAll(storage.getInclusions());
    }

    private void updateExclusions() {
        exclusions.clear();
        exclusions.addAll(storage.getExclusions());
    }

    private void updateCheckboxEnabled(ActionEvent event) {
        updateCheckboxEnabledIfActiveSelected();
        updateCheckboxEnabledGroupReformat();
        updateCheckboxEnabledGroupStaticExclusive(event);
    }

    private void updateCheckboxEnabledIfActiveSelected() {
        for (Map.Entry<Action, JCheckBox> checkbox : checkboxes.entrySet()) {
            Action currentCheckBoxKey = checkbox.getKey();
            if (!activate.equals(currentCheckBoxKey) && !activateOnShortcut.equals(currentCheckBoxKey)) {
                checkbox.getValue().setEnabled(isActiveSelected());
            }
        }
    }

    private void updateCheckboxEnabledGroupReformat() {
        boolean reformatSelected = checkboxes.get(reformat).isSelected();
        checkboxes.get(reformatChangedCode).setEnabled(isActiveSelected() && reformatSelected);
    }

    private void updateCheckboxEnabledGroupStaticExclusive(ActionEvent event) {
        if (event == null || !(event.getSource() instanceof JCheckBox)) {
            return;
        }
        JCheckBox thisCheckbox = (JCheckBox) event.getSource();
        if (thisCheckbox.isSelected()) {
            if (thisCheckbox == checkboxes.get(unqualifiedStaticMemberAccess)) {
                checkboxes.get(customUnqualifiedStaticMemberAccess).setSelected(false);
            } else if (thisCheckbox == checkboxes.get(customUnqualifiedStaticMemberAccess)) {
                checkboxes.get(unqualifiedStaticMemberAccess).setSelected(false);
            }
        }
    }

    private boolean isActiveSelected() {
        boolean activateIsSelected = checkboxes.get(activate).isSelected();
        boolean activateShortcutIsSelected = checkboxes.get(activateOnShortcut).isSelected();
        return activateIsSelected || activateShortcutIsSelected;
    }

}
