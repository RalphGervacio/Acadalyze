package com.acadalyze.beans.admin.manage_users;

import java.sql.Timestamp;

/**
 *
 * @author Ralph Gervacio
 */
public class UsersBean extends RolesBean {

    private Long authUserId;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String userName;
    private String email;
    private String password;
    private String studentId;
    private RolesBean role;
    private boolean isVerified;
    private String verificationToken;
    private byte[] profileImage;
    private byte[] coverImage;
    private boolean isActive;
    private String bio;
    private String createdBy;
    private Timestamp createdAt;

    // Fields to use for Messages
    private String lastMessage;
    private Long lastSenderId;
    private Timestamp lastMessageTime;
    private String lastMessageTimeFormatted;
    private Integer unreadCount;
    private Long lastUnreadMessageId;
    private String profileImageBase64;
    private String profileImageMimeType;

    // Getters and Setters
    public Long getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(Long authUserId) {
        this.authUserId = authUserId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public RolesBean getRole() {
        return role;
    }

    public void setRole(RolesBean role) {
        this.role = role;
    }

    public boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }

    public byte[] getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(byte[] coverImage) {
        this.coverImage = coverImage;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Long getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(Long lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessageTimeFormatted() {
        return lastMessageTimeFormatted;
    }

    public void setLastMessageTimeFormatted(String lastMessageTimeFormatted) {
        this.lastMessageTimeFormatted = lastMessageTimeFormatted;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Long getLastUnreadMessageId() {
        return lastUnreadMessageId;
    }

    public void setLastUnreadMessageId(Long lastUnreadMessageId) {
        this.lastUnreadMessageId = lastUnreadMessageId;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }

    public String getProfileImageMimeType() {
        return profileImageMimeType;
    }

    public void setProfileImageMimeType(String profileImageMimeType) {
        this.profileImageMimeType = profileImageMimeType;
    }

}
