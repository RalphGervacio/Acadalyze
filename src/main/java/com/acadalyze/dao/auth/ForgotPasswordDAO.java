package com.acadalyze.dao.auth;

import com.acadalyze.beans.admin.manage_users.ForgotPasswordBean;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class ForgotPasswordDAO {

    @Autowired
    JdbcTemplate jdbc;

    public ForgotPasswordDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(ForgotPasswordBean token) {
        String sql = "INSERT INTO password_reset_token (token, email, expiry) VALUES (?, ?, ?)";
        jdbc.update(sql, token.getToken(), token.getEmail(), Timestamp.valueOf(token.getExpiry()));
    }

    public ForgotPasswordBean findByToken(String token) {
        try {
            String sql = "SELECT * FROM password_reset_token WHERE token = ?";
            return jdbc.queryForObject(sql,
                    (rs, rowNum) -> {
                        ForgotPasswordBean bean = new ForgotPasswordBean();
                        bean.setToken(rs.getString("token"));
                        bean.setEmail(rs.getString("email"));
                        bean.setExpiry(rs.getTimestamp("expiry").toLocalDateTime());
                        return bean;
                    }, token);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void delete(String token) {
        String sql = "DELETE FROM password_reset_token WHERE token = ?";
        jdbc.update(sql, token);
    }
}
