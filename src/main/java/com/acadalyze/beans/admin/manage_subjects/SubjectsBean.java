package com.acadalyze.beans.admin.manage_subjects;

import java.sql.Timestamp;
import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class SubjectsBean {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private String description;
    private Timestamp createdAt;

}
