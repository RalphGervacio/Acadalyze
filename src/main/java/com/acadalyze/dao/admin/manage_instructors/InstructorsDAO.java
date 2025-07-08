package com.acadalyze.dao.admin.manage_instructors;

import com.acadalyze.beans.admin.manage_instructors.InstructorsBean;
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
public class InstructorsDAO {

    JdbcTemplate jdbcTemplate;

    public InstructorsDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<InstructorsBean> getAllAssignedInstructors() {
        String sql
                = "SELECT "
                + "    si.subject_instructor_id, "
                + "    si.subject_id, "
                + "    si.instructor_id, "
                + "    CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS full_name, "
                + "    u.email, "
                + "    s.subject_code, "
                + "    s.subject_name, "
                + "    si.assigned_at "
                + "FROM subject_instructors si "
                + "JOIN users u ON u.auth_user_id = si.instructor_id "
                + "JOIN subjects s ON s.subject_id = si.subject_id "
                + "ORDER BY u.last_name, u.first_name";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            InstructorsBean bean = new InstructorsBean();

            bean.setSubjectInstructorId(rs.getLong("subject_instructor_id"));
            bean.setSubjectId(rs.getLong("subject_id"));
            bean.setInstructorId(rs.getLong("instructor_id"));
            bean.setFullName(rs.getString("full_name"));
            bean.setEmail(rs.getString("email"));
            bean.setSubjectCode(rs.getString("subject_code"));
            bean.setSubjectName(rs.getString("subject_name"));

            Timestamp assignedAt = rs.getTimestamp("assigned_at");
            bean.setAssignedAt(assignedAt);

            if (assignedAt != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm:ss a");
                String formatted = assignedAt.toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
                bean.setAssignedAtFormatted(formatted);
            }

            return bean;
        });
    }

    public boolean assignInstructorToSubject(InstructorsBean bean) {
        String sql = "INSERT INTO subject_instructors (subject_id, instructor_id) VALUES (?, ?)";
        return jdbcTemplate.update(sql, bean.getSubjectId(), bean.getInstructorId()) > 0;
    }

    public boolean removeInstructorFromSubject(Long subjectId, Long instructorId) {
        String sql = "DELETE FROM subject_instructors WHERE subject_id = ? AND instructor_id = ?";
        return jdbcTemplate.update(sql, subjectId, instructorId) > 0;
    }

    public boolean isAlreadyAssigned(Long subjectId, Long instructorId) {
        String sql = "SELECT COUNT(*) FROM subject_instructors WHERE subject_id = ? AND instructor_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, subjectId, instructorId);
        return count != null && count > 0;
    }
}
