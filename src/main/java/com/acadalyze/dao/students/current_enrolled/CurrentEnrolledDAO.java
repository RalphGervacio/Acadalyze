package com.acadalyze.dao.students.current_enrolled;

import com.acadalyze.beans.students.current_enrolled.CurrentEnrolledBean;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class CurrentEnrolledDAO {

    JdbcTemplate jdbcTemplate;

    public CurrentEnrolledDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<CurrentEnrolledBean> getEnrolledByStudentId(Long authUserId) {
        String sql = "SELECT "
                + "e.enrollment_id, e.enrolled_at, "
                + "c.course_id, c.course_code, c.course_title "
                + "FROM enrollments e "
                + "JOIN courses c ON e.course_id = c.course_id "
                + "WHERE e.auth_user_id = ? "
                + "ORDER BY e.enrolled_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CurrentEnrolledBean bean = new CurrentEnrolledBean();

            bean.setEnrollmentId(rs.getLong("enrollment_id"));

            Timestamp enrolledAt = rs.getTimestamp("enrolled_at");
            bean.setEnrolledAt(enrolledAt);

            if (enrolledAt != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm:ss a");
                String formatted = enrolledAt.toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
                bean.setEnrolledAtFormatted(formatted);
            }

            // Course info
            bean.setCourseId(rs.getLong("course_id"));
            bean.setCourseCode(rs.getString("course_code"));
            bean.setCourseTitle(rs.getString("course_title"));

            return bean;
        }, authUserId);
    }

}
