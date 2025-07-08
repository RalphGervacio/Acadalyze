package com.acadalyze.dao.messages;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.beans.messages.LightweightMessageBean;
import com.acadalyze.beans.messages.MessageBean;
import java.sql.PreparedStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * DAO for handling all messaging-related database operations. Includes user
 * search, message sending, chat heads, conversations, and status tracking.
 */
@Repository
public class MessagesDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MessagesDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ============================
    // Message Mappers
    // ============================
    private MessageBean mapMessage(ResultSet rs, int rowNum) throws SQLException {
        MessageBean msg = new MessageBean();
        msg.setMessageId(rs.getLong("message_id"));
        msg.setSenderId(rs.getLong("sender_id"));
        msg.setReceiverId(rs.getLong("receiver_id"));
        msg.setSenderName(rs.getString("sender_name"));
        msg.setProfileImage(rs.getString("profile_image"));
        msg.setMessageContent(rs.getString("message_content"));

        Timestamp sentAt = rs.getTimestamp("sent_at");
        if (sentAt != null) {
            msg.setSentAt(new SimpleDateFormat("yyyy-MM-dd hh:mm a").format(sentAt));
        }

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        Timestamp seenAt = rs.getTimestamp("seen_at");
        msg.setSeenAt(seenAt);
        msg.setDeletedAt(deletedAt);

        return msg;
    }

    // ============================
    // Fetching Messages & Conversations
    // ============================
    public List<MessageBean> getMessagesForUser(Long userId) {
        String sql = "SELECT m.message_id, m.sender_id, m.receiver_id, "
                + "CONCAT_WS(' ', u.first_name, u.middle_name, u.last_name) AS sender_name, "
                + "u.profile_image, m.sent_at, m.message_content, m.deleted_at, m.seen_at "
                + "FROM messages m "
                + "JOIN users u ON m.sender_id = u.auth_user_id "
                + "WHERE m.receiver_id = ? "
                + "ORDER BY m.sent_at DESC";
        return jdbcTemplate.query(sql, this::mapMessage, userId);
    }

    public List<MessageBean> getConversation(Long user1Id, Long user2Id) {
        String sql = "SELECT m.message_id, m.sender_id, m.receiver_id, "
                + "CONCAT_WS(' ', u.first_name, u.middle_name, u.last_name) AS sender_name, "
                + "u.profile_image, m.sent_at, m.message_content, m.deleted_at, m.seen_at "
                + "FROM messages m "
                + "JOIN users u ON m.sender_id = u.auth_user_id "
                + "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) "
                + "ORDER BY m.sent_at ASC";
        return jdbcTemplate.query(sql, this::mapMessage, user1Id, user2Id, user2Id, user1Id);
    }

    public List<MessageBean> getConversationBetween(Long senderId, Long receiverId) {
        String sql = "SELECT message_id, sender_id, receiver_id, message_content, attachment_name, attachment_type, sent_at, deleted_at, seen_at "
                + "FROM messages "
                + "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) "
                + "ORDER BY sent_at ASC";
        return jdbcTemplate.query(sql, new Object[]{senderId, receiverId, receiverId, senderId}, (rs, rowNum) -> {
            MessageBean msg = new MessageBean();
            msg.setMessageId(rs.getLong("message_id"));
            msg.setSenderId(rs.getLong("sender_id"));
            msg.setReceiverId(rs.getLong("receiver_id"));
            msg.setMessageContent(rs.getString("message_content"));
            msg.setAttachmentName(rs.getString("attachment_name"));
            msg.setAttachmentType(rs.getString("attachment_type"));

            Timestamp ts = rs.getTimestamp("sent_at");
            msg.setSentAt(ts != null ? ts.toInstant().toString() : null);

            msg.setDeletedAt(rs.getTimestamp("deleted_at"));
            msg.setSeenAt(rs.getTimestamp("seen_at"));
            return msg;
        });
    }

    public MessageBean getMessageById(Long messageId) {
        String sql = """
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.message_content,
            m.attachment,
            m.attachment_name,
            m.attachment_type,
            m.sent_at,
            m.deleted_at,
            m.seen_at,
            CONCAT_WS(' ', u.first_name, u.middle_name, u.last_name) AS sender_name
        FROM messages m
        JOIN users u ON u.auth_user_id = m.sender_id
        WHERE m.message_id = ?
        """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            MessageBean msg = new MessageBean();
            msg.setMessageId(rs.getLong("message_id"));
            msg.setSenderId(rs.getLong("sender_id"));
            msg.setReceiverId(rs.getLong("receiver_id"));
            msg.setMessageContent(rs.getString("message_content"));
            msg.setAttachment(rs.getBytes("attachment"));
            msg.setAttachmentName(rs.getString("attachment_name"));
            msg.setAttachmentType(rs.getString("attachment_type"));
            msg.setSentAt(rs.getTimestamp("sent_at").toString());
            msg.setDeletedAt(rs.getTimestamp("deleted_at"));
            msg.setSeenAt(rs.getTimestamp("seen_at"));
            msg.setSenderName(rs.getString("sender_name"));

            return msg;
        }, messageId);
    }

    public List<LightweightMessageBean> getRecentUnreadPreview(Long userId, int limit) {
        String sql = "SELECT m.message_id, m.sender_id, m.message_content, m.attachment_type, m.sent_at, "
                + "CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS sender_name "
                + "FROM messages m "
                + "JOIN users u ON u.auth_user_id = m.sender_id "
                + "WHERE m.receiver_id = ? AND m.is_read = false "
                + "ORDER BY m.sent_at DESC LIMIT ?";

        return jdbcTemplate.query(sql, new Object[]{userId, limit},
                new BeanPropertyRowMapper<>(LightweightMessageBean.class));
    }

    public boolean isNewConversation(Long senderId, Long receiverId) {
        String sql = "SELECT COUNT(*) FROM messages "
                + "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, senderId, receiverId, receiverId, senderId);
        return count != null && count <= 1;
    }

    // ============================
    // Chat Heads & User Info
    // ============================
    public List<UsersBean> getChatHeadsForUser(Long userId) {
        String sql
                = "SELECT u.auth_user_id, "
                + "       CONCAT_WS(' ', u.first_name, u.middle_name, u.last_name) AS full_name, "
                + "       u.profile_image, "
                + "       CASE "
                + "           WHEN m.message_content IS NOT NULL AND m.message_content != '[file]' AND m.attachment_type IS NOT NULL "
                + "               THEN CONCAT(m.message_content, ' 📎') "
                + "           WHEN m.message_content IS NOT NULL AND m.message_content != '[file]' "
                + "               THEN m.message_content "
                + "           WHEN m.attachment_type IS NOT NULL THEN "
                + "               CASE "
                + "                   WHEN m.attachment_type LIKE 'image/%' THEN 'sent a photo' "
                + "                   WHEN m.attachment_type LIKE 'video/%' THEN 'sent a video' "
                + "                   WHEN m.attachment_type LIKE 'audio/%' THEN 'sent an audio clip' "
                + "                   WHEN m.attachment_type IN ('application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') THEN 'sent a document' "
                + "                   WHEN m.attachment_type IN ('application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') THEN 'sent a spreadsheet' "
                + "                   WHEN m.attachment_type IN ('application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation') THEN 'sent a presentation' "
                + "                   WHEN m.attachment_type IN ('text/plain', 'text/csv') THEN 'sent a text file' "
                + "                   WHEN m.attachment_type = 'application/zip' THEN 'sent a zip file' "
                + "                   WHEN m.attachment_type IN ('application/x-msdownload', 'application/x-msdos-program', 'application/x-ms-installer', 'application/x-exe', 'application/octet-stream') THEN NULL "
                + "                   ELSE 'sent a file' "
                + "               END "
                + "           ELSE NULL "
                + "       END AS last_message, "
                + "       m.sender_id AS last_sender_id, "
                + "       m.sent_at AS last_message_time, "
                + "       m.attachment_type, "
                + "       (SELECT COUNT(*) FROM messages m4 "
                + "        WHERE m4.sender_id = u.auth_user_id AND m4.receiver_id = ? AND m4.is_read = false) AS unread_count, "
                + "       (SELECT m5.message_id FROM messages m5 "
                + "        WHERE m5.sender_id = u.auth_user_id AND m5.receiver_id = ? AND m5.is_read = false "
                + "        ORDER BY m5.sent_at DESC LIMIT 1) AS last_unread_message_id "
                + "FROM users u "
                + "JOIN ( "
                + "    SELECT * FROM messages "
                + "    WHERE sender_id = ? OR receiver_id = ? "
                + ") m ON ( "
                + "    (m.sender_id = u.auth_user_id AND m.receiver_id = ?) OR "
                + "    (m.receiver_id = u.auth_user_id AND m.sender_id = ?) "
                + ") "
                + "WHERE u.auth_user_id != ? "
                + "AND m.sent_at = ( "
                + "    SELECT MAX(sent_at) "
                + "    FROM messages "
                + "    WHERE (sender_id = m.sender_id AND receiver_id = m.receiver_id) "
                + "       OR (sender_id = m.receiver_id AND receiver_id = m.sender_id) "
                + ") "
                + "GROUP BY u.auth_user_id, u.first_name, u.middle_name, u.last_name, u.profile_image, "
                + "         m.message_content, m.sender_id, m.sent_at, m.attachment_type "
                + "ORDER BY last_message_time DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UsersBean user = new UsersBean();
            user.setAuthUserId(rs.getLong("auth_user_id"));
            user.setFullName(rs.getString("full_name"));
            user.setProfileImage(rs.getBytes("profile_image"));

            String lastMsg = rs.getString("last_message");
            user.setLastMessage(lastMsg != null && lastMsg.length() > 32 ? lastMsg.substring(0, 32) + "..." : lastMsg);

            user.setLastSenderId(rs.getLong("last_sender_id"));
            Timestamp lastMessageTime = rs.getTimestamp("last_message_time");
            user.setLastMessageTime(lastMessageTime);

            if (lastMessageTime != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
                user.setLastMessageTimeFormatted(sdf.format(lastMessageTime));
            } else {
                user.setLastMessageTimeFormatted("");
            }

            user.setUnreadCount(rs.getInt("unread_count"));
            long lastUnreadMsgId = rs.getLong("last_unread_message_id");
            user.setLastUnreadMessageId(rs.wasNull() ? null : lastUnreadMsgId);

            return user;
        }, userId, userId, userId, userId, userId, userId, userId);
    }

    public UsersBean getUserById(Long userId) {
        String sql = """
                SELECT u.auth_user_id,
                       CONCAT_WS(' ', u.first_name, u.middle_name, u.last_name) AS full_name,
                       u.email, u.profile_image
                FROM users u
                WHERE u.auth_user_id = ?
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            UsersBean user = new UsersBean();
            user.setAuthUserId(rs.getLong("auth_user_id"));
            user.setFullName(rs.getString("full_name"));
            user.setEmail(rs.getString("email"));
            user.setProfileImage(rs.getBytes("profile_image"));
            return user;
        }, userId);
    }

    // ============================
    // Sending & Searching
    // ============================
    public Long sendMessage(MessageBean message) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String sql
                = "INSERT INTO messages ("
                + "sender_id, receiver_id, message_content, "
                + "attachment, attachment_name, attachment_type, "
                + "is_read, sent_at"
                + ") VALUES (?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP) "
                + "RETURNING message_id";

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, message.getSenderId());
            ps.setLong(2, message.getReceiverId());
            ps.setString(3, message.getMessageContent());
            ps.setBytes(4, message.getAttachment());
            ps.setString(5, message.getAttachmentName());
            ps.setString(6, message.getAttachmentType());
            return ps;
        }, keyHolder);

        return ((Number) keyHolder.getKeys().get("message_id")).longValue();
    }

    public List<UsersBean> searchUsersByKeyword(Long currentUserId, String keyword) {
        String like = "%" + keyword.toLowerCase() + "%";

        String sql = "SELECT auth_user_id, "
                + "       first_name || ' ' || middle_name || ' ' || last_name AS full_name, "
                + "       profile_image "
                + "FROM users "
                + "WHERE LOWER(first_name || ' ' || middle_name || ' ' || last_name) LIKE ? "
                + "  AND auth_user_id <> ? "
                + "ORDER BY first_name ASC "
                + "LIMIT 10";

        return jdbcTemplate.query(sql, new Object[]{like, currentUserId}, (rs, rowNum) -> {
            UsersBean user = new UsersBean();
            user.setAuthUserId(rs.getLong("auth_user_id"));
            user.setFullName(rs.getString("full_name"));
            user.setProfileImage(rs.getBytes("profile_image"));
            return user;
        });
    }

    // ============================
    // Pagination Support
    // ============================
    public List<MessageBean> getConversationBefore(Long senderId, Long receiverId, int limit, Timestamp before) {
        String sql = """
            SELECT m.message_id, m.sender_id, m.receiver_id,
                   CONCAT_WS(' ', u.first_name, u.middle_name, u.last_name) AS sender_name,
                   u.profile_image, m.sent_at, m.message_content, m.deleted_at
            FROM messages m
            JOIN users u ON m.sender_id = u.auth_user_id
            WHERE ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
              AND m.sent_at < ?
            ORDER BY m.sent_at DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MessageBean msg = new MessageBean();
            msg.setMessageId(rs.getLong("message_id"));
            msg.setSenderId(rs.getLong("sender_id"));
            msg.setReceiverId(rs.getLong("receiver_id"));
            msg.setSenderName(rs.getString("sender_name"));
            msg.setProfileImage(rs.getString("profile_image"));
            msg.setMessageContent(rs.getString("message_content"));

            Timestamp sentAt = rs.getTimestamp("sent_at");
            if (sentAt != null) {
                msg.setSentAt(new SimpleDateFormat("yyyy-MM-dd hh:mm a").format(sentAt));
            }

            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            msg.setDeletedAt(deletedAt);

            return msg;
        }, senderId, receiverId, receiverId, senderId, before, limit);
    }

    // ============================
    // Status Updates & Deletion
    // ============================
    public int markMessageAsRead(Long senderId, Long receiverId) {
        String sql = "UPDATE messages SET is_read = true WHERE sender_id = ? AND receiver_id = ? AND is_read = false";
        return jdbcTemplate.update(sql, senderId, receiverId);
    }

    public int markAllMessagesAsRead(Long userId) {
        return jdbcTemplate.update("UPDATE messages SET is_read = true WHERE receiver_id = ? AND is_read = false", userId);
    }

    public int markMessagesAsSeen(Long senderId, Long receiverId) {
        String sql = "UPDATE messages SET seen_at = NOW() WHERE sender_id = ? AND receiver_id = ? AND seen_at IS NULL";
        return jdbcTemplate.update(sql, senderId, receiverId);
    }

    public int deleteMessage(Long messageId) {
        String sql = "UPDATE messages SET message_content = 'deleted message', deleted_at = NOW() WHERE message_id = ?";
        return jdbcTemplate.update(sql, messageId);
    }

    public int updateMessageContent(Long userId, Long messageId, String newContent) {
        String sql = "UPDATE messages SET message_content = ?, updated_at = now() WHERE message_id = ? AND sender_id = ?";
        return jdbcTemplate.update(sql, newContent, messageId, userId);
    }

    public int countUnreadMessages(Long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = false",
                Integer.class, userId);
    }

    public int countUnreadMessagesFrom(Long senderId, Long receiverId) {
        String sql = "SELECT COUNT(*) FROM messages WHERE sender_id = ? AND receiver_id = ? AND is_read = false";
        return jdbcTemplate.queryForObject(sql, Integer.class, senderId, receiverId);
    }

    public String getLatestSeenTimestamp(Long senderId, Long receiverId) {
        String sql = "SELECT MAX(seen_at) FROM messages "
                + "WHERE sender_id = ? AND receiver_id = ? AND seen_at IS NOT NULL";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, senderId, receiverId);
        } catch (Exception e) {
            return null;
        }
    }

    public String getLastMessageTimestamp(Long senderId, Long receiverId) {
        String sql = "SELECT MAX(sent_at) FROM messages WHERE sender_id = ? AND receiver_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, senderId, receiverId);
        } catch (Exception e) {
            return null;
        }
    }

}
