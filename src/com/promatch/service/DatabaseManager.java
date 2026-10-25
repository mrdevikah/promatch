package com.promatch.service;

import com.promatch.exception.InvalidPitchException;
import com.promatch.model.Invitation;
import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:promatch.db";

    private static final String STUDENT_SELECT =
            "SELECT u.user_id, u.name, u.email, u.department, u.contact_info, " +
            "s.skills_list, s.cgpa, s.areas_of_interest, s.availability_status " +
            "FROM users u JOIN students s ON u.user_id = s.user_id";

    private static final String PITCH_SELECT =
            "SELECT project_id, title, description, required_skills, max_team_size, creator_id " +
            "FROM project_pitches";

    // Initialize Database and Tables
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            // 1. Users Table (Base schema)
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "department TEXT NOT NULL, " +
                    "contact_info TEXT)";
            stmt.execute(sqlUsers);

            // 2. Students Table (Inheritance extension)
            String sqlStudents = "CREATE TABLE IF NOT EXISTS students (" +
                    "user_id INTEGER PRIMARY KEY, " +
                    "skills_list TEXT, " + // Stored as comma-separated values (e.g., "Java,Python,SQL")
                    "cgpa REAL, " +
                    "areas_of_interest TEXT, " +
                    "availability_status BOOLEAN, " +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id))";
            stmt.execute(sqlStudents);

            // 3. Project Pitches Table
            String sqlPitches = "CREATE TABLE IF NOT EXISTS project_pitches (" +
                    "project_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "required_skills TEXT, " +
                    "max_team_size INTEGER, " +
                    "creator_id INTEGER, " +
                    "FOREIGN KEY(creator_id) REFERENCES users(user_id))";
            stmt.execute(sqlPitches);

            // 4. Invitations Table
            String sqlInvitations = "CREATE TABLE IF NOT EXISTS invitations (" +
                    "invitation_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "sender_id INTEGER, " +
                    "receiver_id INTEGER, " +
                    "project_id INTEGER, " +
                    "status TEXT, " +
                    "FOREIGN KEY(sender_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(receiver_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(project_id) REFERENCES project_pitches(project_id))";
            stmt.execute(sqlInvitations);

            // Added for the GUI login: the users table also needs a password column.
            // (Adds it to an existing promatch.db too, so old databases keep working.)
            ensurePasswordColumn(conn);

            System.out.println("Database and tables initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            throw new IllegalStateException("Could not set up the database: " + e.getMessage(), e);
        }

        seedSampleData();
    }

    // Insert a Student into the database (no password - same behaviour as before)
    public static void registerStudent(Student student) {
        try {
            registerStudent(student, null);
            System.out.println("Student registered successfully in database!");
        } catch (IllegalStateException e) {
            System.err.println("Error registering student: " + e.getMessage());
        }
    }

    // Fetch all students (useful for the MatchEngine)
    public static List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(STUDENT_SELECT + " ORDER BY u.name")) {

            while (rs.next()) {
                students.add(readStudent(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching students: " + e.getMessage());
        }
        return students;
    }

    // =====================================================================
    //  Everything below was added for the GUI (the methods above keep the
    //  same names and behaviour as before).
    // =====================================================================

    // ---------------------------------------------------------------- helpers

    private static IllegalStateException dbError(SQLException e) {
        return new IllegalStateException("Database error: " + e.getMessage(), e);
    }

    private static void ensurePasswordColumn(Connection conn) throws SQLException {
        boolean found = false;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(users)")) {
            while (rs.next()) {
                if ("password_hash".equalsIgnoreCase(rs.getString("name"))) {
                    found = true;
                }
            }
        }
        if (!found) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE users ADD COLUMN password_hash TEXT");
            }
        }
    }

    private static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> splitSkills(String text) {
        List<String> list = new ArrayList<>();
        if (text != null) {
            for (String part : text.split(",")) {
                if (!part.trim().isEmpty()) {
                    list.add(part.trim());
                }
            }
        }
        return list;
    }

    private static Student readStudent(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("department"),
                rs.getString("contact_info"),
                splitSkills(rs.getString("skills_list")),
                rs.getDouble("cgpa"),
                rs.getString("areas_of_interest"),
                rs.getBoolean("availability_status"));
    }

    private static ProjectPitch readPitch(ResultSet rs) throws SQLException {
        return new ProjectPitch(
                rs.getInt("project_id"),
                rs.getString("title"),
                rs.getString("description"),
                splitSkills(rs.getString("required_skills")),
                rs.getInt("max_team_size"),
                rs.getInt("creator_id"));
    }

    private static Invitation readInvitation(ResultSet rs) throws SQLException {
        return new Invitation(
                rs.getInt("invitation_id"),
                rs.getInt("sender_id"),
                rs.getInt("receiver_id"),
                rs.getInt("project_id"),
                rs.getString("status"));
    }

    private static int lastInsertId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    // ------------------------------------------------------------------ users

    /** Returns the student for this email + password, or null if they don't match. */
    public static Student login(String email, String password) {
        String sql = STUDENT_SELECT + " WHERE LOWER(u.email) = LOWER(?) AND u.password_hash = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readStudent(rs);
                }
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        return null;
    }

    public static boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /**
     * Saves a student together with a login password (stored as a SHA-256 hash).
     * Inserts into users and students in one transaction and sets the new id on the Student.
     */
    public static void registerStudent(Student student, String password) {
        String insertUser = "INSERT INTO users(name, email, department, contact_info, password_hash) VALUES(?, ?, ?, ?, ?)";
        String insertStudent = "INSERT INTO students(user_id, skills_list, cgpa, areas_of_interest, availability_status) VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(insertUser)) {
                    ps.setString(1, student.getName());
                    ps.setString(2, student.getEmail());
                    ps.setString(3, student.getDepartment());
                    ps.setString(4, student.getContactInfo());
                    ps.setString(5, password == null ? null : hash(password));
                    ps.executeUpdate();
                }
                int newId = lastInsertId(conn);
                student.setUserId(newId);

                try (PreparedStatement ps = conn.prepareStatement(insertStudent)) {
                    ps.setInt(1, newId);
                    ps.setString(2, String.join(",", student.getSkillsList()));
                    ps.setDouble(3, student.getCgpa());
                    ps.setString(4, student.getAreasOfInterest());
                    ps.setBoolean(5, student.isAvailabilityStatus());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Saves the editable profile fields (contact, skills, cgpa, interests, availability). */
    public static void updateStudentProfile(Student s) {
        String updateUser = "UPDATE users SET contact_info = ? WHERE user_id = ?";
        String updateStudent = "UPDATE students SET skills_list = ?, cgpa = ?, areas_of_interest = ?, "
                + "availability_status = ? WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(updateUser)) {
                    ps.setString(1, s.getContactInfo());
                    ps.setInt(2, s.getUserId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(updateStudent)) {
                    ps.setString(1, String.join(",", s.getSkillsList()));
                    ps.setDouble(2, s.getCgpa());
                    ps.setString(3, s.getAreasOfInterest());
                    ps.setBoolean(4, s.isAvailabilityStatus());
                    ps.setInt(5, s.getUserId());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    // ---------------------------------------------------------------- pitches

    /** Validates and saves a pitch. Throws InvalidPitchException if a rule is broken. Returns the new id. */
    public static int addPitch(ProjectPitch p) {
        if (p.getTitle() == null || p.getTitle().trim().isEmpty()) {
            throw new InvalidPitchException("Please give your project a title.");
        }
        if (p.getTitle().trim().length() > 100) {
            throw new InvalidPitchException("The title must be 100 characters or fewer.");
        }
        if (p.getDescription() == null || p.getDescription().trim().isEmpty()) {
            throw new InvalidPitchException("Please add a short summary of the project.");
        }
        if (p.getDescription().trim().length() > 280) {
            throw new InvalidPitchException("The summary must be 280 characters or fewer.");
        }
        if (p.getRequiredSkills() == null || p.getRequiredSkills().isEmpty()) {
            throw new InvalidPitchException("Add at least one required skill.");
        }
        if (p.getMaxTeamSize() < 2 || p.getMaxTeamSize() > 10) {
            throw new InvalidPitchException("Team size must be between 2 and 10.");
        }

        String sql = "INSERT INTO project_pitches(title, description, required_skills, max_team_size, creator_id) "
                + "VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitle().trim());
            ps.setString(2, p.getDescription().trim());
            ps.setString(3, String.join(",", p.getRequiredSkills()));
            ps.setInt(4, p.getMaxTeamSize());
            ps.setInt(5, p.getCreatorId());
            ps.executeUpdate();
            return lastInsertId(conn);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** All pitches, newest first. */
    public static List<ProjectPitch> getAllPitches() {
        List<ProjectPitch> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(PITCH_SELECT + " ORDER BY project_id DESC")) {
            while (rs.next()) {
                list.add(readPitch(rs));
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        return list;
    }

    /** Pitches created by one student, newest first. */
    public static List<ProjectPitch> getPitchesByCreator(int creatorId) {
        List<ProjectPitch> list = new ArrayList<>();
        String sql = PITCH_SELECT + " WHERE creator_id = ? ORDER BY project_id DESC";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, creatorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readPitch(rs));
                }
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        return list;
    }

    /** Projects (made by someone else) that this student joined through an accepted invitation. */
    public static List<ProjectPitch> getJoinedPitches(int userId) {
        List<ProjectPitch> list = new ArrayList<>();
        String sql = "SELECT DISTINCT p.project_id, p.title, p.description, p.required_skills, "
                + "p.max_team_size, p.creator_id "
                + "FROM project_pitches p JOIN invitations i ON i.project_id = p.project_id "
                + "WHERE i.status = 'Accepted' AND (i.sender_id = ? OR i.receiver_id = ?) "
                + "AND p.creator_id <> ? ORDER BY p.project_id DESC";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readPitch(rs));
                }
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        return list;
    }

    /** Number of people who joined a project through accepted invitations (the creator is not counted). */
    public static int countAcceptedMembers(int projectId) {
        String sql = "SELECT COUNT(*) FROM invitations WHERE project_id = ? AND status = 'Accepted'";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    // ------------------------------------------------------------ invitations

    /**
     * Creates a Pending invitation. Returns false (and saves nothing) if the two students
     * already have an invitation or request for this project, or if sender and receiver are the same.
     */
    public static boolean sendInvitation(int senderId, int receiverId, int projectId) {
        if (senderId == receiverId) {
            return false;
        }
        String check = "SELECT 1 FROM invitations WHERE project_id = ? AND "
                + "((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))";
        try (Connection conn = DriverManager.getConnection(URL)) {
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setInt(1, projectId);
                ps.setInt(2, senderId);
                ps.setInt(3, receiverId);
                ps.setInt(4, receiverId);
                ps.setInt(5, senderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }
            insertInvitation(conn, senderId, receiverId, projectId, "Pending");
            return true;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    private static void insertInvitation(Connection conn, int senderId, int receiverId, int projectId,
                                         String status) throws SQLException {
        String sql = "INSERT INTO invitations(sender_id, receiver_id, project_id, status) VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setInt(3, projectId);
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    public static List<Invitation> getInvitationsReceivedBy(int userId) {
        return queryInvitations("SELECT * FROM invitations WHERE receiver_id = ? ORDER BY invitation_id DESC", userId);
    }

    public static List<Invitation> getInvitationsSentBy(int userId) {
        return queryInvitations("SELECT * FROM invitations WHERE sender_id = ? ORDER BY invitation_id DESC", userId);
    }

    private static List<Invitation> queryInvitations(String sql, int userId) {
        List<Invitation> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readInvitation(rs));
                }
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        return list;
    }

    /** status must be "Pending", "Accepted" or "Declined". */
    public static void updateInvitationStatus(int invitationId, String status) {
        String sql = "UPDATE invitations SET status = ? WHERE invitation_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, invitationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    // ------------------------------------------------------------ sample data

    private static int countUsers() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    private static Student sampleStudent(String name, String email, String dept, String contact,
                                         String[] skills, double cgpa, String interests, boolean available) {
        Student s = new Student(0, name, email, dept, contact, Arrays.asList(skills), cgpa, interests, available);
        registerStudent(s, email.startsWith("demo@") ? "demo1234" : "password123");
        return s;
    }

    /** Fills a brand-new (empty) database with a demo account, sample students, pitches and invitations. */
    private static void seedSampleData() {
        if (countUsers() > 0) {
            return;
        }
        final String cse = "Computer Science & Engineering";
        final String ece = "Electronics & Communication Engineering";
        final String eee = "Electrical & Electronics Engineering";

        // Demo login:  demo@gecthrissur.ac.in / demo1234   (the others use password123)
        Student demo = sampleStudent("Demo Student", "demo@gecthrissur.ac.in", cse, "github.com/demo-student",
                new String[]{"Java", "SQL", "Swing", "Git"}, 8.4, "Software engineering", true);
        Student anjali = sampleStudent("Anjali Menon", "anjali@gecthrissur.ac.in", cse, "+91 90000 00001",
                new String[]{"Python", "TensorFlow", "OpenCV", "SQL"}, 9.1, "Machine learning", true);
        Student rahul = sampleStudent("Rahul Nair", "rahul@gecthrissur.ac.in", ece, "+91 90000 00002",
                new String[]{"Embedded C", "MQTT", "PCB Design", "Python"}, 8.2, "IoT", true);
        Student fathima = sampleStudent("Fathima Noor", "fathima@gecthrissur.ac.in", cse, "+91 90000 00003",
                new String[]{"React", "Node.js", "MongoDB", "TypeScript"}, 8.8, "Web development", true);
        Student vishnu = sampleStudent("Vishnu Das", "vishnu@gecthrissur.ac.in", eee, "+91 90000 00004",
                new String[]{"MATLAB", "Signal Processing", "Python", "LabVIEW"}, 7.9, "Signal processing", false);
        Student meera = sampleStudent("Meera Pillai", "meera@gecthrissur.ac.in", cse, "+91 90000 00005",
                new String[]{"Java", "SQL", "Docker", "Git"}, 8.6, "Backend systems", true);

        int portal = addPitch(new ProjectPitch(0, "ProMatch Project Portal",
                "A desktop app that matches students to project pitches by skills and CGPA.",
                Arrays.asList("Java", "SQL", "Swing", "Git"), 5, demo.getUserId()));
        int iot = addPitch(new ProjectPitch(0, "Smart Campus IoT Monitoring System",
                "A sensor network to monitor energy use and conditions across campus buildings.",
                Arrays.asList("Embedded C", "MQTT", "React", "Node.js", "PCB Design"), 5, rahul.getUserId()));
        addPitch(new ProjectPitch(0, "AI-Powered Attendance System",
                "Automated classroom attendance using face recognition.",
                Arrays.asList("Python", "OpenCV", "TensorFlow", "Flask", "SQL"), 4, anjali.getUserId()));
        addPitch(new ProjectPitch(0, "Decentralised Library Resource Exchange",
                "A peer-to-peer platform to lend, borrow and track academic resources.",
                Arrays.asList("React", "TypeScript", "Solidity", "Node.js", "MongoDB"), 4, fathima.getUserId()));
        addPitch(new ProjectPitch(0, "Structural Health Monitoring via Vibration Analysis",
                "Accelerometer arrays and signal processing to detect early micro-cracks.",
                Arrays.asList("MATLAB", "Signal Processing", "Python", "LabVIEW"), 5, vishnu.getUserId()));
        int events = addPitch(new ProjectPitch(0, "Campus Event Management App",
                "One place to publish, discover and register for events on campus.",
                Arrays.asList("Java", "SQL", "Git", "Docker"), 4, meera.getUserId()));

        try (Connection conn = DriverManager.getConnection(URL)) {
            // Meera invites the demo student to her project (pending)
            insertInvitation(conn, meera.getUserId(), demo.getUserId(), events, "Pending");
            // Anjali already joined the demo student's portal project
            insertInvitation(conn, demo.getUserId(), anjali.getUserId(), portal, "Accepted");
            // The demo student asked to join the IoT project (pending)
            insertInvitation(conn, demo.getUserId(), rahul.getUserId(), iot, "Pending");
        } catch (SQLException e) {
            throw dbError(e);
        }
    }
}
