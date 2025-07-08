package com.acadalyze.beans.admin.manage_schedules;

import lombok.Data;
import java.time.LocalTime;

/**
 * @author Ralph Gervacio
 */
@Data
public class ScheduleBean {

    private Long scheduleId;
    private Long courseId;
    private Long subjectId;
    private Long instructorId;

    private String dayOfWeek;
    private transient LocalTime startTime;
    private transient LocalTime endTime;
    private String startTimeRaw;
    private String endTimeRaw;

    private String startTimeDisplay;
    private String endTimeDisplay;

    private String room;
    private String section;
    private String createdAt;
    private String courseDisplay;

    // Additional fields for display
    private String courseCode;
    private String courseTitle;
    private String subjectCode;
    private String subjectName;
    private String instructorFullName;
    private String startDateTime; 
    private String endDateTime;   

}
