package com.promatch.service;

import com.promatch.model.Student;
import com.promatch.model.ProjectPitch;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:promatch.db";

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

            System.out.println("Database and tables initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    // Insert a Student into the database
    public static void registerStudent(Student student) {
        String insertUser = "INSERT INTO users(name, email, department, contact_info) VALUES(?, ?, ?, ?)";
        String insertStudent = "INSERT INTO students(user_id, skills_list, cgpa, areas_of_interest, availability_status) VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false); // Transaction block for safety

            try (PreparedStatement pstmtUser = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                pstmtUser.setString(1, student.getName());
                pstmtUser.setString(2, student.getEmail());
                pstmtUser.setString(3, student.getDepartment());
                pstmtUser.setString(4, student.getContactInfo());
                pstmtUser.executeUpdate();

                ResultSet rs = pstmtUser.getGeneratedKeys();
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    student.setUserId(generatedId);

                    try (PreparedStatement pstmtStudent = conn.prepareStatement(insertStudent)) {
                        pstmtStudent.setInt(1, generatedId);
                        // Convert List<String> to comma-separated String for storage
                        pstmtStudent.setString(2, String.join(",", student.getSkillsList()));
                        pstmtStudent.setDouble(3, student.getCgpa());
                        pstmtStudent.setString(4, student.getAreasOfInterest());
                        pstmtStudent.setBoolean(5, student.isAvailabilityStatus());
                        pstmtStudent.executeUpdate();
                    }
                }
                conn.commit();
                System.out.println("Student registered successfully in database!");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error registering student: " + e.getMessage());
        }
    }

    // Fetch all students (useful for the MatchEngine)
    public static List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String query = "SELECT u.user_id, u.name, u.email, u.department, u.contact_info, " +
                       "s.skills_list, s.cgpa, s.areas_of_interest, s.availability_status " +
                       "FROM users u JOIN students s ON u.user_id = s.user_id";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("user_id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                String dept = rs.getString("department");
                String contact = rs.getString("contact_info");
                
                // Convert stored comma-separated string back to List<String>
                String skillsStr = rs.getString("skills_list");
                List<String> skills = skillsStr != null && !skillsStr.isEmpty() ? 
                                      Arrays.asList(skillsStr.split(",")) : new ArrayList<>();
                
                double cgpa = rs.getDouble("cgpa");
                String interests = rs.getString("areas_of_interest");
                boolean available = rs.getBoolean("availability_status");

                students.add(new Student(id, name, email, dept, contact, skills, cgpa, interests, available));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching students: " + e.getMessage());
        }
        return students;
    }
}
