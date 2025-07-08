import { formatMessagePreview } from './messages.js';
import { playNotificationSound } from './handleSoundNotification.js';
import { openConversationWith } from './handleConversationWith.js';

const MAX_NOTIFICATIONS = 5;
const $messageList = $('#messageList');
const $messageBadge = $('#messageBadge');
let socketRef = null;

/**
 * Injects the socket instance into this module
 * @param {WebSocket} socket - active WebSocket connection
 */
export function setNotificationSocket(socket) {
    socketRef = socket;
}

let messageNotifications = new Map(); // key = userId, value = { count, latestMessage }

loadNotificationsFromStorage(); // 🔁 Load from localStorage on load
listenForCrossTabClears();      // 📡 Listen for other tabs

export function showMessageNotification(msg, currentUserId) {
    if (msg.senderId === currentUserId) {
        return;
    }
    const otherUserId = msg.senderId;
    const existing = messageNotifications.get(otherUserId);

    if (existing) {
        existing.count += 1;
        existing.latestMessage = msg;
    } else {
        messageNotifications.set(otherUserId, {
            count: 1,
            latestMessage: msg
        });
    }

    trimIfTooMany();
    renderMessageList();
    updateBadge();
    saveNotificationsToStorage();

    if (Number($('#selectedUserId').val()) !== otherUserId) {
        playNotificationSound();
    }
}

function trimIfTooMany() {
    if (messageNotifications.size <= MAX_NOTIFICATIONS)
        return;

    const entries = Array.from(messageNotifications.entries());
    entries.sort((a, b) => {
        const t1 = new Date(b[1].latestMessage.timestamp).getTime();
        const t2 = new Date(a[1].latestMessage.timestamp).getTime();
        return t1 - t2;
    });

    while (entries.length > MAX_NOTIFICATIONS) {
        const [userIdToRemove] = entries.pop();
        messageNotifications.delete(userIdToRemove);
    }
}

function saveNotificationsToStorage() {
    const raw = Array.from(messageNotifications.entries()).map(([userId, value]) => ({
            userId,
            count: value.count,
            latestMessage: value.latestMessage
        }));
    localStorage.setItem('chat_message_notifications', JSON.stringify(raw));
}

function loadNotificationsFromStorage() {
    const data = localStorage.getItem('chat_message_notifications');
    if (!data)
        return;

    try {
        const parsed = JSON.parse(data);
        parsed.forEach(item => {
            messageNotifications.set(item.userId, {
                count: item.count,
                latestMessage: item.latestMessage
            });
        });

        renderMessageList();
        updateBadge();
    } catch (e) {
        console.warn('⚠️ Failed to parse stored notifications:', e);
        localStorage.removeItem('chat_message_notifications');
    }
}

function renderMessageList() {
    $messageList.empty();

    if (messageNotifications.size === 0) {
        $messageList.html(`<div class="text-center text-muted py-2">No unread messages</div>`);
        return;
    }

    const sorted = Array.from(messageNotifications.entries()).sort((a, b) => {
        const t1 = new Date(b[1].latestMessage.timestamp).getTime();
        const t2 = new Date(a[1].latestMessage.timestamp).getTime();
        return t1 - t2;
    });

    sorted.forEach(([userId, { count, latestMessage }]) => {
        const avatarUrl = `/messages/media/profile/${userId}`;
        const preview = formatMessagePreview(latestMessage.content, latestMessage.senderId, latestMessage.attachmentType);
        const time = formatTimeAgo(latestMessage.timestamp);
        const fullName = latestMessage.fullName;

        const countBadge = count > 1
                ? `<span class="msg-badge badge bg-primary text-light fw-semibold px-2 py-1 small flex-shrink-0">${count}</span>`
                : '';

        const html = `
                <a href="#" class="message-item px-3 py-2 rounded-2 d-flex flex-column gap-1 text-decoration-none" data-user-id="${userId}" data-read="false">
                    <div class="d-flex align-items-start gap-3">
                        <img src="${avatarUrl}" class="msg-avatar shadow-sm" width="40" height="40">
                        <div class="msg-info w-100">
                            <div class="msg-name-row">
                                <span class="msg-name text-truncate">${fullName}</span>
                            </div>
                            <div class="msg-preview text-truncate">${preview}</div>
                        </div>
                    </div>
                    <div class="msg-time text-end text-muted small">${time}</div>
                    ${countBadge}
                </a>
            `;

        $messageList.append(html);
    });
}

function updateBadge() {
    const count = Array.from(messageNotifications.values())
            .reduce((sum, val) => sum + val.count, 0);

    if (count > 0) {
        $messageBadge.text(count).removeClass('d-none').fadeIn(150);
    } else {
        $messageBadge.addClass('d-none').text('');
    }
}

function formatTimeAgo(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = Math.floor((now - date) / 60000);

    if (diff < 1)
        return 'Just now';
    if (diff < 60)
        return `${diff}m ago`;
    const hours = Math.floor(diff / 60);
    if (hours < 24)
        return `${hours}h ago`;
    return date.toLocaleDateString();
}

// 🧼 Handle message item click
$(document).ready(function () {
    $('#messageList').on('click', '.message-item', function (e) {
        e.preventDefault();
        const userId = $(this).data('user-id');

        if (userId) {
            messageNotifications.delete(userId);
            saveNotificationsToStorage();

            // 📡 Sync to other tabs
            localStorage.setItem('notification_cleared', JSON.stringify({userId, time: Date.now()}));

            renderMessageList();
            updateBadge();

            // ✅ Redirect only if not already in /messages
            if (window.location.pathname === '/messages') {
                openConversationWith(userId, socketRef);
            } else {
                window.location.href = `/messages?open=${userId}`;
            }
        }
    });
});

// 📡 Sync clears from other tabs
function listenForCrossTabClears() {
    window.addEventListener('storage', (event) => {
        if (event.key === 'notification_cleared' && event.newValue) {
            try {
                const {userId} = JSON.parse(event.newValue);
                if (userId && messageNotifications.has(userId)) {
                    messageNotifications.delete(userId);
                    saveNotificationsToStorage();
                    renderMessageList();
                    updateBadge();
                }
            } catch (e) {
                console.warn('⚠️ Failed to handle cross-tab sync:', e);
            }
        }
    });
}
