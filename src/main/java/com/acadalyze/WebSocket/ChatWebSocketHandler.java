package com.acadalyze.WebSocket;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.beans.messages.MessageBean;
import com.acadalyze.dao.messages.MessagesDAO;
import java.io.IOException;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    MessagesDAO messageDao;

    private final Map<Long, Set<WebSocketSession>> userSessionsMap = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> userFocusMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> currentChatMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessionsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
            System.out.println("🟢 WebSocket connected for userId: " + userId);

            // Only broadcast if it's the first active session
            if (userSessionsMap.get(userId).size() == 1) {
                broadcastUserStatus(userId, true);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessionsMap.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessionsMap.remove(userId);
                    broadcastUserStatus(userId, false); // Only if truly offline
                }
            }
            System.out.println("🔴 WebSocket closed for userId: " + userId + " | Reason: " + status);
            System.out.println("✅ userSessions now contains: " + userSessionsMap.keySet());
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject payload = new JSONObject(message.getPayload());
            String type = payload.getString("type");

            System.out.println("📨 Received WebSocket payload: " + message.getPayload());
            System.out.println("🧠 userSessionsMap keys: " + userSessionsMap.keySet());

            switch (type) {
                case "MESSAGE":
                    handleIncomingMessage(session, payload);
                    break;

                case "MARK_AS_READ":
                    // Mark as read logic
                    break;

                case "MARK_AS_SEEN":
                    handleMarkAsSeen(payload);
                    break;

                case "DELETE_MESSAGE":
                    // Soft delete logic
                    break;

                case "EDIT_MESSAGE":
                    // Update message content
                    break;

                case "RECALL_MESSAGE":
                    // Fully retract/delete the message from both sides
                    break;

                case "REACTION":
                    // Add/remove emoji reaction
                    break;

                case "TYPING":
                    handleTyping(payload);
                    break;

                case "STOP_TYPING":
                    handleStopTyping(payload);
                    break;

                case "PING":
                    // Reply with PONG for connection keep-alive
                    break;

                case "PONG":
                    // Optional: confirm keep-alive
                    break;

                case "USER_ONLINE":
                    // Track or notify user is online
                    break;

                case "USER_OFFLINE":
                    // Track or notify user is offline
                    break;

                case "ACTIVE_NOW":
                    // Presence change logic
                    break;

                case "STATUS_CHANGE":
                    // Set user status (e.g. away, busy)
                    break;

                case "SEND_ATTACHMENT":
                    // Handle attachment sending (metadata)
                    break;

                case "PREVIEW_ATTACHMENT":
                    // Preview/stream attachment if necessary
                    break;

                case "NEW_CONVERSATION":
                    // Started new thread
                    break;

                case "OPEN_CONVERSATION":
                    handleOpenConversation(payload);
                    break;

                case "REMOVE_CONVERSATION":
                    // Archive or hide chat thread
                    break;

                case "PIN_CONVERSATION":
                    // Pin/unpin conversation
                    break;

                case "SYNC_STATE":
                    handleSyncState(payload);
                    break;

                case "SEARCH_USER":
                    handleSearchUser(session, payload);
                    break;

                case "FOCUS_CHANGED":
                    handleFocusChanged(payload);
                    break;

                default:
                    System.err.println("❌ Unknown WebSocket message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("❌ Exception in WebSocket handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Object userIdAttr = session.getAttributes().get("authUserId");
        return (userIdAttr instanceof Long) ? (Long) userIdAttr : null;
    }

    private void handleTyping(JSONObject payload) {
        try {
            Long senderId = payload.getLong("senderId");
            Long receiverId = payload.getLong("receiverId");

            Set<WebSocketSession> receiverSessions = userSessionsMap.get(receiverId);
            if (receiverSessions != null) {
                for (WebSocketSession receiverWs : receiverSessions) {
                    if (receiverWs.isOpen()) {
                        JSONObject notify = new JSONObject();
                        notify.put("type", "TYPING");
                        notify.put("senderId", senderId);
                        receiverWs.sendMessage(new TextMessage(notify.toString()));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error in handleTyping: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStopTyping(JSONObject payload) {
        try {
            Long senderId = payload.getLong("senderId");
            Long receiverId = payload.getLong("receiverId");

            Set<WebSocketSession> receiverSessions = userSessionsMap.get(receiverId);
            if (receiverSessions != null) {
                for (WebSocketSession receiverWs : receiverSessions) {
                    if (receiverWs.isOpen()) {
                        JSONObject notify = new JSONObject();
                        notify.put("type", "STOP_TYPING");
                        notify.put("senderId", senderId);
                        receiverWs.sendMessage(new TextMessage(notify.toString()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error in handleStopTyping: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSearchUser(WebSocketSession session, JSONObject payload) {
        try {
            Long currentUserId = getUserIdFromSession(session);
            if (currentUserId == null) {
                System.err.println("⚠️ SearchUser: Session user ID is null");
                return;
            }

            String query = payload.optString("query", "").trim();
            if (query.length() < 2) {
                System.out.println("🔍 Ignored short search query: '" + query + "'");
                return;
            }

            System.out.println("🔍 Searching users for: " + query);

            List<UsersBean> users = messageDao.searchUsersByKeyword(currentUserId, query);

            JSONArray resultArray = new JSONArray();
            for (UsersBean user : users) {
                JSONObject obj = new JSONObject();
                obj.put("userId", user.getAuthUserId());
                obj.put("name", user.getFullName());
                obj.put("avatar", "/messages/media/profile/" + user.getAuthUserId());
                obj.put("status", "");
                resultArray.put(obj);
            }

            JSONObject response = new JSONObject();
            response.put("type", "SEARCH_USER_RESULT");
            response.put("query", query);
            response.put("results", resultArray);

            session.sendMessage(new TextMessage(response.toString()));

        } catch (Exception e) {
            System.err.println("❌ Error in handleSearchUser: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleIncomingMessage(WebSocketSession senderSession, JSONObject payload) {
        try {
            Long senderId = getUserIdFromSession(senderSession);
            Long receiverId = payload.getLong("receiverId");
            String content = payload.optString("content", "");
            String attachmentType = payload.optString("attachmentType", null);
            String attachmentName = payload.optString("attachmentName", null);

            if (receiverId == null || senderId == null) {
                System.err.println("⚠️ Invalid sender or receiver in MESSAGE payload.");
                return;
            }

            MessageBean message = new MessageBean();
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setMessageContent(content);
            message.setAttachment(null);
            message.setAttachmentName(null);
            message.setAttachmentType(attachmentType);
            message.setAttachmentName(attachmentName);

            Long messageId = messageDao.sendMessage(message);
            message.setMessageId(messageId);
            message.setSentAt(LocalDateTime.now().toString());

            broadcastMessagePayload(message);

            System.out.println("📤 Message sent from " + senderId + " to " + receiverId + " (ID: " + messageId + ")");
        } catch (Exception e) {
            System.err.println("❌ Error in handleIncomingMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMarkAsSeen(JSONObject payload) throws Exception {
        Long messageSenderId = payload.getLong("messageSenderId");
        Long viewerId = payload.getLong("viewerId");

        Long currentTarget = currentChatMap.get(viewerId);
        Boolean isFocused = userFocusMap.getOrDefault(viewerId, false);

        System.out.println("🔎 handleMarkAsSeen — viewerId: " + viewerId + ", target: " + currentTarget + ", focused: " + isFocused);

        if (currentTarget == null || !currentTarget.equals(messageSenderId)) {
            System.out.println("⚠️ Skipped MARK_AS_SEEN — not viewing the correct chat.");
            return;
        }

        if (!isFocused) {
            System.out.println("⚠️ Skipped MARK_AS_SEEN — tab is not focused.");
            return;
        }

        Set<WebSocketSession> senderSessions = userSessionsMap.get(messageSenderId);
        if (senderSessions == null || senderSessions.isEmpty()) {
            System.out.println("⚠️ Skipped MARK_AS_SEEN — sender offline.");
            return;
        }

        String lastMessageSentAt = messageDao.getLastMessageTimestamp(messageSenderId, viewerId);
        if (lastMessageSentAt == null) {
            System.out.println("ℹ️ No message from sender to mark as seen.");
            return;
        }

        String latestSeenAt = messageDao.getLatestSeenTimestamp(messageSenderId, viewerId);
        String normalizedLastSent = lastMessageSentAt.replace(" ", "T");

        if (latestSeenAt != null) {
            String normalizedSeen = latestSeenAt.replace(" ", "T");
            if (!LocalDateTime.parse(normalizedLastSent).isAfter(LocalDateTime.parse(normalizedSeen))) {
                System.out.println("⏩ Skipped MARK_AS_SEEN — already seen or nothing new");
                return;
            }
        }

        int updated = messageDao.markMessagesAsSeen(messageSenderId, viewerId);
        System.out.println("👁️‍🗨️ " + updated + " messages marked as seen from " + messageSenderId + " to " + viewerId);

        JSONObject seenNotification = new JSONObject();
        seenNotification.put("type", "SEEN_CONFIRMATION");
        seenNotification.put("seenBy", viewerId);
        seenNotification.put("seenTo", messageSenderId);
        seenNotification.put("seenAt", LocalDateTime.now().toString());

        for (WebSocketSession session : senderSessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(seenNotification.toString()));
            }
        }

        System.out.println("📨 Sent SEEN_CONFIRMATION to messageSenderId: " + messageSenderId);
        System.out.println("🔎 Comparing latestSeenAt: " + latestSeenAt + " vs lastMessageSentAt: " + lastMessageSentAt);
    }

    private void handleOpenConversation(JSONObject payload) {
        Long userId = payload.getLong("userId");
        Long withUserId = payload.getLong("withUserId");

        currentChatMap.put(userId, withUserId);
        System.out.println("👁️‍🗨️ " + userId + " is now viewing conversation with " + withUserId);
    }

    private void handleSyncState(JSONObject payload) {
        try {
            System.out.println("🔄 Received SYNC_STATE: " + payload);
            Long senderId = payload.getLong("userId");
            Long withUserId = payload.getLong("withUserId");

            // Correct direction: check if withUserId is viewing senderId
            Long currentTarget = currentChatMap.get(withUserId);
            System.out.println("📌 currentChatMap.get(" + withUserId + ") = " + currentTarget);
            System.out.println("📌 Expecting " + withUserId + " to be currently viewing " + senderId);
            System.out.println("🧠 currentChatMap dump: " + currentChatMap);

            if (currentTarget == null || !currentTarget.equals(senderId)) {
                System.out.println("⏩ SYNC_STATE skipped — " + withUserId + " is not currently viewing " + senderId);
                return;
            }

            String seenAt = messageDao.getLatestSeenTimestamp(senderId, withUserId);
            String lastMessageSentAt = messageDao.getLastMessageTimestamp(withUserId, senderId);

            if (seenAt != null && lastMessageSentAt != null
                    && LocalDateTime.parse(seenAt.replace(" ", "T")).isAfter(LocalDateTime.parse(lastMessageSentAt.replace(" ", "T")))) {

                Set<WebSocketSession> senderSessions = userSessionsMap.get(senderId);
                if (senderSessions != null) {
                    JSONObject seenSync = new JSONObject();
                    seenSync.put("type", "SEEN_CONFIRMATION");
                    seenSync.put("seenBy", withUserId);
                    seenSync.put("seenTo", senderId);
                    seenSync.put("seenAt", seenAt);

                    for (WebSocketSession session : senderSessions) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(seenSync.toString()));
                        }
                    }

                    System.out.println("🔁 SYNC_STATE: Sent SEEN_CONFIRMATION to userId: " + senderId + " from withUserId: " + withUserId);
                } else {
                    System.out.println("📴 SYNC_STATE: senderId " + senderId + " is offline.");
                }
            } else {
                System.out.println("ℹ️ SYNC_STATE: Skipped — no new seenAt or user hasn't seen latest message");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in handleSyncState: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcastSavedMessage(MessageBean message) {
        try {
            broadcastMessagePayload(message);
            System.out.println("📡 Broadcasted saved message ID: " + message.getMessageId());
        } catch (Exception e) {
            System.err.println("❌ Error in broadcastSavedMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastMessagePayload(MessageBean message) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("type", "MESSAGE");
        payload.put("senderId", message.getSenderId());
        payload.put("receiverId", message.getReceiverId());
        payload.put("messageId", message.getMessageId());
        payload.put("content", message.getMessageContent() != null ? message.getMessageContent() : "[file]");
        payload.put("attachmentType", message.getAttachmentType());
        payload.put("attachmentName", message.getAttachmentName());
        payload.put("timestamp", message.getSentAt().toString());
        payload.put("fullName", message.getSenderName());

        Set<WebSocketSession> senderSessions = userSessionsMap.get(message.getSenderId());
        Set<WebSocketSession> receiverSessions = userSessionsMap.get(message.getReceiverId());

        if (senderSessions != null) {
            for (WebSocketSession s : senderSessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(payload.toString()));
                }
            }
        }

        if (receiverSessions != null) {
            for (WebSocketSession r : receiverSessions) {
                if (r.isOpen()) {
                    r.sendMessage(new TextMessage(payload.toString()));
                }
            }
        }

        int unreadCount = messageDao.countUnreadMessagesFrom(message.getSenderId(), message.getReceiverId());
        JSONObject unreadUpdate = new JSONObject();
        unreadUpdate.put("type", "UNREAD_COUNT");
        unreadUpdate.put("fromUserId", message.getSenderId());
        unreadUpdate.put("toUserId", message.getReceiverId());
        unreadUpdate.put("unreadCount", unreadCount);

        if (receiverSessions != null) {
            unreadUpdate.put("type", "UNREAD_COUNT");
            unreadUpdate.put("fromUserId", message.getSenderId());
            unreadUpdate.put("toUserId", message.getReceiverId());
            unreadUpdate.put("unreadCount", unreadCount);

            for (WebSocketSession r : receiverSessions) {
                if (r.isOpen()) {
                    r.sendMessage(new TextMessage(unreadUpdate.toString()));
                }
            }
        }

        if (messageDao.isNewConversation(message.getSenderId(), message.getReceiverId())) {
            JSONObject newConv = new JSONObject();
            newConv.put("type", "NEW_CONVERSATION");

            if (receiverSessions != null) {
                newConv.put("userId", message.getSenderId());
                for (WebSocketSession r : receiverSessions) {
                    if (r.isOpen()) {
                        r.sendMessage(new TextMessage(newConv.toString()));
                    }
                }
            }

            if (senderSessions != null) {
                newConv.put("userId", message.getReceiverId());
                for (WebSocketSession s : senderSessions) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(newConv.toString()));
                    }
                }
            }
        }

    }

    private void broadcastUserStatus(Long userId, boolean online) {
        try {
            JSONObject statusMsg = new JSONObject();
            statusMsg.put("type", online ? "USER_ONLINE" : "USER_OFFLINE");
            statusMsg.put("userId", userId);

            TextMessage textMsg = new TextMessage(statusMsg.toString());

            for (Set<WebSocketSession> sessions : userSessionsMap.values()) {
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        session.sendMessage(textMsg);
                    }
                }
            }

            System.out.println("📡 Broadcasted " + (online ? "USER_ONLINE" : "USER_OFFLINE") + " for userId: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Error broadcasting user status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendToUser(Long userId, JSONObject payload) throws IOException {
        Set<WebSocketSession> sessions = userSessionsMap.get(userId);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(payload.toString()));
                }
            }
        } else {
            System.out.println("⚠️ sendToUser: User " + userId + " is offline or session is closed.");
        }
    }

    private void handleFocusChanged(JSONObject payload) {
        if (!payload.has("userId")) {
            return;
        }

        Long userId = payload.optLong("userId", -1);
        boolean focused = payload.optBoolean("focused", false);

        if (userId > 0) {
            userFocusMap.put(userId, focused);
            System.out.println("🔆 Updated focus state — userId: " + userId + ", focused: " + focused);
        } else {
            System.err.println("⚠️ Invalid userId in FOCUS_CHANGED payload: " + payload);
        }
    }

    public Map<Long, Long> getCurrentChatMap() {
        return currentChatMap;
    }

}
