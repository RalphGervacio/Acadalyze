package com.acadalyze.dao.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.acadalyze.beans.tokens.VerificationTokenBean;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class VerificationTokenDAO {

    JdbcTemplate jdbcTemplate;

    public VerificationTokenDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void save(VerificationTokenBean token) {
        String sql = "INSERT INTO verification_token (token, auth_user_id, expiry) VALUES (?, ?, ?)";
        System.out.println("Executing SQL: " + sql);
        System.out.println("Params: " + token.getToken() + ", " + token.getAuthUserId() + ", " + token.getExpiry());

        try {
            jdbcTemplate.update(sql, token.getToken(), token.getAuthUserId(), token.getExpiry());
            System.out.println("Token inserted successfully.");
        } catch (Exception e) {
            System.out.println("Token insert FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public VerificationTokenBean findByToken(String token) {
        String sql = "SELECT * FROM verification_token WHERE token = ?";
        List<VerificationTokenBean> tokens = jdbcTemplate.query(sql, new Object[]{token}, new TokenRowMapper());
        return tokens.isEmpty() ? null : tokens.get(0);
    }

    public void delete(String token) {
        String sql = "DELETE FROM verification_token WHERE token = ?";
        jdbcTemplate.update(sql, token);
    }

    private static class TokenRowMapper implements RowMapper<VerificationTokenBean> {

        @Override
        public VerificationTokenBean mapRow(ResultSet rs, int rowNum) throws SQLException {
            VerificationTokenBean token = new VerificationTokenBean();
            token.setToken(rs.getString("token"));
            token.setAuthUserId(rs.getLong("auth_user_id"));
            token.setExpiry(rs.getTimestamp("expiry"));
            return token;
        }
    }

    public void deleteExpiredTokens() {
        String sql = "DELETE FROM verification_token WHERE expiry < now()";
        jdbcTemplate.update(sql);
    }

    public void deleteExpiredTokensForUser(Long userId) {
        String sql = "DELETE FROM verification_token WHERE auth_user_id = ? AND expiry < now()";
        jdbcTemplate.update(sql, userId);
    }

}
