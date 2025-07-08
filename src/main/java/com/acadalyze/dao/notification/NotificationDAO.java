package com.acadalyze.dao.notification;

import com.acadalyze.beans.admin.notification.NotificationBean;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class NotificationDAO {

    JdbcTemplate jdbcTemplate;

    public NotificationDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<NotificationBean> notificationRowMapper = new RowMapper<>() {
        @Override
        public NotificationBean mapRow(ResultSet rs, int rowNum) throws SQLException {
            NotificationBean n = new NotificationBean();
            n.setNotificationId(rs.getInt("notification_id"));
            n.setUserId(rs.getInt("user_id"));
            n.setType(rs.getString("type"));
            n.setTitle(rs.getString("title"));
            n.setMessage(rs.getString("message"));
            n.setUrl(rs.getString("url"));
            n.setCreatedAt(rs.getTimestamp("created_at"));
            n.setReadStatus(rs.getBoolean("read_status"));
            n.setExtraData(rs.getString("extra_data"));
            return n;
        }
    };

    public boolean insert(NotificationBean notification) {
        String sql = "INSERT INTO notifications (user_id, type, title, message, url, created_at, read_status, extra_data) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)";
        int rows = jdbcTemplate.update(sql,
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getUrl(),
                notification.getCreatedAt() != null ? notification.getCreatedAt() : new Timestamp(System.currentTimeMillis()),
                notification.isReadStatus(),
                notification.getExtraData() != null ? notification.getExtraData() : "{}"
        );
        return rows > 0;
    }

    public List<Integer> getAllUserIds() {
        String sql = "SELECT auth_user_id FROM users WHERE is_active = true";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("auth_user_id"));
    }

    public List<NotificationBean> getNotificationsByUserId(int userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, notificationRowMapper, userId);
    }

    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET read_status = TRUE WHERE notification_id = ?";
        int rows = jdbcTemplate.update(sql, notificationId);
        return rows > 0;
    }

    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET read_status = TRUE WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, userId);
        return rows > 0;
    }

    public boolean deleteNotification(int notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        int rows = jdbcTemplate.update(sql, notificationId);
        return rows > 0;
    }

    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_status = FALSE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }
}
