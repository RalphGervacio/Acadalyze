package com.acadalyze.dao.admin.manage_grades;

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
public class GradesDAO {

    JdbcTemplate jdbcTemplate;

    public GradesDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Map<String, Object>> searchGrades(String query, Long studentId) {
        String sql = ""
                + "SELECT g.grade_id, g.grade, g.remarks, "
                + "s.subject_code, s.subject_name "
                + "FROM grades g "
                + "JOIN subjects s ON g.subject_id = s.subject_id "
                + "WHERE g.auth_user_id = ? "
                + "AND (CAST(g.grade AS TEXT) ILIKE ? "
                + "OR g.remarks ILIKE ? "
                + "OR s.subject_code ILIKE ? "
                + "OR s.subject_name ILIKE ?)";

        String like = "%" + query + "%";
        return jdbcTemplate.queryForList(sql, studentId, like, like, like, like);
    }

    public List<Map<String, Object>> searchGradesAsAdmin(String query) {
        String sql = ""
                + "SELECT g.grade_id, g.grade, g.remarks, "
                + "s.subject_code, s.subject_name, "
                + "u.first_name, u.last_name "
                + "FROM grades g "
                + "JOIN subjects s ON g.subject_id = s.subject_id "
                + "JOIN users u ON g.auth_user_id = u.auth_user_id "
                + "WHERE CAST(g.grade AS TEXT) ILIKE ? "
                + "OR g.remarks ILIKE ? "
                + "OR s.subject_code ILIKE ? "
                + "OR s.subject_name ILIKE ? "
                + "OR u.first_name ILIKE ? "
                + "OR u.last_name ILIKE ?";

        String like = "%" + query + "%";
        return jdbcTemplate.queryForList(sql, like, like, like, like, like, like);
    }

}
