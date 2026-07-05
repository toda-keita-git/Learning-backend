package com.udemy.hello.model;

import java.time.LocalDateTime;

public class User {

    private Integer id;               // 主キー
    private String githubId;          // GitHubアカウントのID
    private String username;          // GitHubユーザー名
    private String email;             // メールアドレス
    private String avatarUrl;         // GitHubアイコンURL
    private String accessToken;       // OAuthアクセストークン
    private LocalDateTime createdAt;  // 作成日時
    private Boolean deleteFlg;        // 論理削除フラグ（true:削除）
    private String githubLogin;
    private boolean createdRepo;

    // --- Getter / Setter ---
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getDeleteFlg() {
        return deleteFlg;
    }

    public void setDeleteFlg(Boolean deleteFlg) {
        this.deleteFlg = deleteFlg;
    }

    public String getGithubLogin() {
        return githubLogin;
    }
    public void setGithubLogin(String githubLogin) {
        this.githubLogin = githubLogin;
    }

    // createdRepo の getter/setter
    public boolean getCreatedRepo() {
        return createdRepo;
    }
    public void setCreatedRepo(boolean createdRepo) {
        this.createdRepo = createdRepo;
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", githubId='" + githubId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", createdAt=" + createdAt +
                ", deleteFlg=" + deleteFlg +
                '}';
    }
}
