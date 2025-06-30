package com.example.knjigomatpis.models;

import com.google.gson.annotations.SerializedName;

public class Auth0User {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private String name;

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("picture")
    private String picture;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("email_verified")
    private boolean emailVerified;

    @SerializedName("user_metadata")
    private Auth0UserMetadata userMetadata;

    @SerializedName("app_metadata")
    private Object appMetadata;

    // Constructors
    public Auth0User() {}

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public Auth0UserMetadata getUserMetadata() { return userMetadata; }
    public void setUserMetadata(Auth0UserMetadata userMetadata) { this.userMetadata = userMetadata; }

    public Object getAppMetadata() { return appMetadata; }
    public void setAppMetadata(Object appMetadata) { this.appMetadata = appMetadata; }
}

