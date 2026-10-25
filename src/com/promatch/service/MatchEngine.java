package com.promatch.service;

import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchEngine {

    // Computes compatibility score between a student and a project pitch
    public static double calculateMatchScore(Student student, ProjectPitch pitch) {
        List<String> studentSkills = student.getSkillsList();
        List<String> requiredSkills = pitch.getRequiredSkills();

        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 50.0; // Default baseline score if no specific skills required
        }

        int matchCount = 0;
        for (String reqSkill : requiredSkills) {
            for (String studSkill : studentSkills) {
                if (reqSkill.trim().equalsIgnoreCase(studSkill.trim())) {
                    matchCount++;
                    break;
                }
            }
        }

        // Skill match percentage (70% weight)
        double skillScore = ((double) matchCount / requiredSkills.size()) * 70.0;

        // CGPA bonus (30% weight, scaled assuming a 10-point GPA scale)
        double cgpaScore = (student.getCgpa() / 10.0) * 30.0;

        return skillScore + cgpaScore;
    }

    // Ranks available students for a given project pitch
    public static Map<Student, Double> rankStudentsForProject(ProjectPitch pitch, List<Student> allStudents) {
        Map<Student, Double> matchResults = new HashMap<>();

        for (Student student : allStudents) {
            // Only evaluate students who are currently available for collaboration
            if (student.isAvailabilityStatus()) {
                double score = calculateMatchScore(student, pitch);
                matchResults.put(student, score);
            }
        }

        return matchResults;
    }
}
