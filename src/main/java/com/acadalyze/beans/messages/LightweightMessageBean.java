package com.acadalyze.beans.messages;

import java.sql.Timestamp;
import lombok.Data;

/**
 *
 * @author Ralph Gervacio
 */
@Data
public class LightweightMessageBean {

    private Long messageId;
    private Long senderId;
    private String senderName;
    private String messageContent;
    private String attachmentType;
    private Timestamp sentAt;

}
