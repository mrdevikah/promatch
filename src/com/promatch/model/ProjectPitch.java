package com.promatch.model;

import java.util.List;

public class ProjectPitch {
    private int projectId;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private int maxTeamSize;
    private int creatorId;

    public ProjectPitch(int projectId, String title, String description, List<String> requiredSkills, int maxTeamSize, int creatorId) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.maxTeamSize = maxTeamSize;
        this.creatorId = creatorId;
    }

    // Getters and Setters
    public int getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public int getMaxTeamSize() { return maxTeamSize; }
    public int getCreatorId() { return creatorId; }
}
