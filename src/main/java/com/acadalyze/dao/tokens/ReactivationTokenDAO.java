package com.acadalyze.dao.tokens;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.acadalyze.beans.tokens.ReactivationTokenBean;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class ReactivationTokenDAO {

    private final JdbcTemplate jdbcTemplate;

    public ReactivationTokenDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void save(ReactivationTokenBean token) {
        String sql = "INSERT INTO reactivation_token (token, auth_user_id, expiry) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, token.getToken(), token.getAuthUserId(), token.getExpiry());
    }

    public ReactivationTokenBean findByToken(String token) {
        String sql = "SELECT * FROM reactivation_token WHERE token = ?";
        List<ReactivationTokenBean> tokens = jdbcTemplate.query(sql, new Object[]{token}, new ReactivationTokenMapper());
        return tokens.isEmpty() ? null : tokens.get(0);
    }

    public void delete(String token) {
        String sql = "DELETE FROM reactivation_token WHERE token = ?";
        jdbcTemplate.update(sql, token);
    }

    public void deleteExpiredTokens() {
        String sql = "DELETE FROM reactivation_token WHERE expiry < CURRENT_TIMESTAMP";
        jdbcTemplate.update(sql);
    }

    public void deleteExpiredTokensForUser(Long userId) {
        String sql = "DELETE FROM reactivation_token WHERE auth_user_id = ? AND expiry < CURRENT_TIMESTAMP";
        jdbcTemplate.update(sql, userId);
    }

    private static class ReactivationTokenMapper implements RowMapper<ReactivationTokenBean> {

        @Override
        public ReactivationTokenBean mapRow(ResultSet rs, int rowNum) throws SQLException {
            ReactivationTokenBean bean = new ReactivationTokenBean();
            bean.setToken(rs.getString("token"));
            bean.setAuthUserId(rs.getLong("auth_user_id"));
            bean.setExpiry(rs.getTimestamp("expiry"));
            return bean;
        }

    }
}
