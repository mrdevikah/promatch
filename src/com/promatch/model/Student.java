package com.promatch.model;

import java.util.List;

public class Student extends User {
    private List<String> skillsList;
    private double cgpa;
    private String areasOfInterest;
    private boolean availabilityStatus;

    public Student(int userId, String name, String email, String department, String contactInfo, 
                   List<String> skillsList, double cgpa, String areasOfInterest, boolean availabilityStatus) {
        super(userId, name, email, department, contactInfo);
        this.skillsList = skillsList;
        this.cgpa = cgpa;
        this.areasOfInterest = areasOfInterest;
        this.availabilityStatus = availabilityStatus;
    }

    public List<String> getSkillsList() { return skillsList; }
    public void setSkillsList(List<String> skillsList) { this.skillsList = skillsList; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public String getAreasOfInterest() { return areasOfInterest; }
    public void setAreasOfInterest(String areasOfInterest) { this.areasOfInterest = areasOfInterest; }

    public boolean isAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(boolean availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    @Override
    public void displayRoleInfo() {
        System.out.println("Role: Student | Department: " + department + " | CGPA: " + cgpa);
    }
}
