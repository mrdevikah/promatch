package com.promatch.model;

public abstract class User {
    protected int userId;
    protected String name;
    protected String email;
    protected String department;
    protected String contactInfo;

    // Constructor
    public User(int userId, String name, String email, String department, String contactInfo) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.contactInfo = contactInfo;
    }

    // Getters and Setters (Encapsulation)
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    // Abstract method to be overridden if needed
    public abstract void displayRoleInfo();
}
