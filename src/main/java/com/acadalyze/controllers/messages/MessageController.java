package com.acadalyze.controllers.messages;

import com.acadalyze.WebSocket.ChatWebSocketHandler;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.beans.messages.LightweightMessageBean;
import com.acadalyze.beans.messages.MessageBean;
import com.acadalyze.dao.messages.MessagesDAO;
import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

@Controller
@RequestMapping(value = "/messages")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessagesDAO messagesDAO;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private Gson gson;

    // ============================
    // Page Rendering
    // ============================
    @GetMapping
    public ModelAndView showMessagesPage(HttpSession session,
            @RequestParam(name = "receiver", required = false) Long receiverId) {
        logger.info("GET /messages - Rendering messages page");

        UsersBean currentUser = (UsersBean) session.getAttribute("user");
        if (currentUser == null) {
            logger.warn("User session missing. Redirecting to /login");
            return new ModelAndView("redirect:/login");
        }

        Long currentUserId = currentUser.getAuthUserId();
        List<UsersBean> chatHeads = messagesDAO.getChatHeadsForUser(currentUserId);

        if (receiverId == null && !chatHeads.isEmpty()) {
            receiverId = chatHeads.get(0).getAuthUserId();
        }

        ModelAndView mav = new ModelAndView("pages/messages");
        mav.addObject("chatHeads", chatHeads);
        mav.addObject("pageName", "messages");

        if (receiverId != null) {
            mav.addObject("conversation", messagesDAO.getConversation(currentUserId, receiverId));
            mav.addObject("selectedUser", messagesDAO.getUserById(receiverId));
            mav.addObject("selectedUserId", receiverId);
        }

        return mav;
    }

    // ============================
    // Message Retrieval
    // ============================
    @GetMapping(value = "/received")
    public void getReceivedMessages(HttpServletResponse response,
            @SessionAttribute("user") UsersBean user) throws IOException {
        logger.info("GET /messages/received - Retrieving latest messages");

        try {
            List<MessageBean> messages = messagesDAO.getMessagesForUser(user.getAuthUserId());
            respondJson(response, true, null, messages);
        } catch (Exception e) {
            logger.error("Error fetching received messages", e);
            respondJson(response, false, "Failed to load messages", null);
        }
    }

    @GetMapping(value = "/conversation/{receiverId}")
    @ResponseBody
    public Map<String, Object> getConversation(@PathVariable Long receiverId,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        UsersBean currentUser = (UsersBean) session.getAttribute("user");

        if (currentUser == null) {
            return Map.of("success", false, "message", "Not authenticated");
        }

        UsersBean otherUser = messagesDAO.getUserById(receiverId);
        if (otherUser == null) {
            return Map.of("success", false, "message", "User not found");
        }

        List<MessageBean> messages = messagesDAO.getConversationBetween(currentUser.getAuthUserId(), receiverId);
        return Map.of("success", true, "data", Map.of("user", otherUser, "messages", messages));
    }

    @GetMapping(value = "/chat-heads")
    public void getChatHeads(HttpServletResponse response,
            @SessionAttribute("user") UsersBean currentUser) throws IOException {
        logger.info("GET /messages/chat-heads - Fetching chat heads for user {}", currentUser.getAuthUserId());

        try {
            List<UsersBean> chatHeads = messagesDAO.getChatHeadsForUser(currentUser.getAuthUserId());
            respondJson(response, true, null, chatHeads);
        } catch (Exception e) {
            logger.error("Error retrieving chat heads", e);
            respondJson(response, false, "Failed to load chat heads", null);
        }
    }

    // ============================
    // Sending Messages
    // ============================
    @PostMapping(value = "/send", consumes = "multipart/form-data")
    public void sendMessageToUser(HttpServletResponse response,
            @SessionAttribute("user") UsersBean sender,
            @RequestParam Long receiverId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) MultipartFile attachment) throws IOException {

        logger.info("📨 POST /messages/send - From {} to {}", sender.getAuthUserId(), receiverId);

        final long MAX_FILE_SIZE = 25L * 1024 * 1024;
        final Set<String> allowedTypes = Set.of(
                // Images, Videos, Audio
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "video/mp4", "video/webm", "video/quicktime",
                "audio/mpeg", "audio/mp3", "audio/wav",
                // Documents
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
                "text/plain", "text/csv",
                // Archive
                "application/zip"
        );

        final Set<String> blockedTypes = Set.of(
                "application/x-msdownload", "application/x-msdos-program",
                "application/x-ms-installer", "application/x-exe",
                "application/octet-stream", "application/java-archive",
                "application/x-sh", "application/x-bat", "application/x-jar",
                "application/vnd.microsoft.portable-executable"
        );

        boolean hasAttachment = attachment != null && !attachment.isEmpty();
        boolean hasContent = content != null && !content.isBlank();

        if (!hasAttachment && !hasContent) {
            respondJson(response, false, "Cannot send empty message", null);
            return;
        }

        MessageBean message = new MessageBean();

        if (hasAttachment) {
            if (attachment.getSize() > MAX_FILE_SIZE) {
                respondJson(response, false, "Attachment exceeds 25MB limit.", null);
                return;
            }

            String contentType = attachment.getContentType();
            if (contentType == null || !allowedTypes.contains(contentType) || blockedTypes.contains(contentType)) {
                respondJson(response, false, "Unsupported file type.", null);
                return;
            }

            message.setAttachment(attachment.getBytes());
            message.setAttachmentName(attachment.getOriginalFilename().replaceAll("[^a-zA-Z0-9.\\-_]", "_"));
            message.setAttachmentType(contentType);
        }

        message.setSenderId(sender.getAuthUserId());
        message.setReceiverId(receiverId);
        message.setMessageContent(hasContent ? content : null);

        Long messageId = messagesDAO.sendMessage(message);
        if (messageId == null || messageId <= 0) {
            respondJson(response, false, "Failed to send message", null);
            return;
        }

        MessageBean saved = messagesDAO.getMessageById(messageId);

        if (saved != null) {
            chatWebSocketHandler.broadcastSavedMessage(saved);
        }

        JsonObject data = new JsonObject();
        data.addProperty("messageId", saved.getMessageId());
        data.addProperty("senderId", saved.getSenderId());
        data.addProperty("receiverId", saved.getReceiverId());
        data.addProperty("messageContent", saved.getMessageContent()); 
        data.addProperty("attachmentType", saved.getAttachmentType());
        data.addProperty("sentAt", saved.getSentAt().toString());

        respondJson(response, true, "Message sent successfully", data);

    }

    @GetMapping(value = "/attachment/{id}")
    public void serveAttachment(@PathVariable("id") Long messageId,
            HttpServletResponse response) throws IOException {
        logger.info("GET /messages/attachment/{} - Downloading attachment", messageId);

        MessageBean message = messagesDAO.getMessageById(messageId);

        if (message != null && message.getAttachment() != null) {
            response.setContentType(message.getAttachmentType());
            response.setHeader("Content-Disposition", "inline; filename=\"" + message.getAttachmentName() + "\"");
            response.getOutputStream().write(message.getAttachment());
            response.flushBuffer();
        } else {
            logger.warn("Attachment not found for message {}", messageId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attachment not found.");
        }
    }

    // ============================
    // Message State
    // ============================
    @PatchMapping(value = "/mark-all-read")
    public void markAllAsRead(HttpServletResponse response,
            @SessionAttribute("user") UsersBean user) throws IOException {
        int count = messagesDAO.markAllMessagesAsRead(user.getAuthUserId());
        respondJson(response, true, null, Map.of("updatedCount", count));
    }

    @PatchMapping(value = "/mark-seen")
    public void markMessagesSeen(@RequestBody Map<String, Object> payload,
            HttpServletResponse response) {
        response.setContentType("application/json");
        try (PrintWriter out = response.getWriter()) {
            Long messageSenderId = ((Number) payload.get("messageSenderId")).longValue();
            Long viewerId = ((Number) payload.get("viewerId")).longValue();

            // ADD: Verify viewer is currently viewing sender's chat before marking as seen
            Long currentTarget = chatWebSocketHandler.getCurrentChatMap().get(viewerId);
            if (currentTarget == null || !currentTarget.equals(messageSenderId)) {
                System.out.println("⚠️ Skipped mark-seen — viewerId " + viewerId + " is NOT actively viewing " + messageSenderId);

                JsonObject json = new JsonObject();
                json.addProperty("success", false);
                json.addProperty("message", "User is not actively viewing this conversation.");
                out.print(json.toString());
                return;
            }

            int seenUpdated = messagesDAO.markMessagesAsSeen(messageSenderId, viewerId);
            int readUpdated = messagesDAO.markMessageAsRead(messageSenderId, viewerId);

            String latestSeenAt = messagesDAO.getLatestSeenTimestamp(messageSenderId, viewerId);
            String lastSentAt = messagesDAO.getLastMessageTimestamp(viewerId, messageSenderId);

            if (latestSeenAt != null && lastSentAt != null
                    && LocalDateTime.parse(latestSeenAt.replace(" ", "T"))
                            .isAfter(LocalDateTime.parse(lastSentAt.replace(" ", "T")))) {

                JSONObject seenMsg = new JSONObject();
                seenMsg.put("type", "SEEN_CONFIRMATION");
                seenMsg.put("seenBy", viewerId);
                seenMsg.put("seenTo", messageSenderId);
                seenMsg.put("seenAt", latestSeenAt);

                chatWebSocketHandler.sendToUser(messageSenderId, seenMsg);
            }

            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("message", seenUpdated + " seen, " + readUpdated + " read");
            json.addProperty("seen", seenUpdated);
            json.addProperty("read", readUpdated);

            out.print(json.toString());
        } catch (Exception e) {
            try {
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("message", "❌ Error marking seen: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().print(error.toString());
            } catch (IOException ignored) {
            }
        }
    }

    @PostMapping(value = "/conversation/load-more")
    public void loadMoreMessages(HttpServletResponse response,
            @SessionAttribute("user") UsersBean currentUser,
            @RequestBody Map<String, Object> payload) throws IOException {
        try {
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String beforeStr = payload.get("before").toString();
            Timestamp before = Timestamp.valueOf(beforeStr);
            int limit = 20;

            List<MessageBean> olderMessages = messagesDAO.getConversationBefore(
                    currentUser.getAuthUserId(), receiverId, limit, before
            );

            respondJson(response, true, null, olderMessages);
        } catch (Exception e) {
            logger.error("Error in /conversation/load-more", e);
            respondJson(response, false, "Failed to load older messages", null);
        }
    }

    // ============================
    // Delete & Count
    // ============================
    @DeleteMapping(value = "/delete")
    public void deleteMessage(HttpServletResponse response,
            @RequestParam Long messageId) throws IOException {
        logger.info("DELETE /messages/delete - Deleting message ID {}", messageId);
        boolean deleted = messagesDAO.deleteMessage(messageId) > 0;
        respondJson(response, deleted, deleted ? "Message deleted." : "Failed to delete", null);
    }

    @GetMapping(value = "/unread-count")
    public void getUnreadCount(HttpServletResponse response,
            @SessionAttribute("user") UsersBean user) throws IOException {
        logger.info("GET /messages/unread-count - Getting unread count for user {}", user.getAuthUserId());
        int count = messagesDAO.countUnreadMessages(user.getAuthUserId());

        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("unreadCount", count);
        writeJson(response, json);
    }

    // ============================
    // Update
    // ============================
    @PatchMapping(value = "/update")
    public void updateMessageContent(HttpServletResponse response,
            @SessionAttribute("user") UsersBean sender,
            @RequestBody Map<String, Object> payload) throws IOException {

        Long messageId = Long.valueOf(payload.get("messageId").toString());
        String newContent = payload.get("newContent").toString();

        logger.info("PATCH /messages/update - Editing message ID {}", messageId);

        boolean updated = messagesDAO.updateMessageContent(sender.getAuthUserId(), messageId, newContent) > 0;
        respondJson(response, updated, updated ? "Message updated." : "Failed to update message", null);
    }

    // ============================
    // User Search
    // ============================
    @PostMapping(value = "/search-users")
    public void searchUsers(HttpServletResponse response,
            @SessionAttribute("user") UsersBean currentUser,
            @RequestBody Map<String, Object> payload) throws IOException {

        String keyword = (String) payload.get("keyword");
        logger.info("POST /messages/search-users - Searching users for keyword: {}", keyword);

        try {
            List<UsersBean> results = messagesDAO.searchUsersByKeyword(currentUser.getAuthUserId(), keyword);
            respondJson(response, true, null, results);
        } catch (Exception e) {
            logger.error("Error during user search", e);
            respondJson(response, false, "Search failed", null);
        }
    }

    @GetMapping("/media/profile/{userId}")
    public void getUserProfileImage(@PathVariable Long userId, HttpServletResponse response) throws IOException {
        UsersBean user = messagesDAO.getUserById(userId);

        byte[] image = user.getProfileImage();
        if (image != null && image.length > 0) {
            response.setContentType("image/jpeg");
            response.getOutputStream().write(image);
            response.flushBuffer();
        } else {
            response.sendRedirect("/img/no-profile-picture.png");
        }
    }

    // ============================
    // Utility: JSON Response Helpers
    // ============================
    private void respondJson(HttpServletResponse response, boolean success, String message, Object data) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        if (message != null) {
            json.addProperty("message", message);
        }
        if (data != null) {
            json.add("data", gson.toJsonTree(data));
        }
        writeJson(response, json);
    }

    // ============================
    // Navbar Dropdown: Top 5 Unread Messages
    // ============================
    @GetMapping("/navbar/unread-top")
    public void getTopUnreadMessages(HttpServletResponse response,
            @SessionAttribute("user") UsersBean user) throws IOException {
        logger.info("GET /messages/navbar/unread-top - Loading top unread messages for user {}", user.getAuthUserId());

        try {
            List<LightweightMessageBean> raw = messagesDAO.getRecentUnreadPreview(user.getAuthUserId(), 5);

            List<Map<String, Object>> formatted = raw.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("messageId", msg.getMessageId());
                map.put("senderId", msg.getSenderId());
                map.put("senderName", msg.getSenderName());
                map.put("messageContent", msg.getMessageContent());
                map.put("attachmentType", msg.getAttachmentType());
                map.put("sentAt", msg.getSentAt() != null ? msg.getSentAt().toInstant().toString() : null);
                return map;
            }).toList();

            respondJson(response, true, null, formatted);
        } catch (Exception e) {
            logger.error("Failed to load top unread messages", e);
            respondJson(response, false, "Unable to load messages", null);
        }
    }

    // ============================
    // Navbar Badge: Unread Count
    // ============================
    @GetMapping("/navbar/unread-count")
    public void getNavbarUnreadCount(HttpServletResponse response,
            @SessionAttribute("user") UsersBean user) throws IOException {
        logger.info("GET /messages/navbar/unread-count - Checking unread count for user {}", user.getAuthUserId());

        try {
            int count = messagesDAO.countUnreadMessages(user.getAuthUserId());

            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("count", count);
            writeJson(response, json);
        } catch (Exception e) {
            logger.error("Error fetching navbar unread count", e);
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("message", "Failed to get unread count.");
            writeJson(response, error);
        }
    }

    private void writeJson(HttpServletResponse response, JsonObject json) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}
