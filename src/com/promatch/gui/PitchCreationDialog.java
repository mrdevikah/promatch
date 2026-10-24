package com.promatch.gui;

import com.promatch.exception.InvalidPitchException;
import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;
import com.promatch.service.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/** "Pitch a Project" form. Check isPublished() after it closes. */
public class PitchCreationDialog extends JDialog {

    private static final int MAX_SUMMARY = 280;
    private static final String[] QUICK_SKILLS = {
            "Java", "Python", "SQL", "React", "Node.js", "Embedded C", "MATLAB", "Docker"
    };

    private final DatabaseManager db;
    private final Student creator;
    private boolean published = false;

    private final JTextField titleField = new JTextField();
    private final JTextArea descriptionArea = new JTextArea(4, 20);
    private final JLabel counterLabel = Theme.label("0/" + MAX_SUMMARY, Font.PLAIN, 12, Theme.TEXT_MUTED);
    private final JTextField skillsField = new JTextField();
    private final JSpinner teamSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 10, 1));
    private final JLabel messageLabel = Theme.label(" ", Font.PLAIN, 13, Theme.ERROR);
    private final Theme.PillButton publishButton =
            new Theme.PillButton("Publish Pitch", Theme.PillButton.Kind.PRIMARY);

    public PitchCreationDialog(Frame owner, DatabaseManager db, Student creator) {
        super(owner, "Pitch a Project", true);
        this.db = db;
        this.creator = creator;

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(28, 32, 24, 32));
        root.add(Theme.label("Pitch a Project", Font.BOLD, 22, Theme.TEXT_PRIMARY), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setSize(Math.max(getWidth(), 560), getHeight());
        setLocationRelativeTo(owner);
    }

    public boolean isPublished() {
        return published;
    }

    // ------------------------------------------------------------ layout

    private JComponent buildForm() {
        Theme.styleField(titleField);
        Theme.styleField(skillsField);
        titleField.setToolTipText("e.g. Smart Campus IoT Monitoring System");
        skillsField.setToolTipText("Separate with commas, e.g. Java, SQL, Git");

        // description: text area inside a rounded scroll pane
        descriptionArea.setFont(Theme.font(Font.PLAIN, 14));
        descriptionArea.setForeground(Theme.TEXT_PRIMARY);
        descriptionArea.setCaretColor(Theme.MAROON);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        descriptionArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateCounter(); }
            @Override public void removeUpdate(DocumentEvent e) { updateCounter(); }
            @Override public void changedUpdate(DocumentEvent e) { updateCounter(); }
        });
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(new Theme.RoundedBorder(Theme.BORDER_STRONG, 10, new Insets(1, 1, 1, 1)));
        descScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel descBox = new JPanel(new BorderLayout(0, 4));
        descBox.setOpaque(false);
        descBox.add(descScroll, BorderLayout.CENTER);
        JPanel counterRow = new JPanel(new BorderLayout());
        counterRow.setOpaque(false);
        counterRow.add(Theme.label("High-level overview only.", Font.PLAIN, 12, Theme.TEXT_MUTED), BorderLayout.WEST);
        counterRow.add(counterLabel, BorderLayout.EAST);
        descBox.add(counterRow, BorderLayout.SOUTH);

        // quick-add skill buttons
        JPanel quick = new JPanel(new GridLayout(2, 4, 6, 6));
        quick.setOpaque(false);
        for (String skill : QUICK_SKILLS) {
            Theme.PillButton b = new Theme.PillButton(skill, Theme.PillButton.Kind.SECONDARY);
            b.setFont(Theme.font(Font.BOLD, 12));
            b.setBorder(new EmptyBorder(5, 8, 5, 8));
            b.addActionListener(e -> addSkill(skill));
            quick.add(b);
        }
        JPanel skillsBox = new JPanel(new BorderLayout(0, 8));
        skillsBox.setOpaque(false);
        skillsBox.add(skillsField, BorderLayout.NORTH);
        skillsBox.add(quick, BorderLayout.CENTER);

        // team size
        teamSizeSpinner.setFont(Theme.font(Font.PLAIN, 14));
        teamSizeSpinner.setPreferredSize(new Dimension(90, 38));
        JPanel teamRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        teamRow.setOpaque(false);
        teamRow.add(teamSizeSpinner);

        JPanel form = Theme.column();
        Theme.addRow(form, Theme.labeled("Project Title", titleField), 14);
        Theme.addRow(form, Theme.labeled("Short Public Summary", descBox), 14);
        Theme.addRow(form, Theme.labeled("Required Skills (comma separated)", skillsBox), 14);
        Theme.addRow(form, Theme.labeled("Team Size (including you)", teamRow), 8);
        Theme.addRow(form, messageLabel, 0);
        return Theme.top(form);
    }

    private JComponent buildFooter() {
        Theme.PillButton cancel = new Theme.PillButton("Cancel", Theme.PillButton.Kind.SECONDARY);
        cancel.addActionListener(e -> dispose());
        publishButton.addActionListener(e -> publish());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.add(cancel);
        footer.add(publishButton);
        return footer;
    }

    // ------------------------------------------------------------ actions

    private void updateCounter() {
        int length = descriptionArea.getText().length();
        counterLabel.setText(length + "/" + MAX_SUMMARY);
        counterLabel.setForeground(length > MAX_SUMMARY ? Theme.ERROR : Theme.TEXT_MUTED);
    }

    private void addSkill(String skill) {
        List<String> skills = Theme.parseSkills(skillsField.getText());
        for (String s : skills) {
            if (s.equalsIgnoreCase(skill)) {
                return;
            }
        }
        skills.add(skill);
        skillsField.setText(String.join(", ", skills));
    }

    private void publish() {
        publishButton.setEnabled(false);
        try {
            ProjectPitch pitch = new ProjectPitch(
                    0,
                    titleField.getText(),
                    descriptionArea.getText(),
                    Theme.parseSkills(skillsField.getText()),
                    (Integer) teamSizeSpinner.getValue(),
                    creator.getUserId());
            db.addPitch(pitch);          // throws InvalidPitchException if a rule is broken
            published = true;
            dispose();
        } catch (InvalidPitchException ex) {
            messageLabel.setText(ex.getMessage());
        } finally {
            publishButton.setEnabled(true);
        }
    }
}

