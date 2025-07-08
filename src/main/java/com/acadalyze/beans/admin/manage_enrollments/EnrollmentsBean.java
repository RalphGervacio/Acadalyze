package com.acadalyze.beans.admin.manage_enrollments;

import java.sql.Timestamp;
import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class EnrollmentsBean {

    // Enrollment Info
    private Long enrollmentId;
    private Timestamp enrolledAt;
    private String enrolledAtFormatted;

    // User Info
    private Long authUserId;
    private String studentId;
    private String userName;
    private String email;
    private String fullName;
    private String profileImage;
    private String coverImage;
    private String bio;
    private Long createdBy;
    private Timestamp userCreatedAt;
    private Boolean userActive;
    private Boolean isVerified;

    // Course Info
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private String courseDescription;
    private Timestamp courseCreatedAt;
    private Timestamp courseUpdatedAt;

}
