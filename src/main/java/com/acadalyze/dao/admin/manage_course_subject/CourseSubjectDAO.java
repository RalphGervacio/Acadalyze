package com.acadalyze.dao.admin.manage_course_subject;

import com.acadalyze.beans.admin.manage_course_subject.CourseSubjectBean;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class CourseSubjectDAO {

    JdbcTemplate jdbc;

    public CourseSubjectDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Get all subjects assigned to a specific course
    public List<Map<String, Object>> getAllCourseSubjectBindings() {
        String sql = "SELECT cs.course_subject_id, cs.course_id, cs.subject_id, cs.semester, cs.year_level, "
                + "c.course_code, c.course_title, "
                + "s.subject_code, s.subject_name "
                + "FROM course_subjects cs "
                + "JOIN courses c ON cs.course_id = c.course_id "
                + "JOIN subjects s ON cs.subject_id = s.subject_id "
                + "ORDER BY c.course_code, s.subject_code";
        return jdbc.queryForList(sql);
    }

    // Just find by id
    public Map<String, Object> findById(Long id) {
        String sql = "SELECT * FROM course_subjects WHERE course_subject_id = ?";
        return jdbc.queryForMap(sql, id);
    }

    // Check if subject is already assigned to course
    public boolean exists(Long courseId, Long subjectId, Integer semester, Integer yearLevel) {
        String sql = "SELECT COUNT(*) FROM course_subjects WHERE course_id = ? AND subject_id = ? AND semester = ? AND year_level = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, courseId, subjectId, semester, yearLevel);
        return count != null && count > 0;
    }

    // Add new course-subject mapping
    public int insert(CourseSubjectBean bean) {
        String sql = "INSERT INTO course_subjects (course_id, subject_id, semester, year_level) VALUES (?, ?, ?, ?)";
        return jdbc.update(sql, bean.getCourse_id(), bean.getSubject_id(), bean.getSemester(), bean.getYear_level());
    }

    // Delete by course_subject_id
    public int delete(Long courseSubjectId) {
        String sql = "DELETE FROM course_subjects WHERE course_subject_id = ?";
        return jdbc.update(sql, courseSubjectId);
    }

    // Update semester and year level only
    public int update(CourseSubjectBean bean) {
        String sql = "UPDATE course_subjects SET course_id = ?, subject_id = ?, semester = ?, year_level = ? WHERE course_subject_id = ?";
        return jdbc.update(sql, bean.getCourse_id(), bean.getSubject_id(), bean.getSemester(), bean.getYear_level(), bean.getCourse_subject_id());
    }

    // Mapping helper
    private CourseSubjectBean mapResultSet(ResultSet rs) throws SQLException {
        CourseSubjectBean bean = new CourseSubjectBean();
        bean.setCourse_subject_id(rs.getLong("course_subject_id"));
        bean.setCourse_id(rs.getLong("course_id"));
        bean.setSubject_id(rs.getLong("subject_id"));
        bean.setSemester(rs.getInt("semester"));
        bean.setYear_level(rs.getInt("year_level"));
        return bean;
    }
}
