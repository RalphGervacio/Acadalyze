package com.acadalyze.beans.messages;

import java.sql.Timestamp;
import lombok.Data;

/**
 * @author Ralph Gervacio
 */
@Data
public class MessageBean {

    private Long messageId;
    private Long senderId;
    private Long receiverId;

    // Sender's display info (from JOIN)
    private String senderName;
    private String profileImage;

    // Content & Attachment
    private String messageContent;
    private byte[] attachment;
    private String attachmentName;
    private String attachmentType;

    // Timestamps
    private String sentAt; // Formatted string (DAO handles formatting)
    private Timestamp deletedAt;
    private Timestamp seenAt;
}
