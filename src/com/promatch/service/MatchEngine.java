package com.promatch.service;

import com.promatch.model.ProjectPitch;
import com.promatch.model.Student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MatchEngine {

    // =====================================================================
    //  HOW THE MATCH SCORE WORKS (0-100)
    //  Five parts, each worth a fixed number of points. The weights add up to 100,
    //  so change them here (and only here) if you want a different balance.
    // =====================================================================
    public static final int W_SKILLS = 55;      // how many of the project's required skills you have
    public static final int W_INTERESTS = 15;   // do your areas of interest show up in the project?
    public static final int W_MODE = 10;        // online / offline / hybrid compatibility
    public static final int W_HOURS = 10;       // can you give the weekly hours the project needs?
    public static final int W_CGPA = 10;        // small bonus for CGPA (out of 10)

    /** The choices for work mode, used by every form. */
    public static final String[] WORK_MODES = {"Online", "Offline", "Hybrid"};

    /** When one side hasn't filled something in, that part gets half its points (no reward, no penalty). */
    private static final double UNKNOWN = 0.5;

    /** Words that say nothing about a topic, so they never count as an interest match. */
    private static final Set<String> IGNORED_WORDS = new HashSet<>(Arrays.asList(
            "and", "the", "for", "with", "from", "into", "project", "based", "using", "engineering",
            "system", "development", "design", "app", "application", "general", "technology"));

    /** The score split into its five parts (each already rounded to whole points). */
    public static class Breakdown {
        public final int skills, interests, mode, hours, cgpa;

        Breakdown(int skills, int interests, int mode, int hours, int cgpa) {
            this.skills = skills;
            this.interests = interests;
            this.mode = mode;
            this.hours = hours;
            this.cgpa = cgpa;
        }

        public int total() {
            return skills + interests + mode + hours + cgpa;
        }

        /** One line for the GUI, e.g. "Skills 33/55  ·  Interests 15/15  ·  ..." */
        public String describe() {
            return "Skills " + skills + "/" + W_SKILLS
                    + "   \u00B7   Interests " + interests + "/" + W_INTERESTS
                    + "   \u00B7   Work mode " + mode + "/" + W_MODE
                    + "   \u00B7   Hours " + hours + "/" + W_HOURS
                    + "   \u00B7   CGPA " + cgpa + "/" + W_CGPA;
        }
    }

    /** The five parts of the score for one student and one project. */
    public static Breakdown breakdown(Student student, ProjectPitch pitch) {
        return new Breakdown(
                points(skillsFraction(student, pitch), W_SKILLS),
                points(interestsFraction(student, pitch), W_INTERESTS),
                points(modeFraction(student.getWorkMode(), pitch.getWorkMode()), W_MODE),
                points(hoursFraction(student.getWeeklyHours(), pitch.getHoursPerWeek()), W_HOURS),
                points(student.getCgpa() / 10.0, W_CGPA));
    }

    // Computes compatibility score (0-100) between a student and a project pitch
    public static double calculateMatchScore(Student student, ProjectPitch pitch) {
        return breakdown(student, pitch).total();
    }

    private static int points(double fraction, int weight) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        return (int) Math.round(f * weight);
    }

    /** Share of the required skills the student has (1.0 = has all of them). */
    private static double skillsFraction(Student student, ProjectPitch pitch) {
        List<String> required = pitch.getRequiredSkills();
        if (required == null || required.isEmpty()) {
            return UNKNOWN;
        }
        int matchCount = getMatchedSkills(required, student.getSkillsList()).size();
        return (double) matchCount / required.size();
    }

    /**
     * Do the student's interests appear in the project's title, summary or skills?
     * Interests are comma separated, e.g. "IoT, machine learning". An interest counts as found if
     * any of its meaningful words appears in the project text. One interest found is a full score
     * for someone with one interest; with two or more interests, two found is a full score.
     * (This is keyword matching, so "AI" will not find a project that only says "neural network".)
     */
    private static double interestsFraction(Student student, ProjectPitch pitch) {
        String interests = student.getAreasOfInterest();
        if (interests == null || interests.trim().isEmpty()) {
            return UNKNOWN;
        }
        Set<String> projectWords = words(pitch.getTitle() + " " + pitch.getDescription() + " "
                + String.join(" ", pitch.getRequiredSkills()));
        int phrases = 0;
        int found = 0;
        for (String phrase : interests.split("[,;]")) {
            if (phrase.trim().isEmpty()) {
                continue;
            }
            phrases++;
            for (String word : words(phrase)) {
                if (!IGNORED_WORDS.contains(word) && projectWords.contains(word)) {
                    found++;
                    break;
                }
            }
        }
        if (phrases == 0) {
            return UNKNOWN;
        }
        return Math.min(1.0, (double) found / Math.min(phrases, 2));
    }

    /** Lower-case words of a text, with a trailing "s" removed ("systems" -> "system"). */
    static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        if (text == null) {
            return out;
        }
        for (String w : text.toLowerCase().split("[^a-z0-9+#]+")) {
            if (w.length() > 3 && w.endsWith("s")) {
                w = w.substring(0, w.length() - 1);
            }
            if (w.length() >= 2) {
                out.add(w);
            }
        }
        return out;
    }

    /** Same mode = 1.0, one side Hybrid = 0.5, Online vs Offline = 0, anything missing = unknown. */
    static double modeFraction(String studentMode, String projectMode) {
        if (isBlank(studentMode) || isBlank(projectMode)) {
            return UNKNOWN;
        }
        if (studentMode.equalsIgnoreCase(projectMode)) {
            return 1.0;
        }
        if (studentMode.equalsIgnoreCase("Hybrid") || projectMode.equalsIgnoreCase("Hybrid")) {
            return 0.5;
        }
        return 0.0;
    }

    /** Enough hours = 1.0, otherwise the share of the needed hours the student can give. */
    static double hoursFraction(int studentHours, int projectHours) {
        if (studentHours <= 0 || projectHours <= 0) {
            return UNKNOWN;
        }
        return Math.min(1.0, (double) studentHours / projectHours);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
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