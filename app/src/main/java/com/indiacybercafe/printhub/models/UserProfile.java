package com.indiacybercafe.printhub.models;

import java.io.Serializable;

public class UserProfile implements Serializable {
    private String fullName;
    private String mobile;
    private String email;
    private String photo;
    private String gender;
    private String profession;
    private Long createdAt;
    private Long updatedAt;

    public UserProfile() {
        // Required for Firebase
    }

    public UserProfile(String fullName, String mobile, String email, String photo, String gender, String profession, Long createdAt, Long updatedAt) {
        this.fullName = fullName;
        this.mobile = mobile;
        this.email = email;
        this.photo = photo;
        this.gender = gender;
        this.profession = profession;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
