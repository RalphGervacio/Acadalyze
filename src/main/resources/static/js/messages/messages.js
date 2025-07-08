// ✅ WebSocket Messaging Entry
// File: /js/messages.js

import { initWebSocketUserSearch, handleSearchUserResult, loadChatHeads, moveChatHeadToTop, updateChatHeadBadge, updateChatHeadLastMessage } from './handleSearchUserResult.js';
import { openConversationWith, handleIncomingMessage, updateUserStatusUI, handleSeenConfirmationNotification, forceHeaderStatusRefresh } from './handleConversationWith.js';
import { initSendMessageHandler } from './handleSendMessages.js';
import { initTypingIndicator, showTypingIndicator, hideTypingIndicator } from './handleTypingIndicator.js';
import { playNotificationSound } from './handleSoundNotification.js';
import { showMessageNotification, setNotificationSocket } from './handleMessageNotification.js';


let socket = null;
let userId = Number($('#sessionUserId').val());
const onlineUsersSet = new Set();
export { onlineUsersSet };

function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    socket = new WebSocket(`${protocol}${location.host}/ws/chat`);

    socket.onopen = () => {
        console.log('🟢 WebSocket connected');
        setNotificationSocket(socket);
        initWebSocketUserSearch(socket, (userId) => openConversationWith(userId, socket));
        initSendMessageHandler(socket);
        loadChatHeads((userId) => openConversationWith(userId, socket));
        initTypingIndicator(socket);

        // ✅ Auto-open conversation from URL param ?open=xxx
        const params = new URLSearchParams(window.location.search);
        const openId = params.get('open');

        if (openId && typeof openConversationWith === 'function') {
            waitForChatElementsAndOpen(openId, socket);
        }
    };

    socket.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        const type = msg.type;

        console.log('📥 WebSocket received:', msg);

        switch (type) {
            // 🔹 Messaging
            case 'MESSAGE':
                handleIncomingMessage(msg);

                const otherUserId = msg.senderId === userId ? msg.receiverId : msg.senderId;
                const preview = formatMessagePreview(msg.content, msg.senderId, msg.attachmentType);
                const timestamp = msg.timestamp ? new Date(msg.timestamp).getTime() : null;

                updateChatHeadLastMessage(otherUserId, preview, timestamp);

                moveChatHeadToTop(otherUserId);
                showMessageNotification(msg, userId);
                break;

            case 'EDIT_MESSAGE':
                break;
            case 'DELETE_MESSAGE':
                break;
            case 'RECALL_MESSAGE':
                break;

                // 🔹 Message Read / Seen
            case 'UNREAD_COUNT':
                console.log('🔔 UNREAD_COUNT:', msg);
                updateChatHeadBadge(msg.fromUserId, msg.unreadCount);
                break;
            case 'MARK_AS_READ':
                break;
            case 'SEEN_CONFIRMATION':
                console.log('👁️ Received SEEN_CONFIRMATION:', msg);
                handleSeenConfirmationNotification(msg);
                break;

                // 🔹 Typing Activity
            case 'TYPING':
                showTypingIndicator(msg.senderId);
                break;
            case 'STOP_TYPING':
                hideTypingIndicator(msg.senderId);
                break;

                // 🔹 Reactions
            case 'REACTION':
                break;

                // 🔹 Presence & Status
            case 'USER_ONLINE':
                console.log('🟢 USER_ONLINE:', msg.userId);
                onlineUsersSet.add(msg.userId);
                updateUserStatusUI(msg.userId, true);
                forceHeaderStatusRefresh(msg.userId);
                break;

            case 'USER_OFFLINE':
                console.log('🔴 USER_OFFLINE:', msg.userId);
                onlineUsersSet.delete(msg.userId);
                updateUserStatusUI(msg.userId, false);
                forceHeaderStatusRefresh(msg.userId);
                break;

            case 'ACTIVE_NOW':
                break;
            case 'STATUS_CHANGE':
                break;

                // 🔹 Attachments
            case 'PREVIEW_ATTACHMENT':
                break;
            case 'SEND_ATTACHMENT':
                break;

                // 🔹 Conversations
            case 'NEW_CONVERSATION':
                console.log('🆕 NEW_CONVERSATION:', msg);
                loadChatHeads((userId) => openConversationWith(userId, socket));
                break;
            case 'REMOVE_CONVERSATION':
                break;
            case 'PIN_CONVERSATION':
                break;

                // 🔹 Sync & State
            case 'SYNC_STATE':
                console.log('🔄 SYNC_STATE seen status:', msg);
                handleSeenConfirmationNotification(msg);
                break;

            case 'FOCUS_CHANGED':
                break;

                // 🔹 Search
            case 'SEARCH_USER_RESULT':
                handleSearchUserResult(msg, (userId) => openConversationWith(userId, socket));
                break;

                // 🔹 Pings
            case 'PING':
                socket.send(JSON.stringify({type: 'PONG'}));
                break;
            case 'PONG':
                console.log('🏓 Pong received');
                break;

                // 🔹 Notifications
            case 'NOTIFICATION_SOUND':
                if (Number($('#selectedUserId').val()) !== otherUserId) {
                    playNotificationSound();
                }
                break;

                // 🔹 Unknown
            default:
                console.warn('❓ Unknown WebSocket message type:', type);
        }
    };

    socket.onclose = () => {
        console.warn('🔴 WebSocket closed. Reconnecting in 5s...');
        setTimeout(connectWebSocket, 5000);
    };

    socket.onerror = (error) => {
        console.error('WebSocket error:', error);
    };
}

connectWebSocket();


export function formatMessagePreview(content, senderId, attachmentType = null) {
    const isSender = senderId === userId;
    const hasText = typeof content === 'string' && content.trim().length > 0 && content.trim() !== '[file]';
    let preview = '';

    if (attachmentType && !hasText) {
        if (attachmentType.startsWith('image/'))
            preview = 'sent a photo';
        else if (attachmentType.startsWith('video/'))
            preview = 'sent a video';
        else if (attachmentType.startsWith('audio/'))
            preview = 'sent an audio clip';
        else if (attachmentType.includes('pdf') || attachmentType.includes('word'))
            preview = 'sent a document';
        else if (attachmentType.includes('excel'))
            preview = 'sent a spreadsheet';
        else if (attachmentType.includes('powerpoint'))
            preview = 'sent a presentation';
        else if (attachmentType.includes('text/') || attachmentType.includes('csv'))
            preview = 'sent a text file';
        else if (attachmentType === 'application/zip')
            preview = 'sent a zip file';
        else if (
                attachmentType.includes('msdownload') ||
                attachmentType.includes('x-ms') ||
                attachmentType.includes('octet-stream')
                )
            preview = '';
        else
            preview = 'sent a file';
    } else if (hasText && attachmentType) {
        // 🔥 This fixes the [file] 📎 bug
        preview = content.trim() + ' 📎';
    } else if (hasText) {
        preview = content.trim();
    }

    if (!preview.trim()) {
        return '';
    }

    if (isSender) {
        preview = `You: ${preview}`;
    }

    if (preview.length > 25) {
        preview = preview.slice(0, 25) + '…';
    }

    return preview;
}

function waitForChatElementsAndOpen(userId, socket) {
    const maxWait = 3000;
    const interval = 100;
    let waited = 0;

    const checker = setInterval(() => {
        const ready = document.querySelector('#chatMessages') && document.querySelector('#chatHeader');

        if (ready) {
            clearInterval(checker);
            openConversationWith(Number(userId), socket);
        } else if ((waited += interval) >= maxWait) {
            clearInterval(checker);
            console.warn('⏱️ Timeout waiting for chat layout.');
        }
    }, interval);
}

$(document).ready(function () {
    $('#toggleSidebarBtn').on('click', function () {
        const $sidebar = $('#chatSidebar');
        $sidebar.toggleClass('collapsed-sidebar');

        const isCollapsed = $sidebar.hasClass('collapsed-sidebar');

        // ✅ Remove inline styles so CSS can take over
        $('.badge-on-avatar').each(function () {
            this.style.removeProperty('display');
        });
        $('.badge-in-details').each(function () {
            this.style.removeProperty('display');
        });
    });

    // 🔁 Notify backend on tab focus/blur
    document.addEventListener('visibilitychange', () => {
        if (socket?.readyState === 1) {
            socket.send(JSON.stringify({
                type: 'FOCUS_CHANGED',
                userId,
                focused: !document.hidden
            }));
        }
    });
});
