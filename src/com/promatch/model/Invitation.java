package com.promatch.model;

public class Invitation {
    private int invitationId;
    private int senderId;
    private int receiverId;
    private int projectId;
    private String status; // "Pending", "Accepted", "Declined"

    public Invitation(int invitationId, int senderId, int receiverId, int projectId, String status) {
        this.invitationId = invitationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.projectId = projectId;
        this.status = status;
    }

    public int getInvitationId() { return invitationId; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public int getProjectId() { return projectId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
