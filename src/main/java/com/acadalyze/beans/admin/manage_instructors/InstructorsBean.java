package com.acadalyze.beans.admin.manage_instructors;

import java.sql.Timestamp;
import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class InstructorsBean {

    private Long subjectInstructorId;
    private Long subjectId;
    private Long instructorId;
    private String fullName;
    private String email;
    private String subjectCode;
    private String subjectName;
    private Timestamp assignedAt;
    private String assignedAtFormatted;

}
