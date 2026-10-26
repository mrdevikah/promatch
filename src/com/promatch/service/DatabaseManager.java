package com.promatch.service;

import com.promatch.exception.InvalidPitchException;
import com.promatch.model.Invitation;
import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class DatabaseManager {
    // Connection details come from db.properties (kept out of Git - see db.properties.example)
    private static final String CONFIG_FILE = "db.properties";
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    // One connection is opened and then reused, because opening a new connection to an
    // online database for every query would make each screen slow.
    private static Connection shared;
    private static long lastUsed;

    private static final String STUDENT_SELECT =
            "SELECT u.user_id, u.name, u.email, u.department, u.contact_info, " +
            "s.skills_list, s.cgpa, s.areas_of_interest, s.availability_status, " +
            "s.work_mode, s.weekly_hours " +
            "FROM users u JOIN students s ON u.user_id = s.user_id";

    /** Longest intro message we store. */
    public static final int MAX_MESSAGE = 300;

    /** Longest skill name we store. */
    public static final int MAX_SKILL = 60;

    /** Skills that are in the autocomplete list from day one (people can add more by typing them). */
    private static final String[] BASE_SKILLS = {
            "Java", "Python", "C", "C++", "C#", "JavaScript", "TypeScript", "HTML", "CSS", "SQL", "MySQL",
            "MongoDB", "React", "Node.js", "Flask", "Django", "Spring Boot", "Swing", "Git", "Docker", "Linux",
            "Embedded C", "Arduino", "Raspberry Pi", "MQTT", "PCB Design", "MATLAB", "LabVIEW",
            "Signal Processing", "TensorFlow", "OpenCV", "Machine Learning", "Data Analysis", "Solidity",
            "Figma", "UI/UX Design", "AutoCAD", "SolidWorks", "Revit", "SketchUp", "STAAD Pro", "ANSYS",
            "Aspen Plus", "3D Printing", "Project Management", "Technical Writing"
    };

    private static final String PITCH_SELECT =
            "SELECT project_id, title, description, required_skills, max_team_size, creator_id, " +
            "work_mode, hours_per_week " +
            "FROM project_pitches";

    // Initialize Database and Tables
    public static void initializeDatabase() {
        loadConfig();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            // 1. Users Table (Base schema)
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255) UNIQUE NOT NULL, " +
                    "department VARCHAR(100) NOT NULL, " +
                    "contact_info VARCHAR(255), " +
                    "password_hash VARCHAR(64))";
            stmt.execute(sqlUsers);

            // 2. Students Table (Inheritance extension)
            String sqlStudents = "CREATE TABLE IF NOT EXISTS students (" +
                    "user_id INT PRIMARY KEY, " +
                    "skills_list TEXT, " + // Stored as comma-separated values (e.g., "Java,Python,SQL")
                    "cgpa REAL, " +
                    "areas_of_interest TEXT, " +
                    "availability_status BOOLEAN, " +
                    "work_mode VARCHAR(10), " +          // Online / Offline / Hybrid
                    "weekly_hours INT, " +               // hours per week this student can give
                    "FOREIGN KEY(user_id) REFERENCES users(user_id))";
            stmt.execute(sqlStudents);

            // 3. Project Pitches Table
            String sqlPitches = "CREATE TABLE IF NOT EXISTS project_pitches (" +
                    "project_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(150) NOT NULL, " +
                    "description TEXT, " +
                    "required_skills TEXT, " +
                    "max_team_size INT, " +
                    "creator_id INT, " +
                    "work_mode VARCHAR(10), " +          // Online / Offline / Hybrid
                    "hours_per_week INT, " +             // hours per week expected from each member
                    "FOREIGN KEY(creator_id) REFERENCES users(user_id))";
            stmt.execute(sqlPitches);

            // 4. Invitations Table
            String sqlInvitations = "CREATE TABLE IF NOT EXISTS invitations (" +
                    "invitation_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sender_id INT, " +
                    "receiver_id INT, " +
                    "project_id INT, " +
                    "status VARCHAR(20), " +
                    "FOREIGN KEY(sender_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(receiver_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(project_id) REFERENCES project_pitches(project_id))";
            stmt.execute(sqlInvitations);

            // 5. Skills table: one row per skill name, used for autocomplete
            String sqlSkills = "CREATE TABLE IF NOT EXISTS skills (" +
                    "skill_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(" + MAX_SKILL + ") NOT NULL UNIQUE)";
            stmt.execute(sqlSkills);

            // 6. Migrations: add columns that older databases don't have yet.
            //    (CREATE TABLE IF NOT EXISTS never changes a table that already exists.)
            addColumnIfMissing(conn, "invitations", "message", "TEXT");
            addColumnIfMissing(conn, "students", "work_mode", "VARCHAR(10)");
            addColumnIfMissing(conn, "students", "weekly_hours", "INT");
            addColumnIfMissing(conn, "project_pitches", "work_mode", "VARCHAR(10)");
            addColumnIfMissing(conn, "project_pitches", "hours_per_week", "INT");

            // 7. Make sure every skill already used by someone is in the skills table
            backfillSkills(conn);

            System.out.println("Database and tables initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            throw new IllegalStateException("Could not set up the database: " + e.getMessage(), e);
        }

        seedSampleData();
    }

    /**
     * Copies the starter skills plus every skill already used in students.skills_list and
     * project_pitches.required_skills into the skills table. INSERT IGNORE skips ones that exist.
     * Runs at every start-up, so skills added by an older version of the app are picked up too.
     */
    private static void backfillSkills(Connection conn) throws SQLException {
        Set<String> found = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        found.addAll(Arrays.asList(BASE_SKILLS));
        String[] queries = {"SELECT skills_list FROM students", "SELECT required_skills FROM project_pitches"};
        for (String q : queries) {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
                while (rs.next()) {
                    found.addAll(splitSkills(rs.getString(1)));
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO skills(name) VALUES(?)")) {
            for (String name : found) {
                if (name.length() <= MAX_SKILL) {
                    ps.setString(1, name);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    /** Every known skill, A to Z. */
    public static List<String> getAllSkills() {
        List<String> list = new ArrayList<>();
        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM skills ORDER BY name")) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        return list;
    }

    /**
     * Cleans up typed skills and saves any new ones to the skills table.
     * A skill that already exists (any capitals) is replaced by the stored spelling, so "react"
     * becomes "React" and nobody ends up with two versions. Duplicates in the list are removed.
     */
    public static List<String> canonicalizeSkills(List<String> typed) {
        List<String> result = new ArrayList<>();
        try (Connection conn = connect()) {
            for (String raw : typed) {
                String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
                if (name.isEmpty()) {
                    continue;
                }
                if (name.length() > MAX_SKILL) {
                    name = name.substring(0, MAX_SKILL).trim();
                }
                String stored = findSkill(conn, name);
                if (stored == null) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO skills(name) VALUES(?)")) {
                        ps.setString(1, name);
                        ps.executeUpdate();
                    }
                    stored = findSkill(conn, name);
                    if (stored == null) {
                        stored = name;
                    }
                }
                boolean duplicate = false;
                for (String have : result) {
                    if (have.equalsIgnoreCase(stored)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    result.add(stored);
                }
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        return result;
    }

    private static String findSkill(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM skills WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Adds a column to an existing table, only if it is not there yet. */
    private static void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        String check = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("Added column " + table + "." + column);
        }
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

        try (Connection conn = connect();
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

    // ------------------------------------------------- connection to MySQL

    /** Reads the database address and login from db.properties. */
    private static void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + CONFIG_FILE + " in the project folder. "
                    + "Copy db.properties.example to db.properties and fill in your database details.", e);
        }
        dbUrl = props.getProperty("db.url");
        dbUser = props.getProperty("db.user");
        dbPassword = props.getProperty("db.password");
        if (dbUrl == null || dbUrl.trim().isEmpty() || dbUser == null || dbPassword == null) {
            throw new IllegalStateException(CONFIG_FILE + " must contain db.url, db.user and db.password.");
        }
    }

    private static Connection open() throws SQLException {
        DriverManager.setLoginTimeout(10);          // give up after 10 seconds instead of hanging
        return DriverManager.getConnection(dbUrl.trim(), dbUser.trim(), dbPassword);
    }

    /**
     * Returns the shared connection, reconnecting if it was closed or went stale.
     * The returned connection ignores close(), so the try-with-resources blocks below
     * can stay as they are without throwing the connection away after every query.
     */
    private static synchronized Connection connect() throws SQLException {
        long now = System.currentTimeMillis();
        if (shared == null || shared.isClosed() || (now - lastUsed > 30_000 && !shared.isValid(3))) {
            shared = open();
        }
        lastUsed = now;
        final Connection real = shared;
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        return null;                 // keep it open for the next query
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /** "hybrid" -> "Hybrid". Returns "" for anything that is not Online, Offline or Hybrid. */
    private static String cleanWorkMode(String mode) {
        if (mode != null) {
            for (String valid : MatchEngine.WORK_MODES) {
                if (valid.equalsIgnoreCase(mode.trim())) {
                    return valid;
                }
            }
        }
        return "";
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
                rs.getBoolean("availability_status"),
                rs.getString("work_mode"),          // NULL (older accounts) becomes ""
                rs.getInt("weekly_hours"));         // NULL becomes 0 = not set
    }

    private static ProjectPitch readPitch(ResultSet rs) throws SQLException {
        return new ProjectPitch(
                rs.getInt("project_id"),
                rs.getString("title"),
                rs.getString("description"),
                splitSkills(rs.getString("required_skills")),
                rs.getInt("max_team_size"),
                rs.getInt("creator_id"),
                rs.getString("work_mode"),
                rs.getInt("hours_per_week"));
    }

    private static Invitation readInvitation(ResultSet rs) throws SQLException {
        return new Invitation(
                rs.getInt("invitation_id"),
                rs.getInt("sender_id"),
                rs.getInt("receiver_id"),
                rs.getInt("project_id"),
                rs.getString("status"),
                rs.getString("message"));
    }

    private static int lastInsertId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    // ------------------------------------------------------------------ users

    /** Returns the student for this email + password, or null if they don't match. */
    public static Student login(String email, String password) {
        String sql = STUDENT_SELECT + " WHERE LOWER(u.email) = LOWER(?) AND u.password_hash = ?";
        try (Connection conn = connect();
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
        try (Connection conn = connect();
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
        String insertStudent = "INSERT INTO students(user_id, skills_list, cgpa, areas_of_interest, availability_status, "
                + "work_mode, weekly_hours) VALUES(?, ?, ?, ?, ?, ?, ?)";

        student.setSkillsList(canonicalizeSkills(student.getSkillsList()));   // saves any new skills, fixes spelling

        try (Connection conn = connect()) {
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
                    ps.setString(6, cleanWorkMode(student.getWorkMode()));
                    ps.setInt(7, student.getWeeklyHours());
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
                + "availability_status = ?, work_mode = ?, weekly_hours = ? WHERE user_id = ?";
        s.setSkillsList(canonicalizeSkills(s.getSkillsList()));               // saves any new skills, fixes spelling
        try (Connection conn = connect()) {
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
                    ps.setString(5, cleanWorkMode(s.getWorkMode()));
                    ps.setInt(6, s.getWeeklyHours());
                    ps.setInt(7, s.getUserId());
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
        if (cleanWorkMode(p.getWorkMode()).isEmpty()) {
            throw new InvalidPitchException("Please choose a work mode: Online, Offline or Hybrid.");
        }
        if (p.getHoursPerWeek() < 1 || p.getHoursPerWeek() > 40) {
            throw new InvalidPitchException("Hours per week must be between 1 and 40.");
        }

        List<String> skills = canonicalizeSkills(p.getRequiredSkills());     // saves any new skills, fixes spelling
        String sql = "INSERT INTO project_pitches(title, description, required_skills, max_team_size, creator_id, "
                + "work_mode, hours_per_week) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitle().trim());
            ps.setString(2, p.getDescription().trim());
            ps.setString(3, String.join(",", skills));
            ps.setInt(4, p.getMaxTeamSize());
            ps.setInt(5, p.getCreatorId());
            ps.setString(6, cleanWorkMode(p.getWorkMode()));
            ps.setInt(7, p.getHoursPerWeek());
            ps.executeUpdate();
            return lastInsertId(conn);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** All pitches, newest first. */
    public static List<ProjectPitch> getAllPitches() {
        List<ProjectPitch> list = new ArrayList<>();
        try (Connection conn = connect();
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
        try (Connection conn = connect();
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
                + "p.max_team_size, p.creator_id, p.work_mode, p.hours_per_week "
                + "FROM project_pitches p JOIN invitations i ON i.project_id = p.project_id "
                + "WHERE i.status = 'Accepted' AND (i.sender_id = ? OR i.receiver_id = ?) "
                + "AND p.creator_id <> ? ORDER BY p.project_id DESC";
        try (Connection conn = connect();
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
        try (Connection conn = connect();
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
        return sendInvitation(senderId, receiverId, projectId, "");
    }

    /** Same as above, with a short intro message (max 300 characters, extra text is cut off). */
    public static boolean sendInvitation(int senderId, int receiverId, int projectId, String message) {
        if (senderId == receiverId) {
            return false;
        }
        String check = "SELECT 1 FROM invitations WHERE project_id = ? AND "
                + "((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))";
        try (Connection conn = connect()) {
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
            insertInvitation(conn, senderId, receiverId, projectId, "Pending", message);
            return true;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    private static void insertInvitation(Connection conn, int senderId, int receiverId, int projectId,
                                         String status) throws SQLException {
        insertInvitation(conn, senderId, receiverId, projectId, status, "");
    }

    private static void insertInvitation(Connection conn, int senderId, int receiverId, int projectId,
                                         String status, String message) throws SQLException {
        String text = message == null ? "" : message.trim();
        if (text.length() > MAX_MESSAGE) {
            text = text.substring(0, MAX_MESSAGE);
        }
        String sql = "INSERT INTO invitations(sender_id, receiver_id, project_id, status, message) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setInt(3, projectId);
            ps.setString(4, status);
            ps.setString(5, text);
            ps.executeUpdate();
        }
    }

    /** The creator plus everyone whose invitation/request for this project was accepted (creator first). */
    public static List<Student> getTeamMembers(int projectId) {
        String sql = STUDENT_SELECT
                + " WHERE u.user_id = (SELECT creator_id FROM project_pitches WHERE project_id = ?)"
                + " OR u.user_id IN (SELECT CASE WHEN i.sender_id = p.creator_id THEN i.receiver_id ELSE i.sender_id END"
                + "   FROM invitations i JOIN project_pitches p ON p.project_id = i.project_id"
                + "   WHERE i.project_id = ? AND i.status = 'Accepted')"
                + " ORDER BY u.name";
        List<Student> members = new ArrayList<>();
        int creatorId = -1;
        try (Connection conn = connect()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT creator_id FROM project_pitches WHERE project_id = ?")) {
                ps.setInt(1, projectId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        creatorId = rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, projectId);
                ps.setInt(2, projectId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        members.add(readStudent(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
        // put the project lead first
        final int lead = creatorId;
        members.sort((a, b) -> Boolean.compare(b.getUserId() == lead, a.getUserId() == lead));
        return members;
    }

    public static List<Invitation> getInvitationsReceivedBy(int userId) {
        return queryInvitations("SELECT * FROM invitations WHERE receiver_id = ? ORDER BY invitation_id DESC", userId);
    }

    public static List<Invitation> getInvitationsSentBy(int userId) {
        return queryInvitations("SELECT * FROM invitations WHERE sender_id = ? ORDER BY invitation_id DESC", userId);
    }

    private static List<Invitation> queryInvitations(String sql, int userId) {
        List<Invitation> list = new ArrayList<>();
        try (Connection conn = connect();
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
        try (Connection conn = connect();
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
        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    private static Student sampleStudent(String name, String email, String dept, String contact,
                                         String[] skills, double cgpa, String interests, boolean available,
                                         String workMode, int weeklyHours) {
        Student s = new Student(0, name, email, dept, contact, Arrays.asList(skills), cgpa, interests, available,
                workMode, weeklyHours);
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
                new String[]{"Java", "SQL", "Swing", "Git"}, 8.4, "Software engineering", true, "Hybrid", 8);
        Student anjali = sampleStudent("Anjali Menon", "anjali@gecthrissur.ac.in", cse, "+91 90000 00001",
                new String[]{"Python", "TensorFlow", "OpenCV", "SQL"}, 9.1, "Machine learning", true, "Online", 6);
        Student rahul = sampleStudent("Rahul Nair", "rahul@gecthrissur.ac.in", ece, "+91 90000 00002",
                new String[]{"Embedded C", "MQTT", "PCB Design", "Python"}, 8.2, "IoT", true, "Offline", 10);
        Student fathima = sampleStudent("Fathima Noor", "fathima@gecthrissur.ac.in", cse, "+91 90000 00003",
                new String[]{"React", "Node.js", "MongoDB", "TypeScript"}, 8.8, "Web development", true, "Online", 7);
        Student vishnu = sampleStudent("Vishnu Das", "vishnu@gecthrissur.ac.in", eee, "+91 90000 00004",
                new String[]{"MATLAB", "Signal Processing", "Python", "LabVIEW"}, 7.9, "Signal processing", false, "Offline", 5);
        Student meera = sampleStudent("Meera Pillai", "meera@gecthrissur.ac.in", cse, "+91 90000 00005",
                new String[]{"Java", "SQL", "Docker", "Git"}, 8.6, "Backend systems", true, "Hybrid", 6);

        int portal = addPitch(new ProjectPitch(0, "ProMatch Project Portal",
                "A desktop app that matches students to project pitches by skills and CGPA.",
                Arrays.asList("Java", "SQL", "Swing", "Git"), 5, demo.getUserId(), "Hybrid", 6));
        int iot = addPitch(new ProjectPitch(0, "Smart Campus IoT Monitoring System",
                "A sensor network to monitor energy use and conditions across campus buildings.",
                Arrays.asList("Embedded C", "MQTT", "React", "Node.js", "PCB Design"), 5, rahul.getUserId(), "Offline", 8));
        addPitch(new ProjectPitch(0, "AI-Powered Attendance System",
                "Automated classroom attendance using face recognition.",
                Arrays.asList("Python", "OpenCV", "TensorFlow", "Flask", "SQL"), 4, anjali.getUserId(), "Online", 6));
        addPitch(new ProjectPitch(0, "Decentralised Library Resource Exchange",
                "A peer-to-peer platform to lend, borrow and track academic resources.",
                Arrays.asList("React", "TypeScript", "Solidity", "Node.js", "MongoDB"), 4, fathima.getUserId(), "Online", 5));
        addPitch(new ProjectPitch(0, "Structural Health Monitoring via Vibration Analysis",
                "Accelerometer arrays and signal processing to detect early micro-cracks.",
                Arrays.asList("MATLAB", "Signal Processing", "Python", "LabVIEW"), 5, vishnu.getUserId(), "Offline", 8));
        int events = addPitch(new ProjectPitch(0, "Campus Event Management App",
                "One place to publish, discover and register for events on campus.",
                Arrays.asList("Java", "SQL", "Git", "Docker"), 4, meera.getUserId(), "Hybrid", 6));

        try (Connection conn = connect()) {
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