package com.promatch.model;

import java.util.List;

public class ProjectPitch {
    private int projectId;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private int maxTeamSize;
    private int creatorId;
    private String workMode;      // "Online", "Offline", "Hybrid" or "" if not set (older pitches)
    private int hoursPerWeek;     // hours per week expected from each member, 0 if not set

    /** Old form (no work mode / hours): both are left "not set". */
    public ProjectPitch(int projectId, String title, String description, List<String> requiredSkills, int maxTeamSize, int creatorId) {
        this(projectId, title, description, requiredSkills, maxTeamSize, creatorId, "", 0);
    }

    public ProjectPitch(int projectId, String title, String description, List<String> requiredSkills, int maxTeamSize,
                        int creatorId, String workMode, int hoursPerWeek) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.maxTeamSize = maxTeamSize;
        this.creatorId = creatorId;
        this.workMode = workMode == null ? "" : workMode;
        this.hoursPerWeek = hoursPerWeek;
    }

    // Getters and Setters
    public int getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public int getMaxTeamSize() { return maxTeamSize; }
    public int getCreatorId() { return creatorId; }
    public String getWorkMode() { return workMode; }
    public int getHoursPerWeek() { return hoursPerWeek; }
}