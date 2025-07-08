package com.acadalyze.dao.admin.manage_shedules;

import com.acadalyze.beans.admin.manage_schedules.ScheduleBean;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class SchedulesDAO {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public SchedulesDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<ScheduleBean> getSchedulesFilterByCourse(Long courseId) {
        String sql = "SELECT ss.schedule_id AS scheduleId, "
                + "c.course_id AS courseId, "
                + "c.course_code AS courseCode, "
                + "c.course_title AS courseTitle, "
                + "CONCAT(c.course_code, ' - ', c.course_title) AS courseDisplay, "
                + "s.subject_id AS subjectId, "
                + "s.subject_code AS subjectCode, "
                + "s.subject_name AS subjectName, "
                + "ss.day_of_week AS dayOfWeek, "
                + "ss.start_time AS startTime, "
                + "ss.end_time AS endTime, "
                + "TO_CHAR(ss.start_time, 'HH12:MI AM') AS startTimeDisplay, "
                + "TO_CHAR(ss.end_time, 'HH12:MI AM') AS endTimeDisplay, "
                + "ss.room AS room, "
                + "ss.section AS section, "
                + "u.auth_user_id AS instructorId, "
                + "CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS instructorFullName "
                + "FROM subject_schedule ss "
                + "JOIN courses c ON ss.course_id = c.course_id "
                + "JOIN subjects s ON ss.subject_id = s.subject_id "
                + "LEFT JOIN users u ON ss.instructor_id = u.auth_user_id "
                + "WHERE ss.course_id = ? "
                + "ORDER BY ss.day_of_week, ss.start_time";

        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ScheduleBean.class), courseId);
    }

    public List<ScheduleBean> getAllSchedules() {
        String sql = "SELECT "
                + "ss.schedule_id, ss.course_id, ss.subject_id, ss.instructor_id, "
                + "ss.day_of_week, ss.start_time, ss.end_time, ss.room, ss.section, ss.created_at, "
                + "c.course_code, c.course_title, CONCAT(c.course_code, ' - ', c.course_title) AS course_display, "
                + "s.subject_code, s.subject_name, "
                + "CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS instructor_full_name "
                + "FROM subject_schedule ss "
                + "JOIN courses c ON ss.course_id = c.course_id "
                + "JOIN subjects s ON ss.subject_id = s.subject_id "
                + "LEFT JOIN users u ON ss.instructor_id = u.auth_user_id "
                + "ORDER BY ss.day_of_week, ss.start_time";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ScheduleBean bean = new ScheduleBean();

            bean.setScheduleId(rs.getLong("schedule_id"));
            bean.setCourseId(rs.getLong("course_id"));
            bean.setSubjectId(rs.getLong("subject_id"));
            bean.setInstructorId(rs.getLong("instructor_id"));
            bean.setDayOfWeek(rs.getString("day_of_week"));
            bean.setRoom(rs.getString("room"));
            bean.setSection(rs.getString("section"));
            bean.setCourseCode(rs.getString("course_code"));
            bean.setCourseTitle(rs.getString("course_title"));
            bean.setCourseDisplay(rs.getString("course_display"));
            bean.setSubjectCode(rs.getString("subject_code"));
            bean.setSubjectName(rs.getString("subject_name"));
            bean.setInstructorFullName(rs.getString("instructor_full_name"));

            Time startTime = rs.getTime("start_time");
            Time endTime = rs.getTime("end_time");
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");

            if (startTime != null) {
                LocalTime st = startTime.toLocalTime();
                bean.setStartTime(st);
                bean.setStartTimeDisplay(st.format(timeFormat));
            }
            if (endTime != null) {
                LocalTime et = endTime.toLocalTime();
                bean.setEndTime(et);
                bean.setEndTimeDisplay(et.format(timeFormat));
            }

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm:ss a");
                String formatted = createdAt.toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
                bean.setCreatedAt(formatted);
            }

            return bean;
        });
    }

    public ScheduleBean findById(Long scheduleId) {
        String sql = "SELECT "
                + "ss.schedule_id, ss.course_id, ss.subject_id, ss.instructor_id, "
                + "ss.day_of_week, ss.start_time, ss.end_time, ss.room, ss.section, ss.created_at, "
                + "c.course_code, c.course_title, CONCAT(c.course_code, ' - ', c.course_title) AS course_display, "
                + "s.subject_code, s.subject_name, "
                + "CONCAT(u.first_name, ' ', COALESCE(u.middle_name || ' ', ''), u.last_name) AS instructor_full_name "
                + "FROM subject_schedule ss "
                + "JOIN courses c ON ss.course_id = c.course_id "
                + "JOIN subjects s ON ss.subject_id = s.subject_id "
                + "LEFT JOIN users u ON ss.instructor_id = u.auth_user_id "
                + "WHERE ss.schedule_id = ?";

        return jdbcTemplate.queryForObject(sql, new Object[]{scheduleId}, (rs, rowNum) -> {
            ScheduleBean bean = new ScheduleBean();

            bean.setScheduleId(rs.getLong("schedule_id"));
            bean.setCourseId(rs.getLong("course_id"));
            bean.setSubjectId(rs.getLong("subject_id"));
            bean.setInstructorId(rs.getLong("instructor_id"));
            bean.setDayOfWeek(rs.getString("day_of_week"));
            bean.setRoom(rs.getString("room"));
            bean.setSection(rs.getString("section"));
            bean.setCourseCode(rs.getString("course_code"));
            bean.setCourseTitle(rs.getString("course_title"));
            bean.setCourseDisplay(rs.getString("course_display"));
            bean.setSubjectCode(rs.getString("subject_code"));
            bean.setSubjectName(rs.getString("subject_name"));
            bean.setInstructorFullName(rs.getString("instructor_full_name"));

            Time startTime = rs.getTime("start_time");
            Time endTime = rs.getTime("end_time");
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");

            if (startTime != null) {
                LocalTime st = startTime.toLocalTime();
                bean.setStartTime(st);
                bean.setStartTimeDisplay(st.format(timeFormat));
                bean.setStartTimeRaw(st.toString());
            }
            if (endTime != null) {
                LocalTime et = endTime.toLocalTime();
                bean.setEndTime(et);
                bean.setEndTimeDisplay(et.format(timeFormat));
                bean.setEndTimeRaw(et.toString());
            }

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm:ss a");
                String formatted = createdAt.toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
                bean.setCreatedAt(formatted);
            }

            return bean;
        });
    }

    public int insertSchedule(ScheduleBean s) {
        String sql = "INSERT INTO subject_schedule (course_id, subject_id, instructor_id, day_of_week, "
                + "start_time, end_time, room, section) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        return jdbcTemplate.update(sql,
                s.getCourseId(),
                s.getSubjectId(),
                s.getInstructorId(),
                s.getDayOfWeek(),
                Time.valueOf(s.getStartTime()),
                Time.valueOf(s.getEndTime()),
                s.getRoom(),
                s.getSection());
    }

    public int publishAllUnpublishedSchedules() {
        String sql = "UPDATE subject_schedule SET published = TRUE WHERE published = FALSE";
        return jdbcTemplate.update(sql);
    }

    public List<ScheduleBean> getUnpublishedSchedules() {
        String sql = "SELECT "
                + "s.schedule_id, s.course_id, s.subject_id, s.instructor_id, "
                + "s.day_of_week, s.start_time, s.end_time, s.room, s.section, s.created_at, "
                + "c.course_code, c.course_title, "
                + "c.course_code || ' - ' || c.course_title AS course_display, "
                + "sub.subject_code, sub.subject_name, "
                + "u.first_name || ' ' || u.last_name AS instructor_full_name "
                + "FROM subject_schedule s "
                + "JOIN courses c ON s.course_id = c.course_id "
                + "JOIN subjects sub ON s.subject_id = sub.subject_id "
                + "LEFT JOIN users u ON s.instructor_id = u.auth_user_id "
                + "WHERE s.published = false";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ScheduleBean bean = new ScheduleBean();

            bean.setScheduleId(rs.getLong("schedule_id"));
            bean.setCourseId(rs.getLong("course_id"));
            bean.setSubjectId(rs.getLong("subject_id"));
            bean.setInstructorId(rs.getLong("instructor_id"));
            bean.setDayOfWeek(rs.getString("day_of_week"));
            bean.setRoom(rs.getString("room"));
            bean.setSection(rs.getString("section"));
            bean.setCourseCode(rs.getString("course_code"));
            bean.setCourseTitle(rs.getString("course_title"));
            bean.setCourseDisplay(rs.getString("course_display"));
            bean.setSubjectCode(rs.getString("subject_code"));
            bean.setSubjectName(rs.getString("subject_name"));
            bean.setInstructorFullName(rs.getString("instructor_full_name"));

            Time startTime = rs.getTime("start_time");
            Time endTime = rs.getTime("end_time");
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");

            if (startTime != null) {
                LocalTime st = startTime.toLocalTime();
                bean.setStartTime(st);
                bean.setStartTimeDisplay(st.format(timeFormat));
            }
            if (endTime != null) {
                LocalTime et = endTime.toLocalTime();
                bean.setEndTime(et);
                bean.setEndTimeDisplay(et.format(timeFormat));
            }

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm:ss a");
                String formatted = createdAt.toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
                bean.setCreatedAt(formatted);
            }

            return bean;
        });
    }

    public int updateScheduleTime(Long scheduleId, String dayOfWeek, String startTimeStr, String endTimeStr, String room, String section) {
        String sql = "UPDATE subject_schedule "
                + "SET day_of_week = ?, start_time = ?, end_time = ?, room = ?, section = ? "
                + "WHERE schedule_id = ?";

        // Convert to java.time.LocalTime
        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime = LocalTime.parse(endTimeStr);

        return jdbcTemplate.update(sql, dayOfWeek, startTime, endTime, room, section, scheduleId);
    }

    public int deleteSchedule(Long scheduleId) {
        String sql = "DELETE FROM subject_schedule WHERE schedule_id = ?";
        return jdbcTemplate.update(sql, scheduleId);
    }

}
