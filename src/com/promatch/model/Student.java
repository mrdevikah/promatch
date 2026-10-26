package com.promatch.model;

import java.util.List;

public class Student extends User {
    private List<String> skillsList;
    private double cgpa;
    private String areasOfInterest;
    private boolean availabilityStatus;
    private String workMode;      // "Online", "Offline", "Hybrid" or "" if not set yet
    private int weeklyHours;      // hours per week this student can give, 0 if not set yet

    /** Old form (no work mode / hours): both are left "not set". */
    public Student(int userId, String name, String email, String department, String contactInfo, 
                   List<String> skillsList, double cgpa, String areasOfInterest, boolean availabilityStatus) {
        this(userId, name, email, department, contactInfo, skillsList, cgpa, areasOfInterest,
                availabilityStatus, "", 0);
    }

    public Student(int userId, String name, String email, String department, String contactInfo,
                   List<String> skillsList, double cgpa, String areasOfInterest, boolean availabilityStatus,
                   String workMode, int weeklyHours) {
        super(userId, name, email, department, contactInfo);
        this.skillsList = skillsList;
        this.cgpa = cgpa;
        this.areasOfInterest = areasOfInterest;
        this.availabilityStatus = availabilityStatus;
        this.workMode = workMode == null ? "" : workMode;
        this.weeklyHours = weeklyHours;
    }

    public List<String> getSkillsList() { return skillsList; }
    public void setSkillsList(List<String> skillsList) { this.skillsList = skillsList; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public String getAreasOfInterest() { return areasOfInterest; }
    public void setAreasOfInterest(String areasOfInterest) { this.areasOfInterest = areasOfInterest; }

    public boolean isAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(boolean availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode == null ? "" : workMode; }

    public int getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(int weeklyHours) { this.weeklyHours = weeklyHours; }

    @Override
    public void displayRoleInfo() {
        System.out.println("Role: Student | Department: " + department + " | CGPA: " + cgpa);
    }
}