package com.acadalyze.dao.admin.manage_users;

import com.acadalyze.beans.admin.manage_users.RolesBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class UsersDAO {

    @Autowired
    JdbcTemplate jdbcTemplate;

    // ================================
    // 🔹 NO ROWMAPPER METHODS
    // ================================
    public boolean emailExistsForOtherUser(String email, Long excludedUserId) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND auth_user_id != ? AND is_active = true";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, excludedUserId);
        return count != null && count > 0;
    }

    public void updateBio(Long authUserId, String bio) {
        String sql = "UPDATE users SET bio = ? WHERE auth_user_id = ?";
        jdbcTemplate.update(sql, bio, authUserId);
    }

    public void updateProfileImage(Long userId, byte[] imageBytes) {
        String sql = "UPDATE users SET profile_image = ? WHERE auth_user_id = ?";
        jdbcTemplate.update(sql, imageBytes, userId);
    }

    public void updateCoverImage(Long userId, byte[] imageBytes) {
        String sql = "UPDATE users SET cover_image = ? WHERE auth_user_id = ?";
        jdbcTemplate.update(sql, imageBytes, userId);
    }

    public List<Map<String, Object>> searchStudents(String query) {
        String sql = "SELECT auth_user_id, student_id, first_name, last_name "
                + "FROM users "
                + "WHERE auth_role_id = 3 AND is_active = true "
                + "AND (first_name ILIKE ? OR last_name ILIKE ? OR student_id ILIKE ?)";
        return jdbcTemplate.queryForList(sql, "%" + query + "%", "%" + query + "%", "%" + query + "%");
    }

    public UsersBean findEmailForReactivation(String email) {
        String sql = "SELECT auth_user_id, first_name, email, is_active FROM users WHERE email = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{email}, (rs, rowNum) -> {
                UsersBean user = new UsersBean();
                user.setAuthUserId(rs.getLong("auth_user_id"));
                user.setFirstName(rs.getString("first_name"));
                user.setEmail(rs.getString("email"));
                user.setIsActive(rs.getBoolean("is_active"));
                return user;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // ========================
    // 🔹 FULL MAPPING METHODS
    // ========================
    private static class UsersRowMapper implements RowMapper<UsersBean> {

        @Override
        public UsersBean mapRow(ResultSet rs, int rowNum) throws SQLException {
            UsersBean user = new UsersBean();
            user.setAuthUserId(rs.getLong("auth_user_id"));
            user.setFirstName(rs.getString("first_name"));
            user.setMiddleName(rs.getString("middle_name"));
            user.setLastName(rs.getString("last_name"));
            user.setUserName(rs.getString("user_name"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setStudentId(rs.getString("student_id"));
            user.setIsVerified(rs.getBoolean("is_verified"));
            user.setProfileImage(rs.getBytes("profile_image"));
            user.setCoverImage(rs.getBytes("cover_image"));
            user.setIsActive(rs.getBoolean("is_active"));
            user.setBio(rs.getString("bio"));
            user.setCreatedBy(rs.getString("created_by"));
            user.setCreatedAt(rs.getTimestamp("created_at"));

            RolesBean role = new RolesBean();
            role.setAuthRoleId(rs.getLong("auth_role_id"));
            role.setRoleName(rs.getString("role_name"));
            role.setRoleDesc(rs.getString("role_desc"));

            user.setRole(role);
            return user;
        }
    }

    private static class UsersLightRowMapper implements RowMapper<UsersBean> {

        @Override
        public UsersBean mapRow(ResultSet rs, int rowNum) throws SQLException {
            UsersBean user = new UsersBean();
            user.setAuthUserId(rs.getLong("auth_user_id"));
            user.setFirstName(rs.getString("first_name"));
            user.setMiddleName(rs.getString("middle_name"));
            user.setLastName(rs.getString("last_name"));
            user.setUserName(rs.getString("user_name"));
            user.setEmail(rs.getString("email"));
            user.setStudentId(rs.getString("student_id"));
            user.setAuthRoleId(rs.getLong("auth_role_id"));
            user.setIsVerified(rs.getBoolean("is_verified"));
            user.setIsActive(rs.getBoolean("is_active"));

            // Role
            RolesBean role = new RolesBean();
            role.setRoleName(rs.getString("role_name"));
            user.setRole(role);

            return user;
        }
    }

    public Long save(UsersBean user) {
        String sql = "INSERT INTO users (email, password, first_name, middle_name, last_name, user_name, student_id, auth_role_id, is_verified, verification_token, created_by, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING auth_user_id";

        return jdbcTemplate.queryForObject(sql, new Object[]{
            user.getEmail(),
            user.getPassword(),
            user.getFirstName(),
            user.getMiddleName(),
            user.getLastName(),
            user.getUserName(),
            user.getStudentId(),
            user.getRole().getAuthRoleId(),
            user.getIsVerified(),
            user.getVerificationToken(),
            user.getCreatedBy(),
            user.getCreatedAt()
        }, Long.class);
    }

    public void update(UsersBean user) {
        String sql = "UPDATE users "
                + "SET first_name = ?, middle_name = ?, last_name = ?, user_name = ?, "
                + "email = ?, password = ?, student_id = ?, auth_role_id = ?, is_verified = ?, is_active = ? "
                + "WHERE auth_user_id = ?";

        jdbcTemplate.update(sql,
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getUserName(),
                user.getEmail(),
                user.getPassword(),
                user.getStudentId(),
                user.getRole().getAuthRoleId(),
                user.getIsVerified(),
                user.getIsActive(),
                user.getAuthUserId()
        );
    }

    public void softDeleteUserById(Long authUserId) {
        String sql = "UPDATE users SET is_active = false WHERE auth_user_id = ?";
        jdbcTemplate.update(sql, authUserId);
    }

    public void reactivateUserById(Long authUserId) {
        String sql = "UPDATE users SET is_active = true WHERE auth_user_id = ?";
        jdbcTemplate.update(sql, authUserId);
    }

    public void setIsVerified(Long authUserId) {
        String sql = "UPDATE users SET is_verified = ? WHERE auth_user_id = ?";
        jdbcTemplate.update(sql, true, authUserId);
    }

    public UsersBean findById(Long authUserId) {
        String sql = "SELECT u.auth_user_id, u.first_name, u.middle_name, u.last_name, "
                + "u.user_name, u.email, u.password, u.student_id, u.auth_role_id, "
                + "u.is_verified, u.profile_image, u.cover_image, u.is_active, u.bio, "
                + "u.created_by, u.created_at, "
                + "r.role_name, r.role_desc "
                + "FROM users u "
                + "JOIN auth_role r ON u.auth_role_id = r.auth_role_id "
                + "WHERE u.auth_user_id = ? AND u.is_active = true";

        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{authUserId}, new UsersRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public UsersBean findByEmail(String email) {
        String sql = "SELECT u.auth_user_id, u.first_name, u.middle_name, u.last_name, "
                + "u.user_name, u.email, u.password, u.student_id, u.auth_role_id, "
                + "u.is_verified, u.profile_image, u.cover_image, u.is_active, u.bio, "
                + "u.created_by, u.created_at, "
                + "r.role_name, r.role_desc "
                + "FROM users u "
                + "JOIN auth_role r ON u.auth_role_id = r.auth_role_id "
                + "WHERE u.email = ? AND u.is_active = true";

        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{email}, new UsersRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public UsersBean findByEmailIgnoreActive(String email) {
        String sql = "SELECT auth_user_id, first_name, email FROM users WHERE email = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{email}, (rs, rowNum) -> {
                UsersBean user = new UsersBean();
                user.setAuthUserId(rs.getLong("auth_user_id"));
                user.setFirstName(rs.getString("first_name"));
                user.setEmail(rs.getString("email"));
                return user;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public UsersBean findByUserName(String userName) {
        String sql = "SELECT u.auth_user_id, u.first_name, u.middle_name, u.last_name, "
                + "u.user_name, u.email, u.password, u.student_id, u.auth_role_id, "
                + "u.is_verified, u.profile_image, u.cover_image, u.is_active, u.bio, "
                + "u.created_by, u.created_at, "
                + "r.role_name, r.role_desc "
                + "FROM users u "
                + "JOIN auth_role r ON u.auth_role_id = r.auth_role_id "
                + "WHERE u.user_name = ? AND u.is_active = true";

        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{userName}, new UsersRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public UsersBean findByStudentId(String studentId) {
        String sql = "SELECT u.auth_user_id, u.first_name, u.middle_name, u.last_name, "
                + "u.user_name, u.email, u.password, u.student_id, u.auth_role_id, "
                + "u.is_verified, u.profile_image, u.cover_image, u.is_active, u.bio, "
                + "u.created_by, u.created_at, "
                + "r.role_name, r.role_desc "
                + "FROM users u "
                + "JOIN auth_role r ON u.auth_role_id = r.auth_role_id "
                + "WHERE u.student_id = ? AND u.is_active = true";

        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{studentId}, new UsersRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<UsersBean> findAllUsers() {
        String sql = "SELECT u.auth_user_id, u.first_name, u.middle_name, u.last_name, "
                + "u.user_name, u.email, u.student_id, u.auth_role_id, "
                + "u.is_verified, u.is_active, r.role_name "
                + "FROM users u "
                + "JOIN auth_role r ON u.auth_role_id = r.auth_role_id "
                + "ORDER BY u.auth_user_id DESC";

        return jdbcTemplate.query(sql, new UsersLightRowMapper());
    }

    public boolean updateUserProfile(UsersBean user) {
        String sql = "UPDATE users SET first_name = ?, middle_name = ?, last_name = ?, email = ? WHERE auth_user_id = ? AND is_active = true";
        int rows = jdbcTemplate.update(sql, user.getFirstName(), user.getMiddleName(), user.getLastName(), user.getEmail(), user.getAuthUserId());
        return rows > 0;
    }

    public List<Map<String, Object>> getLoginHistoryByUserId(Long authUserId) {
        String sql = "SELECT login_time AS timestamp, ip_address AS ip, user_agent AS device "
                + "FROM login_history "
                + "WHERE auth_user_id = ? "
                + "ORDER BY login_time DESC "
                + "LIMIT 20";
        return jdbcTemplate.queryForList(sql, authUserId);
    }

    public void saveLoginHistory(Long authUserId, String ipAddress, String userAgent) {
        String sql = "INSERT INTO login_history (auth_user_id, login_time, ip_address, user_agent) VALUES (?, NOW(), ?, ?)";
        jdbcTemplate.update(sql, authUserId, ipAddress, userAgent);
    }

    public boolean verifyUserPassword(Long authUserId, String plainPassword) {
        String sql = "SELECT password FROM users WHERE auth_user_id = ? AND is_active = true";

        try {
            String storedHashedPassword = jdbcTemplate.queryForObject(sql, String.class, authUserId);
            return storedHashedPassword != null && org.springframework.security.crypto.bcrypt.BCrypt.checkpw(plainPassword, storedHashedPassword);
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

}
