package com.acadalyze.beans.tokens;

import java.sql.Timestamp;

/**
 *
 * @author Ralph Gervacio
 */

public class VerificationTokenBean {
    private String token;
    private Long authUserId;
    private Timestamp expiry;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(Long authUserId) {
        this.authUserId = authUserId;
    }

    public Timestamp getExpiry() {
        return expiry;
    }

    public void setExpiry(Timestamp expiry) {
        this.expiry = expiry;
    }
}