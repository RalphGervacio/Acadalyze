package com.acadalyze.dao;

import com.acadalyze.beans.admin.manage_schedules.ScheduleBean;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ralph Gervacio
 */
@Repository
public class DashboardDAO {

    @Autowired
    JdbcTemplate jdbc;

    public List<ScheduleBean> getLatestPublishedSchedules() {
        String sql = "SELECT "
                + "sch.schedule_id, "
                + "s.subject_code, s.subject_name, "
                + "sch.day_of_week, "
                + "sch.start_time, sch.end_time, "
                + "sch.room, sch.section, "
                + "u.first_name || ' ' || u.last_name AS instructor_full_name, "
                + "sch.created_at "
                + "FROM subject_schedule sch "
                + "JOIN subjects s ON sch.subject_id = s.subject_id "
                + "LEFT JOIN users u ON sch.instructor_id = u.auth_user_id "
                + "WHERE sch.published = TRUE "
                + "ORDER BY sch.created_at DESC";

        return jdbc.query(sql, (rs, rowNum) -> {
            ScheduleBean bean = new ScheduleBean();

            bean.setScheduleId(rs.getLong("schedule_id"));
            bean.setSubjectCode(rs.getString("subject_code"));
            bean.setSubjectName(rs.getString("subject_name"));
            bean.setDayOfWeek(rs.getString("day_of_week"));

            LocalTime startTime = rs.getTime("start_time").toLocalTime();
            LocalTime endTime = rs.getTime("end_time").toLocalTime();

            bean.setStartTime(startTime);
            bean.setEndTime(endTime);
            bean.setStartTimeRaw(startTime.toString());
            bean.setEndTimeRaw(endTime.toString());
            bean.setStartTimeDisplay(formatTime(startTime));
            bean.setEndTimeDisplay(formatTime(endTime));

            bean.setRoom(rs.getString("room"));
            bean.setSection(rs.getString("section"));
            bean.setInstructorFullName(rs.getString("instructor_full_name"));

            Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
            String createdAtFormatted = createdAtTimestamp.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy - hh:mm a"));
            bean.setCreatedAt(createdAtFormatted);

            // === Add these for FullCalendar ===
            LocalDate targetDate = getNextDateForDay(bean.getDayOfWeek());
            LocalDateTime startDateTime = LocalDateTime.of(targetDate, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(targetDate, endTime);

            DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            bean.setStartDateTime(startDateTime.format(isoFormatter));
            bean.setEndDateTime(endDateTime.format(isoFormatter));

            return bean;
        });
    }

    private LocalDate getNextDateForDay(String dayOfWeek) {
        try {
            DayOfWeek targetDay = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            LocalDate today = LocalDate.now();
            int todayValue = today.getDayOfWeek().getValue();
            int targetValue = targetDay.getValue();

            int daysUntilTarget = (targetValue - todayValue + 7) % 7;
            return today.plusDays(daysUntilTarget);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
    }

    public boolean unpublishSchedules(List<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return false;
        }

        StringBuilder sql = new StringBuilder("UPDATE subject_schedule SET published = FALSE WHERE schedule_id IN (");
        for (int i = 0; i < scheduleIds.size(); i++) {
            sql.append("?");
            if (i < scheduleIds.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        int affectedRows = jdbc.update(sql.toString(), scheduleIds.toArray());
        return affectedRows > 0;
    }

}
