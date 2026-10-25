package com.promatch.service;

import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;

import java.util.ArrayList;
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

    // =====================================================================
    //  Added for the GUI (the methods above are unchanged).
    //  The GUI needs results as a SORTED LIST (best match first), because a
    //  HashMap has no order. These reuse calculateMatchScore() above.
    // =====================================================================

    /** An item (a project or a student) together with its compatibility score (0-100). */
    public static class Match<T> {
        private final T item;
        private final int score;

        public Match(T item, int score) {
            this.item = item;
            this.score = score;
        }

        public T getItem() { return item; }
        public int getScore() { return score; }
    }

    /** The required skills (original spelling) that the student already has. */
    public static List<String> getMatchedSkills(List<String> requiredSkills, List<String> studentSkills) {
        List<String> matched = new ArrayList<>();
        for (String req : requiredSkills) {
            for (String have : studentSkills) {
                if (req.trim().equalsIgnoreCase(have.trim())) {
                    matched.add(req);
                    break;
                }
            }
        }
        return matched;
    }

    /** Projects ranked best-first for one student. */
    public static List<Match<ProjectPitch>> rankProjectsForStudent(Student student, List<ProjectPitch> pitches) {
        List<Match<ProjectPitch>> result = new ArrayList<>();
        for (ProjectPitch pitch : pitches) {
            result.add(new Match<>(pitch, (int) Math.round(calculateMatchScore(student, pitch))));
        }
        result.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return result;
    }

    /** All given students ranked best-first for one project (available or not; the GUI can filter). */
    public static List<Match<Student>> rankStudentMatches(ProjectPitch pitch, List<Student> students) {
        List<Match<Student>> result = new ArrayList<>();
        for (Student student : students) {
            result.add(new Match<>(student, (int) Math.round(calculateMatchScore(student, pitch))));
        }
        result.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return result;
    }
}
