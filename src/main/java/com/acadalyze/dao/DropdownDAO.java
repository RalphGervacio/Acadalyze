package com.acadalyze.dao;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class DropdownDAO {

    JdbcTemplate jdbcTemplate;

    public DropdownDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // USED BY ManageEnrollmentsController and ManageCourseSubjectController
    public List<Map<String, Object>> getCourses() {
        String sql
                = "SELECT "
                + "course_id AS \"courseId\", "
                + "course_code AS \"courseCode\", "
                + "course_title AS \"courseTitle\" "
                + "FROM courses "
                + "ORDER BY course_code";

        return jdbcTemplate.queryForList(sql);
    }

    // USED BY ManageEnrollmentsController
    public List<Map<String, Object>> getStudents() {
        String sql = "SELECT "
                + "auth_user_id AS \"authUserId\", "
                + "student_id AS \"studentId\", "
                + "CONCAT(first_name, ' ', COALESCE(middle_name || ' ', ''), last_name) AS \"fullName\" "
                + "FROM users u "
                + "WHERE auth_role_id = 3 "
                + "AND is_active = true "
                + "AND student_id IS NOT NULL "
                + "AND NOT EXISTS ("
                + "    SELECT 1 FROM enrollments e WHERE e.auth_user_id = u.auth_user_id"
                + ") "
                + "ORDER BY student_id";

        return jdbcTemplate.queryForList(sql);
    }

    // USED BY ManageEnrollmentsController - Filter dropdown only students who are enrolled
    public List<Map<String, Object>> getStudentsWithEnrollments() {
        String sql
                = "SELECT DISTINCT "
                + "u.auth_user_id AS \"authUserId\", "
                + "u.student_id AS \"studentId\", "
                + "CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS \"fullName\" "
                + "FROM users u "
                + "JOIN enrollments e ON u.auth_user_id = e.auth_user_id "
                + "WHERE u.auth_role_id = 3 "
                + "AND u.is_active = true "
                + "AND u.student_id IS NOT NULL "
                + "ORDER BY u.student_id";

        return jdbcTemplate.queryForList(sql);
    }

    // USED BY ManageInstructorsController
    public List<Map<String, Object>> getInstructors() {
        String sql = "SELECT "
                + "auth_user_id AS \"authUserId\", "
                + "CONCAT(first_name, ' ', COALESCE(middle_name || ' ', ''), last_name) AS \"fullName\" "
                + "FROM users "
                + "WHERE auth_role_id = 4 "
                + "AND is_active = true "
                + "ORDER BY last_name, first_name";

        return jdbcTemplate.queryForList(sql);
    }

    // USED BY ManageInstructorsController - Load subjects for dropdown
    public List<Map<String, Object>> getSubjects() {
        String sql = "SELECT "
                + "subject_id AS \"subjectId\", "
                + "subject_code AS \"subjectCode\", "
                + "subject_name AS \"subjectName\" "
                + "FROM subjects "
                + "ORDER BY subject_code";

        return jdbcTemplate.queryForList(sql);
    }

    // USED BY ManageSchedulesController - To load dropdowns of subjects based on selected course
    public List<Map<String, Object>> getSubjectsByCourseId(Long courseId) {
        String sql = "SELECT s.subject_id AS \"subjectId\", "
                + "s.subject_code AS \"subjectCode\", "
                + "s.subject_name AS \"subjectName\" "
                + "FROM course_subjects cs "
                + "JOIN subjects s ON cs.subject_id = s.subject_id "
                + "WHERE cs.course_id = ? "
                + "ORDER BY s.subject_code";

        return jdbcTemplate.queryForList(sql, courseId);
    }

}
