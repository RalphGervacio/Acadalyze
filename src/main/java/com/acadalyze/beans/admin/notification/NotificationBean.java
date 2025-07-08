package com.acadalyze.beans.admin.notification;

import java.sql.Timestamp;
import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class NotificationBean {

    private int notificationId;
    private int userId;
    private String type;
    private String title;
    private String message;
    private String url;
    private Timestamp createdAt;
    private boolean readStatus;
    private String extraData; 

}
