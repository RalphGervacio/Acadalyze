package com.acadalyze.beans.students.current_enrolled;

import java.sql.Timestamp;
import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class CurrentEnrolledBean {

    private Long enrollmentId;
    private Timestamp enrolledAt;
    private String enrolledAtFormatted;
    private Long courseId;
    private String courseCode;
    private String courseTitle;

}
