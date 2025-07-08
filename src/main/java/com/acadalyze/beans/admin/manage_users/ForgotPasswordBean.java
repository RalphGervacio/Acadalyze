package com.acadalyze.beans.admin.manage_users;

import java.time.LocalDateTime;

/**
 *
 * @author Ralph Gervacio
 */
public class ForgotPasswordBean {

    private String token;
    private String email;
    private LocalDateTime expiry;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(LocalDateTime expiry) {
        this.expiry = expiry;
    }

}
