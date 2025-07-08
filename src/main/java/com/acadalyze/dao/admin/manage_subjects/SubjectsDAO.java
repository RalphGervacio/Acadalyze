package com.acadalyze.dao.admin.manage_subjects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.acadalyze.beans.admin.manage_subjects.SubjectsBean;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class SubjectsDAO {

    JdbcTemplate jdbcTemplate;

    public SubjectsDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Map<String, Object>> searchSubjects(String query) {
        String sql
                = "SELECT subject_id, subject_code, subject_name, description "
                + "FROM subjects "
                + "WHERE subject_code ILIKE ? "
                + "OR subject_name ILIKE ? "
                + "OR description ILIKE ?";

        String like = "%" + query + "%";
        return jdbcTemplate.queryForList(sql, like, like, like);
    }

    public List<SubjectsBean> getAllSubjects() {
        String sql = "SELECT * FROM subjects ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapSubject(rs));
    }

    public SubjectsBean getSubjectById(Long id) {
        String sql = "SELECT * FROM subjects WHERE subject_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{id}, (rs, rowNum) -> mapSubject(rs));
    }

    public void addSubject(SubjectsBean subject) {
        String sql = "INSERT INTO subjects (subject_code, subject_name, description, created_at) VALUES (?, ?, ?, NOW())";
        jdbcTemplate.update(sql, subject.getSubjectCode(), subject.getSubjectName(), subject.getDescription());
    }

    public void updateSubject(SubjectsBean subject) {
        String sql = "UPDATE subjects SET subject_code = ?, subject_name = ?, description = ? WHERE subject_id = ?";
        jdbcTemplate.update(sql, subject.getSubjectCode(), subject.getSubjectName(), subject.getDescription(), subject.getSubjectId());
    }

    public void deleteSubject(Long subjectId) {
        String sql = "DELETE FROM subjects WHERE subject_id = ?";
        jdbcTemplate.update(sql, subjectId);
    }

    public boolean isDuplicateSubject(String code, String name, String description) {
        String sql = "SELECT COUNT(*) FROM subjects WHERE subject_code = ? OR subject_name = ? OR description = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code, name, description);
        return count != null && count > 0;
    }

    public boolean isDuplicateSubjectExcludingId(Long id, String code, String name, String description) {
        String sql
                = "SELECT COUNT(*) FROM subjects "
                + "WHERE subject_id != ? "
                + "AND (LOWER(subject_code) = LOWER(?) "
                + "OR LOWER(subject_name) = LOWER(?) "
                + "OR LOWER(description) = LOWER(?))";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id, code, name, description);
        return count != null && count > 0;
    }

    private SubjectsBean mapSubject(ResultSet rs) throws SQLException {
        SubjectsBean bean = new SubjectsBean();
        bean.setSubjectId(rs.getLong("subject_id"));
        bean.setSubjectCode(rs.getString("subject_code"));
        bean.setSubjectName(rs.getString("subject_name"));
        bean.setDescription(rs.getString("description"));
        bean.setCreatedAt(rs.getTimestamp("created_at"));
        return bean;
    }
}
