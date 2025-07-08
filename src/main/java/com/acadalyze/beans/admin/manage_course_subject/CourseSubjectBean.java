package com.acadalyze.beans.admin.manage_course_subject;

import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class CourseSubjectBean {

    private Long course_subject_id;
    private Long course_id;
    private Long subject_id;
    private Integer semester;
    private Integer year_level;

}
