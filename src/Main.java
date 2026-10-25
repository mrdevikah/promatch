import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;
import com.promatch.service.DatabaseManager;
import com.promatch.service.MatchEngine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing ProMatch System...");

        // 1. Initialize Database & Tables
        DatabaseManager.initializeDatabase();

        // 2. Create sample students
        Student student1 = new Student(
            0, "Alice Smith", "alice@college.edu", "Computer Science", "9876543210",
            Arrays.asList("Java", "Python", "SQL", "Git"), 8.5, "AI & Machine Learning", true
        );

        Student student2 = new Student(
            0, "Bob Jones", "bob@college.edu", "Information Technology", "9123456789",
            Arrays.asList("Java", "HTML", "CSS", "JavaScript"), 7.8, "Web Development", true
        );

        // Uncomment the line below to register them into the SQLite database on first run
        // DatabaseManager.registerStudent(student1);
        // DatabaseManager.registerStudent(student2);

        // 3. Fetch all students from Database (Testing Permanent Storage)
        List<Student> students = DatabaseManager.getAllStudents();
        System.out.println("\n--- Loaded Students from Database ---");
        for (Student s : students) {
            System.out.println("ID: " + s.getUserId() + " | Name: " + s.getName() + " | Skills: " + s.getSkillsList());
        }

        // 4. Create a sample Project Pitch
        ProjectPitch samplePitch = new ProjectPitch(
            1, "AI Campus Chatbot", "Building a smart assistant for college queries.",
            Arrays.asList("Python", "Java", "SQL"), 3, 1
        );

        // 5. Run Match Engine & Display Ranked Results
        System.out.println("\n--- Running Match Engine for Project: " + samplePitch.getTitle() + " ---");
        Map<Student, Double> rankedMatches = MatchEngine.rankStudentsForProject(samplePitch, students);

        for (Map.Entry<Student, Double> entry : rankedMatches.entrySet()) {
            Student s = entry.getKey();
            double score = entry.getValue();
            System.out.printf("Student: %s | Match Score: %.2f%%\n", s.getName(), score);
        }
        
        System.out.println("\nProMatch backend engine executed successfully!");
    }
}
