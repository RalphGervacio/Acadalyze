package com.acadalyze.dao.admin.manage_courses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.acadalyze.beans.admin.manage_courses.CoursesBean;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class CoursesDAO {

    JdbcTemplate jdbcTemplate;

    public CoursesDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<CoursesBean> getAllCourses() {
        String sql = "SELECT * FROM courses ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapCourse(rs));
    }

    public CoursesBean getCourseById(Long id) {
        String sql = "SELECT * FROM courses WHERE course_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{id}, (rs, rowNum) -> mapCourse(rs));
    }

    public void addCourse(CoursesBean course) {
        String sql = "INSERT INTO courses (course_code, course_title, course_description, created_at, updated_at) "
                + "VALUES (?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, course.getCourse_code(), course.getCourse_title(), course.getCourse_description());
    }

    public void updateCourse(CoursesBean course) {
        String sql = "UPDATE courses SET course_code = ?, course_title = ?, course_description = ?, updated_at = NOW() "
                + "WHERE course_id = ?";
        jdbcTemplate.update(sql, course.getCourse_code(), course.getCourse_title(), course.getCourse_description(), course.getCourse_id());
    }

    public void deleteCourse(Long course_id) {
        String sql = "DELETE FROM courses WHERE course_id = ?";
        jdbcTemplate.update(sql, course_id);
    }

    public boolean isDuplicateCourse(String code, String title, String description) {
        String sql = "SELECT COUNT(*) FROM courses WHERE course_code = ? OR course_title = ? OR course_description = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code, title, description);
        return count != null && count > 0;
    }

    public boolean isDuplicateCourseExcludingId(Long id, String code, String title, String description) {
        String sql = "SELECT COUNT(*) FROM courses "
                + "WHERE course_id != ? "
                + "AND (LOWER(course_code) = LOWER(?) "
                + "OR LOWER(course_title) = LOWER(?) "
                + "OR LOWER(course_description) = LOWER(?))";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id, code, title, description);
        return count != null && count > 0;
    }

    private CoursesBean mapCourse(ResultSet rs) throws SQLException {
        CoursesBean bean = new CoursesBean();
        bean.setCourse_id(rs.getLong("course_id"));
        bean.setCourse_code(rs.getString("course_code"));
        bean.setCourse_title(rs.getString("course_title"));
        bean.setCourse_description(rs.getString("course_description"));
        bean.setCreated_at(rs.getTimestamp("created_at"));
        bean.setUpdated_at(rs.getTimestamp("updated_at"));
        return bean;
    }
}
