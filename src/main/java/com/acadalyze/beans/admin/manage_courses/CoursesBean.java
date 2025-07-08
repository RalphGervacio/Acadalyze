package com.acadalyze.beans.admin.manage_courses;

import java.sql.Timestamp;
import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class CoursesBean {

    private Long course_id;
    private String course_code;
    private String course_title;
    private String course_description;
    private Timestamp created_at;
    private Timestamp updated_at;

}
