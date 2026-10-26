package com.promatch.gui;

import com.promatch.model.Student;
import com.promatch.service.DatabaseManager;
import com.promatch.service.MatchEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Sign In / New Student Registration screen (maroon background, white card),
 * like the React "Auth" page.
 */
public class LoginFrame extends JFrame {

    private static final String SIGN_IN = "signin";
    private static final String REGISTER = "register";

    // card switching (CardLayout) - preferred size follows the visible form
    private final CardLayout formCards = new CardLayout() {
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            for (Component c : parent.getComponents()) {
                if (c.isVisible()) {
                    Insets in = parent.getInsets();
                    Dimension d = c.getPreferredSize();
                    return new Dimension(d.width + in.left + in.right, d.height + in.top + in.bottom);
                }
            }
            return super.preferredLayoutSize(parent);
        }
    };
    private final JPanel formHolder = new JPanel(formCards);
    private final Theme.PillButton signInTab = new Theme.PillButton("Sign In", Theme.PillButton.Kind.TAB_ON);
    private final Theme.PillButton registerTab =
            new Theme.PillButton("New Student Registration", Theme.PillButton.Kind.TAB_OFF);

    // sign-in fields
    private final JTextField loginEmailField = new JTextField();
    private final JPasswordField loginPasswordField = new JPasswordField();
    private final JLabel loginMessage = Theme.label(" ", Font.PLAIN, 13, Theme.ERROR);
    private final Theme.PillButton signInButton = new Theme.PillButton("Sign In", Theme.PillButton.Kind.PRIMARY);

    // registration fields
    private final JTextField nameField = new JTextField();
    private final JTextField regEmailField = new JTextField();
    private final JComboBox<String> departmentBox = new JComboBox<>();
    private final JTextField contactField = new JTextField();
    private final JTextField skillsField = new SkillAutoCompleteField();
    private final JTextField cgpaField = new JTextField();
    private final JTextField interestsField = new JTextField();
    private final JPasswordField regPasswordField = new JPasswordField();
    private final JComboBox<String> workModeBox = new JComboBox<>();
    private final JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 40, 1));
    private final JCheckBox availabilityBox = new JCheckBox("I'm available for new projects", true);
    private final JLabel registerMessage = Theme.label(" ", Font.PLAIN, 13, Theme.ERROR);
    private final Theme.PillButton createButton =
            new Theme.PillButton("Create Account", Theme.PillButton.Kind.PRIMARY);

    public LoginFrame() {
        super("ProMatch - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(Theme.MAROON);
        background.setBorder(new EmptyBorder(24, 16, 24, 16));
        background.add(buildCard());
        setContentPane(background);

        showForm(SIGN_IN);
        setMinimumSize(new Dimension(560, 520));
        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------ layout

    private JComponent buildCard() {
        // at least 500px wide, height follows whichever form is showing
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(0, 22), 24, Color.WHITE, null) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(500, d.width), d.height);
            }
        };
        card.setBorder(new EmptyBorder(36, 40, 36, 40));

        // crest + title
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        Theme.Crest crest = new Theme.Crest(56, false);
        crest.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = Theme.label("ProMatch", Font.BOLD, 22, Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = Theme.label(Theme.APP_SUBTITLE, Font.PLAIN, 13, Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(crest);
        header.add(Box.createVerticalStrut(12));
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);

        // pill tabs
        Theme.RoundedPanel tabs = new Theme.RoundedPanel(new GridLayout(1, 2, 4, 0), 12, Theme.NEUTRAL_BG, null);
        tabs.setBorder(new EmptyBorder(4, 4, 4, 4));
        tabs.add(signInTab);
        tabs.add(registerTab);
        signInTab.addActionListener(e -> showForm(SIGN_IN));
        registerTab.addActionListener(e -> showForm(REGISTER));

        JPanel north = new JPanel(new BorderLayout(0, 22));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(tabs, BorderLayout.SOUTH);

        formHolder.setOpaque(false);
        formHolder.add(buildSignInForm(), SIGN_IN);
        formHolder.add(buildRegisterForm(), REGISTER);

        card.add(north, BorderLayout.NORTH);
        card.add(formHolder, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildSignInForm() {
        Theme.styleField(loginEmailField);
        Theme.styleField(loginPasswordField);
        loginEmailField.setToolTipText("yourname@gecthrissur.ac.in");

        JCheckBox showPassword = new JCheckBox("Show password");
        Theme.styleCheck(showPassword);
        final char hiddenChar = loginPasswordField.getEchoChar();
        showPassword.addActionListener(e ->
                loginPasswordField.setEchoChar(showPassword.isSelected() ? (char) 0 : hiddenChar));

        signInButton.addActionListener(e -> handleSignIn());

        Theme.PillButton registerLink = Theme.link("Register here", e -> showForm(REGISTER));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        bottom.setOpaque(false);
        bottom.add(Theme.label("New student?", Font.PLAIN, 13, Theme.TEXT_MUTED));
        bottom.add(registerLink);

        JPanel form = Theme.column();
        Theme.addRow(form, Theme.labeled("University Email", loginEmailField), 14);
        Theme.addRow(form, Theme.labeled("Password", loginPasswordField), 4);
        Theme.addRow(form, showPassword, 6);
        Theme.addRow(form, loginMessage, 6);
        Theme.addRow(form, signInButton, 14);
        Theme.addRow(form, bottom, 0);
        return Theme.top(form);
    }

    private JComponent buildRegisterForm() {
        Theme.styleField(nameField);
        Theme.styleField(regEmailField);
        Theme.styleField(contactField);
        Theme.styleField(skillsField);
        Theme.styleField(cgpaField);
        Theme.styleField(interestsField);
        Theme.styleField(regPasswordField);
        Theme.styleCombo(departmentBox);
        Theme.styleCheck(availabilityBox);

        Theme.styleCombo(workModeBox);
        workModeBox.addItem("Select...");
        for (String mode : MatchEngine.WORK_MODES) {
            workModeBox.addItem(mode);
        }
        hoursSpinner.setFont(Theme.font(Font.PLAIN, 14));
        hoursSpinner.setPreferredSize(new Dimension(90, 38));

        departmentBox.addItem("Select department...");
        for (String d : Theme.DEPARTMENTS) {
            departmentBox.addItem(d);
        }
        nameField.setToolTipText("As on your college ID");
        skillsField.setToolTipText("Separate with commas, e.g. Java, SQL, Python");
        regPasswordField.setToolTipText("Minimum 8 characters");

        JPanel cgpaAndInterest = new JPanel(new GridLayout(1, 2, 12, 0));
        cgpaAndInterest.setOpaque(false);
        cgpaAndInterest.add(Theme.labeled("CGPA (0-10)", cgpaField));
        cgpaAndInterest.add(Theme.labeled("Areas of Interest", interestsField));

        JPanel workModeAndHours = new JPanel(new GridLayout(1, 2, 12, 0));
        workModeAndHours.setOpaque(false);
        workModeAndHours.add(Theme.labeled("Preferred Work Mode", workModeBox));
        workModeAndHours.add(Theme.labeled("Hours per week you can give", hoursSpinner));

        createButton.addActionListener(e -> handleRegister());

        JPanel form = Theme.column();
        Theme.addRow(form, Theme.labeled("Full Name", nameField), 14);
        Theme.addRow(form, Theme.labeled("University Email", regEmailField), 14);
        Theme.addRow(form, Theme.labeled("Department", departmentBox), 14);
        Theme.addRow(form, Theme.labeled("Contact Info (phone / GitHub)", contactField), 14);
        Theme.addRow(form, Theme.labeled("Skills (comma separated)", skillsField), 14);
        Theme.addRow(form, cgpaAndInterest, 14);
        Theme.addRow(form, workModeAndHours, 14);
        Theme.addRow(form, Theme.labeled("Password (min. 8 characters)", regPasswordField), 8);
        Theme.addRow(form, availabilityBox, 6);
        Theme.addRow(form, registerMessage, 6);
        Theme.addRow(form, createButton, 0);

        JScrollPane scroll = new JScrollPane(Theme.top(form));
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(400, 330));
        return scroll;
    }

    private void showForm(String which) {
        boolean signIn = SIGN_IN.equals(which);
        formCards.show(formHolder, which);
        signInTab.setKind(signIn ? Theme.PillButton.Kind.TAB_ON : Theme.PillButton.Kind.TAB_OFF);
        registerTab.setKind(signIn ? Theme.PillButton.Kind.TAB_OFF : Theme.PillButton.Kind.TAB_ON);
        getRootPane().setDefaultButton(signIn ? signInButton : null);
        setTitle(signIn ? "ProMatch - Sign in" : "ProMatch - Register");
        loginMessage.setText(" ");
        registerMessage.setText(" ");
        if (isDisplayable()) {
            pack();
            setLocationRelativeTo(null);
        }
    }

    // ------------------------------------------------------------ actions

    private void handleSignIn() {
        String email = loginEmailField.getText().trim();
        String password = new String(loginPasswordField.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            loginMessage.setText("Please enter your email and password.");
            return;
        }
        Student student = DatabaseManager.login(email, password);
        if (student == null) {
            loginMessage.setText("Incorrect email or password.");
            return;
        }
        openDashboard(student);
    }

    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = regEmailField.getText().trim();
        String contact = contactField.getText().trim();
        String interests = interestsField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        List<String> skills = Theme.parseSkills(skillsField.getText());

        if (name.isEmpty() || email.isEmpty() || contact.isEmpty() || interests.isEmpty()) {
            registerMessage.setText("Please fill in all the fields.");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            registerMessage.setText("Enter a valid university email address.");
            return;
        }
        if (departmentBox.getSelectedIndex() <= 0) {
            registerMessage.setText("Please select your department.");
            return;
        }
        if (skills.isEmpty()) {
            registerMessage.setText("Add at least one skill.");
            return;
        }
        if (workModeBox.getSelectedIndex() <= 0) {
            registerMessage.setText("Please choose your preferred work mode.");
            return;
        }
        double cgpa;
        try {
            cgpa = Double.parseDouble(cgpaField.getText().trim());
        } catch (NumberFormatException ex) {
            registerMessage.setText("CGPA must be a number, for example 8.5.");
            return;
        }
        if (cgpa < 0 || cgpa > 10) {
            registerMessage.setText("CGPA must be between 0 and 10.");
            return;
        }
        if (password.length() < 8) {
            registerMessage.setText("Password must be at least 8 characters.");
            return;
        }
        if (DatabaseManager.emailExists(email)) {
            registerMessage.setText("An account with this email already exists.");
            return;
        }

        Student student = new Student(0, name, email, (String) departmentBox.getSelectedItem(),
                contact, skills, cgpa, interests, availabilityBox.isSelected(),
                (String) workModeBox.getSelectedItem(), (Integer) hoursSpinner.getValue());
        DatabaseManager.registerStudent(student, password);   // also sets the new user id
        openDashboard(student);
    }

    private void openDashboard(Student student) {
        new DashboardFrame(student).setVisible(true);
        dispose();
    }
}