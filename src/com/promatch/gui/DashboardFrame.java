package com.promatch.gui;

import com.promatch.model.Invitation;
import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;
import com.promatch.service.DatabaseManager;
import com.promatch.service.MatchEngine;
import com.promatch.service.MatchEngine.Match;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Main window after login: a maroon sidebar on the left and a CardLayout on the right
 * that switches between Dashboard, Explore Projects, Find Teammates, Invitations and Profile.
 * Each view is rebuilt from the database every time it is opened, so it is always up to date.
 */
public class DashboardFrame extends JFrame {

    private static final String DASHBOARD = "Dashboard";
    private static final String EXPLORE = "Explore Projects";
    private static final String TEAMMATES = "Find Teammates";
    private static final String INVITATIONS = "Invitations";
    private static final String PROFILE = "Profile";
    private static final String[] VIEW_NAMES = {DASHBOARD, EXPLORE, TEAMMATES, INVITATIONS, PROFILE};

    private final Student currentUser;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JComponent> views = new HashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    public DashboardFrame(Student currentUser) {
        super("ProMatch - " + Theme.APP_SUBTITLE);
        this.currentUser = currentUser;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        content.setBackground(Theme.BG_PAGE);
        add(content, BorderLayout.CENTER);

        showView(DASHBOARD);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);
    }

    // =====================================================================
    //  Sidebar + navigation
    // =====================================================================

    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.MAROON);
        side.setPreferredSize(new Dimension(250, 0));
        side.setBorder(new EmptyBorder(24, 12, 24, 12));

        // logo block
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Theme.label("ProMatch", Font.BOLD, 16, Color.WHITE));
        titles.add(Theme.label(Theme.ORG_NAME, Font.PLAIN, 12, new Color(0xE6C3CB)));
        JPanel logo = new JPanel(new BorderLayout(12, 0));
        logo.setOpaque(false);
        logo.setBorder(new EmptyBorder(4, 12, 18, 12));
        logo.add(new Theme.Crest(40, true), BorderLayout.WEST);
        logo.add(titles, BorderLayout.CENTER);

        JPanel line = new JPanel();
        line.setBackground(Theme.SIDEBAR_LINE);
        line.setPreferredSize(new Dimension(1, 1));
        JPanel logoWithLine = new JPanel(new BorderLayout(0, 0));
        logoWithLine.setOpaque(false);
        logoWithLine.add(logo, BorderLayout.CENTER);
        logoWithLine.add(line, BorderLayout.SOUTH);

        // nav items
        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 2));
        nav.setOpaque(false);
        for (String name : VIEW_NAMES) {
            NavButton b = new NavButton(name, Color.WHITE);
            b.addActionListener(e -> showView(name));
            navButtons.put(name, b);
            nav.add(b);
        }
        JPanel navWrap = new JPanel(new BorderLayout());
        navWrap.setOpaque(false);
        navWrap.setBorder(new EmptyBorder(12, 0, 0, 0));
        navWrap.add(nav, BorderLayout.NORTH);

        // sign out
        NavButton signOut = new NavButton("Sign Out", new Color(0xF0D9DE));
        signOut.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel line2 = new JPanel();
        line2.setBackground(Theme.SIDEBAR_LINE);
        line2.setPreferredSize(new Dimension(1, 1));
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(line2, BorderLayout.NORTH);
        bottom.add(signOut, BorderLayout.CENTER);

        side.add(logoWithLine, BorderLayout.NORTH);
        side.add(navWrap, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    /** Builds the view fresh from the database and flips the CardLayout to it. */
    private void showView(String name) {
        JComponent old = views.remove(name);
        if (old != null) {
            content.remove(old);
        }
        JComponent fresh;
        switch (name) {
            case DASHBOARD: fresh = buildDashboardView(); break;
            case EXPLORE: fresh = buildExploreView(); break;
            case TEAMMATES: fresh = buildTeammatesView(); break;
            case INVITATIONS: fresh = buildInvitationsView(); break;
            default: fresh = buildProfileView(); break;
        }
        views.put(name, fresh);
        content.add(fresh, name);
        cards.show(content, name);
        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setActive(e.getKey().equals(name));
        }
        content.revalidate();
        content.repaint();
    }

    /** Sidebar button: highlighted pill + white bar on the left when active. */
    private static class NavButton extends JButton {
        private boolean active;
        private boolean hover;

        NavButton(String text, Color textColor) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(Theme.font(Font.BOLD, 15));
            setForeground(textColor);
            setBorder(new EmptyBorder(0, 20, 0, 14));
            setPreferredSize(new Dimension(100, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active || hover) {
                g2.setColor(new Color(255, 255, 255, active ? 46 : 26));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            if (active) {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, (int) (getHeight() * 0.15), 4, (int) (getHeight() * 0.7), 4, 4);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =====================================================================
    //  Small shared helpers
    // =====================================================================

    private String firstName() {
        return currentUser.getName().trim().split("\\s+")[0];
    }

    private String greeting() {
        int hour = java.time.LocalTime.now().getHour();
        return hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
    }

    private List<ProjectPitch> otherStudentsPitches() {
        List<ProjectPitch> list = new ArrayList<>();
        for (ProjectPitch p : DatabaseManager.getAllPitches()) {
            if (p.getCreatorId() != currentUser.getUserId()) {
                list.add(p);
            }
        }
        return list;
    }

    private List<Student> otherStudents(boolean availableOnly) {
        List<Student> list = new ArrayList<>();
        for (Student s : DatabaseManager.getAllStudents()) {
            if (s.getUserId() != currentUser.getUserId() && (!availableOnly || s.isAvailabilityStatus())) {
                list.add(s);
            }
        }
        return list;
    }

    private Map<Integer, Student> studentsById() {
        Map<Integer, Student> map = new HashMap<>();
        for (Student s : DatabaseManager.getAllStudents()) {
            map.put(s.getUserId(), s);
        }
        return map;
    }

    private static String nameOf(Map<Integer, Student> students, int id) {
        Student s = students.get(id);
        return s == null ? "Unknown" : s.getName();
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** White rounded card that holds a table. */
    private JComponent tableCard(JTable table) {
        Theme.styleTable(table);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());   // (null would let JTable add its own dark border)
        sp.getViewport().setBackground(Color.WHITE);
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(), 16, Color.WHITE, Theme.BORDER);
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel tablePage(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setBackground(Theme.BG_PAGE);
        page.setBorder(new EmptyBorder(32, 32, 32, 32));
        page.add(Theme.pageHeader(title, subtitle), BorderLayout.NORTH);
        return page;
    }

    private void info(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void openPitchDialog() {
        PitchCreationDialog dialog = new PitchCreationDialog(this, currentUser);
        dialog.setVisible(true);                 // modal: waits here until it is closed
        if (dialog.isPublished()) {
            showView(DASHBOARD);
        }
    }

    /** Send an invitation to a student for one of my projects. */
    private void inviteStudent(Student target, ProjectPitch pitch) {
        boolean sent = DatabaseManager.sendInvitation(currentUser.getUserId(), target.getUserId(), pitch.getProjectId());
        if (sent) {
            info("Invitation sent to " + target.getName() + " for \"" + pitch.getTitle() + "\".", "Invitation sent");
        } else {
            warn("You already have an invitation or request with " + target.getName()
                    + " for this project.", "Already invited");
        }
    }

    /** Ask to join somebody else's project. */
    private void requestToJoin(ProjectPitch pitch, Map<Integer, Student> students) {
        boolean sent = DatabaseManager.sendInvitation(currentUser.getUserId(), pitch.getCreatorId(), pitch.getProjectId());
        String creator = nameOf(students, pitch.getCreatorId());
        if (sent) {
            info("Your request was sent to " + creator + ".", "Request sent");
        } else {
            warn("You already have a request or invitation with " + creator + " for this project.",
                    "Already requested");
        }
    }

    // =====================================================================
    //  1. Dashboard
    // =====================================================================

    private JComponent buildDashboardView() {
        List<ProjectPitch> mine = DatabaseManager.getPitchesByCreator(currentUser.getUserId());
        List<ProjectPitch> joined = DatabaseManager.getJoinedPitches(currentUser.getUserId());

        // greeting row
        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 4));
        texts.setOpaque(false);
        texts.add(Theme.label(greeting() + ", " + firstName(), Font.BOLD, 30, Theme.TEXT_PRIMARY));
        texts.add(Theme.label(currentUser.getDepartment() + "   \u00B7   CGPA " + currentUser.getCgpa(),
                Font.PLAIN, 15, Theme.TEXT_MUTED));
        Theme.PillButton pitchButton = new Theme.PillButton("+  Pitch a Project", Theme.PillButton.Kind.PRIMARY);
        pitchButton.addActionListener(e -> openPitchDialog());
        JPanel greetingRow = new JPanel(new BorderLayout());
        greetingRow.setOpaque(false);
        greetingRow.add(texts, BorderLayout.WEST);
        JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonWrap.setOpaque(false);
        buttonWrap.add(pitchButton);
        greetingRow.add(buttonWrap, BorderLayout.EAST);

        // top row: my pitches (58%) + recommended (42%)
        JPanel topRow = new JPanel(new GridBagLayout());
        topRow.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = 0.58;
        gc.insets = new Insets(0, 0, 0, 12);
        topRow.add(buildMyPitchesCard(mine, joined), gc);
        gc.gridx = 1;
        gc.weightx = 0.42;
        gc.insets = new Insets(0, 12, 0, 0);
        topRow.add(buildRecommendedCard(), gc);

        JPanel stack = Theme.column();
        Theme.addRow(stack, greetingRow, 28);
        Theme.addRow(stack, topRow, 24);
        Theme.addRow(stack, buildTopTeammatesCard(mine), 0);

        Theme.ScrollPage page = new Theme.ScrollPage(new BorderLayout());
        page.setBackground(Theme.BG_PAGE);
        page.setBorder(new EmptyBorder(32, 32, 32, 32));
        page.add(stack, BorderLayout.NORTH);
        return Theme.scroll(page);
    }

    private JComponent buildMyPitchesCard(List<ProjectPitch> mine, List<ProjectPitch> joined) {
        JPanel list = Theme.column();
        if (mine.isEmpty() && joined.isEmpty()) {
            Theme.addRow(list, Theme.label("You haven't pitched or joined a project yet.",
                    Font.PLAIN, 14, Theme.TEXT_MUTED), 0);
        }
        for (ProjectPitch p : mine) {
            Theme.addRow(list, projectRow(p, "Owner"), 18);
        }
        for (ProjectPitch p : joined) {
            Theme.addRow(list, projectRow(p, "Member"), 18);
        }
        return Theme.card("My Active Pitches & Joined Projects",
                Theme.link("View all", e -> showView(EXPLORE)), Theme.top(list));
    }

    private JComponent projectRow(ProjectPitch p, String role) {
        boolean owner = "Owner".equals(role);
        Theme.Chip roleChip = owner
                ? new Theme.Chip(role, Theme.MAROON, Theme.BRAND_50, Theme.BRAND_100)
                : new Theme.Chip(role, Theme.TEXT_SECONDARY, Theme.NEUTRAL_BG, Theme.BORDER);
        JPanel chipWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chipWrap.setOpaque(false);
        chipWrap.add(roleChip);

        int filled = Math.min(p.getMaxTeamSize(), 1 + DatabaseManager.countAcceptedMembers(p.getProjectId()));
        JPanel row = Theme.column();
        Theme.addRow(row, chipWrap, 6);
        Theme.addRow(row, Theme.label(p.getTitle(), Font.BOLD, 15, Theme.TEXT_PRIMARY), 10);
        Theme.addRow(row, Theme.teamProgress(filled, p.getMaxTeamSize()), 0);
        return row;
    }

    private JComponent buildRecommendedCard() {
        List<Match<ProjectPitch>> ranked = MatchEngine.rankProjectsForStudent(currentUser, otherStudentsPitches());
        JPanel list = Theme.column();
        if (ranked.isEmpty()) {
            Theme.addRow(list, Theme.label("No pitches from other students yet.", Font.PLAIN, 14, Theme.TEXT_MUTED), 0);
        }
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            Theme.addRow(list, recommendedItem(ranked.get(i)), 12);
        }
        return Theme.card("Recommended Pitches",
                Theme.link("View all", e -> showView(EXPLORE)), Theme.top(list));
    }

    private JComponent recommendedItem(Match<ProjectPitch> match) {
        ProjectPitch p = match.getItem();
        List<String> matched = MatchEngine.getMatchedSkills(p.getRequiredSkills(), currentUser.getSkillsList());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(Theme.chipRow(matched, matched, 2, FlowLayout.LEFT), BorderLayout.WEST);
        bottom.add(Theme.label(match.getScore() + "% match", Font.BOLD, 12, Theme.MAROON), BorderLayout.EAST);

        final Theme.RoundedPanel item = new Theme.RoundedPanel(new BorderLayout(0, 10), 10, Color.WHITE, Theme.BORDER);
        item.setBorder(new EmptyBorder(12, 12, 12, 12));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.add(Theme.wrapLabel(p.getTitle(), 270, Font.BOLD, 14, Theme.TEXT_PRIMARY), BorderLayout.NORTH);
        item.add(bottom, BorderLayout.CENTER);
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setFill(Theme.BRAND_50);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setFill(Color.WHITE);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                showView(EXPLORE);
            }
        });
        return item;
    }

    private JComponent buildTopTeammatesCard(List<ProjectPitch> mine) {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        if (mine.isEmpty()) {
            body.add(Theme.label("Pitch a project and the best-matching students will show up here.",
                    Font.PLAIN, 14, Theme.TEXT_MUTED), BorderLayout.NORTH);
        } else {
            ProjectPitch focus = mine.get(0);            // newest pitch
            List<Match<Student>> ranked = MatchEngine.rankStudentMatches(focus, otherStudents(true));
            body.add(Theme.label("Best fits for \"" + focus.getTitle() + "\"", Font.PLAIN, 14, Theme.TEXT_MUTED),
                    BorderLayout.NORTH);
            JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
            grid.setOpaque(false);
            for (int i = 0; i < 4; i++) {
                if (i < ranked.size()) {
                    grid.add(teammateMiniCard(ranked.get(i), focus));
                } else {
                    JPanel spacer = new JPanel();
                    spacer.setOpaque(false);
                    grid.add(spacer);
                }
            }
            body.add(grid, BorderLayout.CENTER);
        }
        return Theme.card("Top Teammate Matches",
                Theme.link("Browse directory \u2192", e -> showView(TEAMMATES)), body);
    }

    private JComponent teammateMiniCard(Match<Student> match, ProjectPitch pitch) {
        Student s = match.getItem();
        List<String> matched = MatchEngine.getMatchedSkills(pitch.getRequiredSkills(), s.getSkillsList());

        Theme.Avatar avatar = new Theme.Avatar(s.getName(), 48);
        JLabel name = Theme.label(s.getName(), Font.BOLD, 14, Theme.TEXT_PRIMARY);
        JLabel dept = Theme.label(shortDept(s.getDepartment()) + "  \u00B7  CGPA " + s.getCgpa(),
                Font.PLAIN, 12, Theme.TEXT_MUTED);
        JLabel score = Theme.label(match.getScore() + "% match", Font.BOLD, 12, Theme.MAROON);
        JPanel chips = Theme.chipRow(s.getSkillsList(), matched, 2, FlowLayout.CENTER);
        Theme.PillButton invite = new Theme.PillButton("Send Invite", Theme.PillButton.Kind.SECONDARY);
        invite.setBorder(new EmptyBorder(8, 14, 8, 14));
        invite.addActionListener(e -> inviteStudent(s, pitch));

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        for (JComponent c : new JComponent[]{avatar, name, dept, score, chips, invite}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        name.setHorizontalAlignment(SwingConstants.CENTER);
        dept.setHorizontalAlignment(SwingConstants.CENTER);
        score.setHorizontalAlignment(SwingConstants.CENTER);
        invite.setMaximumSize(new Dimension(Integer.MAX_VALUE, invite.getPreferredSize().height));
        box.add(avatar);
        box.add(Box.createVerticalStrut(10));
        box.add(name);
        box.add(dept);
        box.add(Box.createVerticalStrut(6));
        box.add(score);
        box.add(Box.createVerticalStrut(8));
        box.add(chips);
        box.add(Box.createVerticalStrut(12));
        box.add(invite);

        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(), 12, Color.WHITE, Theme.BORDER);
        card.setBorder(new EmptyBorder(16, 14, 16, 14));
        card.add(box, BorderLayout.CENTER);
        return card;
    }

    private static String shortDept(String department) {
        if (department == null) {
            return "";
        }
        if (department.startsWith("Computer")) return "CSE";
        if (department.startsWith("Electronics")) return "ECE";
        if (department.startsWith("Electrical")) return "EEE";
        if (department.startsWith("Mechanical")) return "ME";
        if (department.startsWith("Civil")) return "CE";
        return department;
    }

    // =====================================================================
    //  2. Explore Projects  (match result view: JTable ranked by score)
    // =====================================================================

    private JComponent buildExploreView() {
        final Map<Integer, Student> students = studentsById();
        List<ProjectPitch> others = otherStudentsPitches();
        final List<Match<ProjectPitch>> ranked = MatchEngine.rankProjectsForStudent(currentUser, others);
        final List<Match<ProjectPitch>> shown = new ArrayList<>();

        // filter controls
        TreeSet<String> skillSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ProjectPitch p : others) {
            skillSet.addAll(p.getRequiredSkills());
        }
        final String allSkills = "All skills";
        final JComboBox<String> skillBox = new JComboBox<>();
        skillBox.addItem(allSkills);
        for (String s : skillSet) {
            skillBox.addItem(s);
        }
        Theme.styleCombo(skillBox);
        skillBox.setPreferredSize(new Dimension(190, 38));
        final JTextField searchField = new JTextField(16);
        Theme.styleField(searchField);

        final DefaultTableModel model = readOnlyModel("Project", "Created by", "Required skills", "Team", "Match");
        final JTable table = new JTable(model);

        final Runnable applyFilter = () -> {
            model.setRowCount(0);
            shown.clear();
            String skill = (String) skillBox.getSelectedItem();
            String query = searchField.getText().trim().toLowerCase();
            for (Match<ProjectPitch> m : ranked) {
                ProjectPitch p = m.getItem();
                boolean skillOk = skill == null || allSkills.equals(skill) || containsIgnoreCase(p.getRequiredSkills(), skill);
                boolean textOk = query.isEmpty()
                        || p.getTitle().toLowerCase().contains(query)
                        || p.getDescription().toLowerCase().contains(query);
                if (skillOk && textOk) {
                    shown.add(m);
                    int filled = Math.min(p.getMaxTeamSize(), 1 + DatabaseManager.countAcceptedMembers(p.getProjectId()));
                    model.addRow(new Object[]{
                            p.getTitle(),
                            nameOf(students, p.getCreatorId()),
                            String.join(", ", p.getRequiredSkills()),
                            filled + "/" + p.getMaxTeamSize(),
                            m.getScore() + "%"});
                }
            }
        };

        // events: choosing a skill (ItemListener), pressing Apply / Enter / Clear (ActionListener)
        skillBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilter.run();
            }
        });
        ActionListener apply = e -> applyFilter.run();
        searchField.addActionListener(apply);
        Theme.PillButton applyButton = new Theme.PillButton("Apply Filter", Theme.PillButton.Kind.PRIMARY);
        applyButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        applyButton.addActionListener(apply);
        Theme.PillButton clearButton = new Theme.PillButton("Clear", Theme.PillButton.Kind.SECONDARY);
        clearButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        clearButton.addActionListener(e -> {
            searchField.setText("");
            skillBox.setSelectedIndex(0);
            applyFilter.run();
        });

        Theme.RoundedPanel filterBar =
                new Theme.RoundedPanel(new FlowLayout(FlowLayout.LEFT, 12, 12), 12, Color.WHITE, Theme.BORDER);
        filterBar.add(Theme.label("Skill", Font.BOLD, 13, Theme.TEXT_SECONDARY));
        filterBar.add(skillBox);
        filterBar.add(Theme.label("Search", Font.BOLD, 13, Theme.TEXT_SECONDARY));
        filterBar.add(searchField);
        filterBar.add(applyButton);
        filterBar.add(clearButton);

        // table
        applyFilter.run();
        table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        JComponent tableCard = tableCard(table);
        table.getColumnModel().getColumn(3)
                .setCellRenderer(new Theme.CellRenderer(null, false, SwingConstants.CENTER));
        table.getColumnModel().getColumn(4)
                .setCellRenderer(new Theme.CellRenderer(Theme::matchColor, true, SwingConstants.CENTER));

        // send request
        Theme.PillButton sendButton = new Theme.PillButton("Send Request", Theme.PillButton.Kind.PRIMARY);
        sendButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                info("Select a project in the table first.", "No project selected");
                return;
            }
            requestToJoin(shown.get(row).getItem(), students);
        });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(Theme.label("Select a project, then send a request to its creator.",
                Font.PLAIN, 13, Theme.TEXT_MUTED), BorderLayout.WEST);
        bottom.add(sendButton, BorderLayout.EAST);

        JPanel middle = new JPanel(new BorderLayout(0, 16));
        middle.setOpaque(false);
        middle.add(filterBar, BorderLayout.NORTH);
        middle.add(tableCard, BorderLayout.CENTER);

        JPanel page = tablePage("Explore Projects", "Ranked by how well your skills and CGPA match what each project needs.");
        page.add(middle, BorderLayout.CENTER);
        page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        for (String s : list) {
            if (s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    //  3. Find Teammates  (students ranked for one of my projects)
    // =====================================================================

    private JComponent buildTeammatesView() {
        List<ProjectPitch> mine = DatabaseManager.getPitchesByCreator(currentUser.getUserId());
        JPanel page = tablePage("Find Teammates", "Students ranked by how well they fit one of your projects.");

        if (mine.isEmpty()) {
            JPanel empty = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 60));
            empty.setOpaque(false);
            JPanel inner = Theme.column();
            Theme.addRow(inner, Theme.label("You need a project pitch before you can look for teammates.",
                    Font.PLAIN, 15, Theme.TEXT_MUTED), 16);
            Theme.PillButton pitch = new Theme.PillButton("+  Pitch a Project", Theme.PillButton.Kind.PRIMARY);
            pitch.addActionListener(e -> openPitchDialog());
            JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            btnWrap.setOpaque(false);
            btnWrap.add(pitch);
            Theme.addRow(inner, btnWrap, 0);
            empty.add(inner);
            page.add(empty, BorderLayout.CENTER);
            return page;
        }

        final List<Student> candidates = otherStudents(false);
        final List<Match<Student>> shown = new ArrayList<>();

        final JComboBox<ProjectPitch> projectBox = new JComboBox<>(mine.toArray(new ProjectPitch[0]));
        projectBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String text = value instanceof ProjectPitch ? ((ProjectPitch) value).getTitle() : "";
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        Theme.styleCombo(projectBox);
        projectBox.setPreferredSize(new Dimension(340, 38));
        final JCheckBox availableOnly = new JCheckBox("Only show available students", true);
        Theme.styleCheck(availableOnly);

        final DefaultTableModel model = readOnlyModel("Name", "Dept", "Skills", "CGPA", "Available", "Match");
        final JTable table = new JTable(model);

        final Runnable refresh = () -> {
            model.setRowCount(0);
            shown.clear();
            ProjectPitch pitch = (ProjectPitch) projectBox.getSelectedItem();
            if (pitch == null) {
                return;
            }
            for (Match<Student> m : MatchEngine.rankStudentMatches(pitch, candidates)) {
                Student s = m.getItem();
                if (availableOnly.isSelected() && !s.isAvailabilityStatus()) {
                    continue;
                }
                shown.add(m);
                model.addRow(new Object[]{
                        s.getName(),
                        shortDept(s.getDepartment()),
                        String.join(", ", s.getSkillsList()),
                        String.valueOf(s.getCgpa()),
                        s.isAvailabilityStatus() ? "Yes" : "No",
                        m.getScore() + "%"});
            }
        };
        projectBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                refresh.run();
            }
        });
        availableOnly.addItemListener(e -> refresh.run());
        refresh.run();

        Theme.RoundedPanel filterBar =
                new Theme.RoundedPanel(new FlowLayout(FlowLayout.LEFT, 12, 12), 12, Color.WHITE, Theme.BORDER);
        filterBar.add(Theme.label("My project", Font.BOLD, 13, Theme.TEXT_SECONDARY));
        filterBar.add(projectBox);
        filterBar.add(availableOnly);

        JComponent tableCard = tableCard(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(320);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        for (int col : new int[]{1, 3, 4}) {
            table.getColumnModel().getColumn(col)
                    .setCellRenderer(new Theme.CellRenderer(null, false, SwingConstants.CENTER));
        }
        table.getColumnModel().getColumn(5)
                .setCellRenderer(new Theme.CellRenderer(Theme::matchColor, true, SwingConstants.CENTER));

        Theme.PillButton sendButton = new Theme.PillButton("Send Invitation", Theme.PillButton.Kind.PRIMARY);
        sendButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                info("Select a student in the table first.", "No student selected");
                return;
            }
            inviteStudent(shown.get(row).getItem(), (ProjectPitch) projectBox.getSelectedItem());
        });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(Theme.label("Select a student, then invite them to the project chosen above.",
                Font.PLAIN, 13, Theme.TEXT_MUTED), BorderLayout.WEST);
        bottom.add(sendButton, BorderLayout.EAST);

        JPanel middle = new JPanel(new BorderLayout(0, 16));
        middle.setOpaque(false);
        middle.add(filterBar, BorderLayout.NORTH);
        middle.add(tableCard, BorderLayout.CENTER);
        page.add(middle, BorderLayout.CENTER);
        page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    // =====================================================================
    //  4. Invitations  (received + sent)
    // =====================================================================

    private JComponent buildInvitationsView() {
        final Map<Integer, Student> students = studentsById();
        final Map<Integer, ProjectPitch> pitches = new HashMap<>();
        for (ProjectPitch p : DatabaseManager.getAllPitches()) {
            pitches.put(p.getProjectId(), p);
        }
        final List<Invitation> received = DatabaseManager.getInvitationsReceivedBy(currentUser.getUserId());
        List<Invitation> sent = DatabaseManager.getInvitationsSentBy(currentUser.getUserId());

        // received table
        DefaultTableModel receivedModel = readOnlyModel("From", "Project", "Type", "Status");
        for (Invitation inv : received) {
            receivedModel.addRow(new Object[]{
                    nameOf(students, inv.getSenderId()), projectTitle(pitches, inv), typeOf(pitches, inv), inv.getStatus()});
        }
        final JTable receivedTable = new JTable(receivedModel);
        JComponent receivedTableCard = tableCard(receivedTable);
        styleInvitationColumns(receivedTable);

        Theme.PillButton accept = new Theme.PillButton("Accept", Theme.PillButton.Kind.PRIMARY);
        accept.setBorder(new EmptyBorder(8, 20, 8, 20));
        Theme.PillButton decline = new Theme.PillButton("Decline", Theme.PillButton.Kind.SECONDARY);
        decline.setBorder(new EmptyBorder(8, 20, 8, 20));
        accept.addActionListener(e -> decide(receivedTable, received, "Accepted"));
        decline.addActionListener(e -> decide(receivedTable, received, "Declined"));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(decline);
        buttons.add(accept);

        JPanel receivedBody = new JPanel(new BorderLayout(0, 12));
        receivedBody.setOpaque(false);
        receivedBody.add(receivedTableCard, BorderLayout.CENTER);
        receivedBody.add(buttons, BorderLayout.SOUTH);

        // sent table
        DefaultTableModel sentModel = readOnlyModel("To", "Project", "Type", "Status");
        for (Invitation inv : sent) {
            sentModel.addRow(new Object[]{
                    nameOf(students, inv.getReceiverId()), projectTitle(pitches, inv), typeOf(pitches, inv), inv.getStatus()});
        }
        JTable sentTable = new JTable(sentModel);
        JComponent sentTableCard = tableCard(sentTable);
        styleInvitationColumns(sentTable);

        JPanel both = new JPanel(new GridLayout(2, 1, 0, 24));
        both.setOpaque(false);
        both.add(wrapSection("Received", receivedBody));
        both.add(wrapSection("Sent", sentTableCard));

        JPanel page = tablePage("Invitations", "Invitations and join requests you have received or sent.");
        page.add(both, BorderLayout.CENTER);
        return page;
    }

    private JComponent wrapSection(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(Theme.label(title, Font.BOLD, 18, Theme.TEXT_PRIMARY), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private void styleInvitationColumns(JTable table) {
        table.getColumnModel().getColumn(3)
                .setCellRenderer(new Theme.CellRenderer(Theme::statusColor, true, SwingConstants.LEFT));
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(1).setPreferredWidth(340);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
    }

    private String projectTitle(Map<Integer, ProjectPitch> pitches, Invitation inv) {
        ProjectPitch p = pitches.get(inv.getProjectId());
        return p == null ? "(deleted project)" : p.getTitle();
    }

    /** "Invitation" if the project's creator sent it, otherwise a "Join request". */
    private String typeOf(Map<Integer, ProjectPitch> pitches, Invitation inv) {
        ProjectPitch p = pitches.get(inv.getProjectId());
        return p != null && p.getCreatorId() == inv.getSenderId() ? "Invitation" : "Join request";
    }

    private void decide(JTable table, List<Invitation> received, String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) {
            info("Select an invitation in the Received table first.", "Nothing selected");
            return;
        }
        Invitation inv = received.get(row);
        if (!"Pending".equals(inv.getStatus())) {
            warn("This one was already " + inv.getStatus().toLowerCase() + ".", "Already answered");
            return;
        }
        DatabaseManager.updateInvitationStatus(inv.getInvitationId(), newStatus);
        showView(INVITATIONS);
    }

    // =====================================================================
    //  5. Profile
    // =====================================================================

    private JComponent buildProfileView() {
        final JTextField contactField = new JTextField(currentUser.getContactInfo());
        final JTextField skillsField = new JTextField(String.join(", ", currentUser.getSkillsList()));
        final JTextField cgpaField = new JTextField(String.valueOf(currentUser.getCgpa()));
        final JTextField interestsField = new JTextField(currentUser.getAreasOfInterest());
        final JCheckBox availableBox = new JCheckBox("I'm available for new projects",
                currentUser.isAvailabilityStatus());
        final JLabel message = Theme.label(" ", Font.PLAIN, 13, Theme.ERROR);
        Theme.styleField(contactField);
        Theme.styleField(skillsField);
        Theme.styleField(cgpaField);
        Theme.styleField(interestsField);
        Theme.styleCheck(availableBox);

        Theme.PillButton save = new Theme.PillButton("Save Changes", Theme.PillButton.Kind.PRIMARY);
        save.addActionListener(e -> {
            double cgpa;
            try {
                cgpa = Double.parseDouble(cgpaField.getText().trim());
            } catch (NumberFormatException ex) {
                message.setForeground(Theme.ERROR);
                message.setText("CGPA must be a number, for example 8.5.");
                return;
            }
            List<String> skills = Theme.parseSkills(skillsField.getText());
            if (cgpa < 0 || cgpa > 10 || skills.isEmpty()) {
                message.setForeground(Theme.ERROR);
                message.setText("CGPA must be 0-10 and you need at least one skill.");
                return;
            }
            currentUser.setContactInfo(contactField.getText().trim());
            currentUser.setSkillsList(skills);
            currentUser.setCgpa(cgpa);
            currentUser.setAreasOfInterest(interestsField.getText().trim());
            currentUser.setAvailabilityStatus(availableBox.isSelected());
            DatabaseManager.updateStudentProfile(currentUser);
            message.setForeground(Theme.MATCH_GREEN);
            message.setText("Profile saved.");
        });

        // header: avatar + name + email
        JPanel who = new JPanel(new GridLayout(3, 1, 0, 2));
        who.setOpaque(false);
        who.add(Theme.label(currentUser.getName(), Font.BOLD, 22, Theme.TEXT_PRIMARY));
        who.add(Theme.label(currentUser.getEmail(), Font.PLAIN, 14, Theme.TEXT_MUTED));
        who.add(Theme.label(currentUser.getDepartment(), Font.PLAIN, 14, Theme.TEXT_SECONDARY));
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        header.add(new Theme.Avatar(currentUser.getName(), 72), BorderLayout.WEST);
        header.add(who, BorderLayout.CENTER);

        JPanel twoCols = new JPanel(new GridLayout(1, 2, 12, 0));
        twoCols.setOpaque(false);
        twoCols.add(Theme.labeled("CGPA (0-10)", cgpaField));
        twoCols.add(Theme.labeled("Areas of Interest", interestsField));

        JPanel form = Theme.column();
        Theme.addRow(form, header, 24);
        Theme.addRow(form, Theme.labeled("Contact Info", contactField), 14);
        Theme.addRow(form, Theme.labeled("Skills (comma separated)", skillsField), 14);
        Theme.addRow(form, twoCols, 12);
        Theme.addRow(form, availableBox, 6);
        Theme.addRow(form, message, 6);
        JPanel saveWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        saveWrap.setOpaque(false);
        saveWrap.add(save);
        Theme.addRow(form, saveWrap, 0);

        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(), 16, Color.WHITE, Theme.BORDER) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(700, d.width), d.height);
            }
        };
        card.setBorder(new EmptyBorder(32, 32, 32, 32));
        card.add(form, BorderLayout.CENTER);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(card);

        JPanel stack = Theme.column();
        Theme.addRow(stack, Theme.pageHeader("Profile", "Keep your skills up to date to get better matches."), 24);
        Theme.addRow(stack, left, 0);

        Theme.ScrollPage page = new Theme.ScrollPage(new BorderLayout());
        page.setBackground(Theme.BG_PAGE);
        page.setBorder(new EmptyBorder(32, 32, 32, 32));
        page.add(stack, BorderLayout.NORTH);
        return Theme.scroll(page);
    }
}
