package com.acadalyze.dao.admin.manage_enrollments;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.acadalyze.beans.admin.manage_enrollments.EnrollmentsBean;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class EnrollmentsDAO {

    JdbcTemplate jdbcTemplate;

    public EnrollmentsDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<EnrollmentsBean> getAllEnrollments() {
        String sql
                = "SELECT "
                + "e.enrollment_id AS enrollment_id, "
                + "e.enrolled_at, "
                + "u.auth_user_id, "
                + "u.student_id, "
                + "u.user_name, "
                + "u.email, "
                + "CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS full_name, "
                + "u.profile_image, "
                + "u.cover_image, "
                + "u.bio, "
                + "u.created_by, "
                + "u.created_at AS user_created_at, "
                + "u.is_active AS user_active, "
                + "u.is_verified, "
                + "c.course_id, "
                + "c.course_code, "
                + "c.course_title, "
                + "c.course_description, "
                + "c.created_at AS course_created_at, "
                + "c.updated_at AS course_updated_at "
                + "FROM enrollments e "
                + "JOIN users u ON e.auth_user_id = u.auth_user_id "
                + "JOIN courses c ON e.course_id = c.course_id "
                + "ORDER BY e.enrolled_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            EnrollmentsBean bean = new EnrollmentsBean();

            // Format enrolledAt
            Timestamp enrolledAt = rs.getTimestamp("enrolled_at");
            bean.setEnrolledAt(enrolledAt);

            if (enrolledAt != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm:ss a");
                String formatted = enrolledAt.toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
                bean.setEnrolledAtFormatted(formatted);
            }

            // User info
            bean.setAuthUserId(rs.getLong("auth_user_id"));
            bean.setStudentId(rs.getString("student_id"));
            bean.setUserName(rs.getString("user_name"));
            bean.setEmail(rs.getString("email"));
            bean.setFullName(rs.getString("full_name"));
            bean.setProfileImage(rs.getString("profile_image"));
            bean.setCoverImage(rs.getString("cover_image"));
            bean.setBio(rs.getString("bio"));
            bean.setCreatedBy(rs.getLong("created_by"));
            bean.setUserCreatedAt(rs.getTimestamp("user_created_at"));
            bean.setUserActive(rs.getBoolean("user_active"));
            bean.setIsVerified(rs.getBoolean("is_verified"));

            // Course info
            bean.setCourseId(rs.getLong("course_id"));
            bean.setCourseCode(rs.getString("course_code"));
            bean.setCourseTitle(rs.getString("course_title"));
            bean.setCourseDescription(rs.getString("course_description"));
            bean.setCourseCreatedAt(rs.getTimestamp("course_created_at"));
            bean.setCourseUpdatedAt(rs.getTimestamp("course_updated_at"));

            // Enrollment info
            bean.setEnrollmentId(rs.getLong("enrollment_id"));

            return bean;
        });
    }

    public List<EnrollmentsBean> getEnrollmentsByStudentId(String studentId) {
        String sql
                = "SELECT "
                + "    e.enrollment_id, "
                + "    e.enrolled_at, "
                + "    u.student_id, "
                + "    CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS full_name, "
                + "    u.email, "
                + "    c.course_code, "
                + "    c.course_title "
                + "FROM enrollments e "
                + "JOIN users u ON e.auth_user_id = u.auth_user_id "
                + "JOIN courses c ON e.course_id = c.course_id "
                + "WHERE u.student_id = ? "
                + "ORDER BY e.enrolled_at DESC";

        return jdbcTemplate.query(sql, new Object[]{studentId}, (rs, rowNum) -> {
            EnrollmentsBean bean = new EnrollmentsBean();

            bean.setEnrollmentId(rs.getLong("enrollment_id"));

            Timestamp enrolledAt = rs.getTimestamp("enrolled_at");
            bean.setEnrolledAt(enrolledAt);

            if (enrolledAt != null) {
                String formatted = enrolledAt.toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm:ss a"));
                bean.setEnrolledAtFormatted(formatted);
            }

            bean.setStudentId(rs.getString("student_id"));
            bean.setFullName(rs.getString("full_name"));
            bean.setEmail(rs.getString("email"));
            bean.setCourseCode(rs.getString("course_code"));
            bean.setCourseTitle(rs.getString("course_title"));

            return bean;
        });
    }

    public EnrollmentsBean getEnrollmentById(Long enrollmentId) {
        String sql = "SELECT e.enrollment_id, e.auth_user_id, e.course_id, c.course_code, c.course_title, c.course_description "
                + "FROM enrollments e "
                + "JOIN courses c ON e.course_id = c.course_id "
                + "WHERE e.enrollment_id = ?";

        return jdbcTemplate.queryForObject(sql, new Object[]{enrollmentId}, (rs, rowNum) -> {
            EnrollmentsBean bean = new EnrollmentsBean();
            bean.setEnrollmentId(rs.getLong("enrollment_id"));
            bean.setAuthUserId(rs.getLong("auth_user_id"));
            bean.setCourseId(rs.getLong("course_id"));
            bean.setCourseCode(rs.getString("course_code"));
            bean.setCourseTitle(rs.getString("course_title"));
            bean.setCourseDescription(rs.getString("course_description"));
            return bean;
        });
    }

    public boolean isStudentEnrolled(Long authUserId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE auth_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, authUserId);
        return count != null && count > 0;
    }

    public int enrollStudent(Long authUserId, Long courseId) {
        String sql
                = "INSERT INTO enrollments (auth_user_id, course_id) "
                + "VALUES (?, ?)";
        return jdbcTemplate.update(sql, authUserId, courseId);
    }

    public int deleteEnrollmentById(Long enrollmentId) {
        String sql = "DELETE FROM enrollments WHERE enrollment_id = ?";
        return jdbcTemplate.update(sql, enrollmentId);
    }
}
