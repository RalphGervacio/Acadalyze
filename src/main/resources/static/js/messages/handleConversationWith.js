// ✅ UI Renderer and Incoming WebSocket Message Handler
// File: /js/messages/handleConversationWith.js

import { removeUnreadBadge } from './handleSearchUserResult.js';
import { onlineUsersSet } from './messages.js';
import { playNotificationSound } from './handleSoundNotification.js';
import { handleSeeProfile } from './handleInfoButtons.js';

let currentMessages = [];
let socketRef = null;
let receiverUserRef = null;

// Initialize tooltips
var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl);
});

export async function openConversationWith(userId, socket) {
    socketRef = socket;

    try {
        const response = await fetch(`/messages/conversation/${userId}`);
        const result = await response.json();

        if (!result.success) {
            console.warn('⚠️ Failed to fetch conversation:', result.message);
            return;
        }

        const {user, messages} = result.data;
        receiverUserRef = user;
        $('#selectedUserId').val(user.authUserId);
        currentMessages = messages;

        renderConversationHeader(user);
        renderMessages(currentMessages, user);
        requestAnimationFrame(scrollChatToBottom);
        setTimeout(() => scrollChatToBottom(), 100);
        console.log("Rendering the messages after chathead click");

        const sessionUserId = Number($('#sessionUserId').val());

        if (socket?.readyState === 1) {
            // ✅ Let backend know which chat is currently open
            socket.send(JSON.stringify({
                type: 'OPEN_CONVERSATION',
                userId: sessionUserId,
                withUserId: user.authUserId
            }));
        }
        console.log("USERID: " + sessionUserId);
        console.log("AUTHUSERID: " + user.authUserId);
        console.log('👀 Visibility:', document.visibilityState);
        console.log('📡 Socket readyState:', socket?.readyState);
        console.log('📨 Result:', result);

        if (document.visibilityState === 'visible') {
            attemptMarkSeen(socket);

            if (socket?.readyState === 1) {
                const payload = {
                    type: 'SYNC_STATE',
                    userId: Number($('#sessionUserId').val()),
                    withUserId: user.authUserId
                };
                console.log('🔄 Sending SYNC_STATE:', payload);
                socket.send(JSON.stringify(payload));
            } else {
                console.warn('⚠️ WebSocket not ready to send SYNC_STATE');
            }
        }

        $('#chatHeader').show();
        $('#chatMessages').removeClass('d-none');
        $('.message-input').show();
        $('#emptyChatPlaceholder').hide();
        $('#messageInput').prop('disabled', false).focus();

    } catch (err) {
        console.error('❌ Error opening conversation:', err);
    }
    $('#chatMessages').off('scroll').on('scroll', function () {
        attemptMarkSeen(socket);
    });

}

document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        // ONLY call this if chat is open
        const $chatMessages = $('#chatMessages');
        if (!$chatMessages.hasClass('d-none')) {
            attemptMarkSeen(socketRef);
        }
    }
});

// ✅ Called when receiving a new message
export function handleIncomingMessage(msg) {
    const selectedUserId = Number($('#selectedUserId').val());
    const sessionUserId = Number($('#sessionUserId').val());

    const isOwnMessage = msg.senderId === sessionUserId;
    const isActiveChat = isOwnMessage
            ? msg.receiverId === selectedUserId
            : msg.senderId === selectedUserId;

    console.log({isOwnMessage, isActiveChat, visibility: document.visibilityState});
    if (!isOwnMessage && !isActiveChat) {
        playNotificationSound();
    }

    if (!isActiveChat)
        return;

    const normalized = {
        messageId: msg.messageId,
        senderId: msg.senderId,
        receiverId: msg.receiverId,
        messageContent: msg.messageContent || msg.content || null,
        attachmentType: msg.attachmentType || null,
        attachmentName: msg.attachmentName || null,
        sentAt: msg.sentAt || msg.timestamp || new Date().toISOString(),
        seenAt: msg.seenAt || null
    };

    // ✅ Prevent duplicate render
    if (currentMessages.some(m => m.messageId === normalized.messageId)) {
        console.warn('⚠️ Duplicate message detected. Skipping render:', normalized.messageId);
        return;
    }

    currentMessages.push(normalized);
    const $chat = $('#chatMessages');

    // ✅ FIX: Render the normalized one, not the raw `msg`
    const html = renderMessageBubble(normalized, isOwnMessage, receiverUserRef);
    $chat.append(html);

    $('#typingIndicator').remove();

    requestAnimationFrame(() => {
        const isOwnOrVisible = isOwnMessage || document.visibilityState === 'visible';
        if (isOwnOrVisible) {
            scrollChatToBottom();
        }
    });

    if (!isOwnMessage && socketRef?.readyState === 1) {
        handleSeenConfirmation(msg.senderId, socketRef);
    }

    console.log('✅ Rendered incoming message:', normalized.messageId);
}

// ✅ Sends PATCH + WebSocket MARK_AS_SEEN
export function handleSeenConfirmation(selectedUserId, socket) {
    const sessionUserId = Number($('#sessionUserId').val());

    // ⏳ Delay to allow FOCUS_CHANGED to update on server
    setTimeout(() => {
        if (socket?.readyState === 1 && document.visibilityState === 'visible') {
            try {
                socket.send(JSON.stringify({
                    type: 'MARK_AS_SEEN',
                    messageSenderId: selectedUserId,
                    viewerId: sessionUserId
                }));
                console.log(`📤 Sent MARK_AS_SEEN for senderId: ${selectedUserId}`);
            } catch (err) {
                console.error('❌ Failed to send MARK_AS_SEEN via WebSocket:', err);
            }
        } else {
            console.warn('⏳ Skipped MARK_AS_SEEN — socket not open or tab not focused');
        }

        // ✅ Fallback AJAX call
        patchData('/messages/mark-seen', {
            messageSenderId: selectedUserId,
            viewerId: sessionUserId
        }).done(json => {
            if (json.success) {
                console.log(`👁️ Seen: ${json.seen}, Read: ${json.read}`);
                removeUnreadBadge(selectedUserId);
            }
        }).fail(error => {
            console.error('❌ Failed to mark messages as seen (AJAX fallback):', error);
        });

    }, 150); // 🧠 Slight delay gives time for FOCUS_CHANGED to register server-side
}

// ✅ Handles SEEN_CONFIRMATION from WebSocket
export function handleSeenConfirmationNotification(msg) {
    const sessionUserId = Number($('#sessionUserId').val());
    const selectedUserId = Number($('#selectedUserId').val());

    // ✅ Only proceed if this confirmation is for the current open chat
    if (msg.seenTo !== sessionUserId || msg.seenBy !== selectedUserId)
        return;

    currentMessages = currentMessages.map(message => {
        if (message.senderId === sessionUserId && !isValidSeenAt(message.seenAt)) {
            return {...message, seenAt: msg.seenAt};
        }
        return message;
    });

    $('#chatMessages .chat-message-wrapper.text-end').each(function () {
        const $wrapper = $(this);
        const messageId = Number($wrapper.attr('data-id'));

        // 🔍 Try to find and update the message in memory
        const message = currentMessages.find(m => m.messageId === messageId);
        if (message && message.senderId === sessionUserId && !isValidSeenAt(message.seenAt)) {
            message.seenAt = msg.seenAt; // ✅ inject seenAt from WebSocket
        }

        // 🔁 Update the DOM regardless of local `currentMessages` state
        const $status = $wrapper.find('.chat-status');
        if (
                message?.senderId === sessionUserId &&
                isValidSeenAt(msg.seenAt) &&
                $status.length &&
                !$status.hasClass('seen')
                ) {
            $status.addClass('seen').html(`
                <div class="text-end">
                    <i class="bi bi-check2-circle"></i> Seen
                    <small>${formatTimestamp(msg.seenAt)}</small>
                </div>
            `);
        }
    });

}

function isUserOnline(userId) {
    return onlineUsersSet.has(userId);
}

export function updateUserStatusUI(userId, isOnline) {
    const selectedUserId = Number($('#selectedUserId').val());

    if (userId !== selectedUserId)
        return;

    const $status = $('#chatHeaderTitle .small.text-muted');
    if ($status.length) {
        $status.text(isOnline ? 'Active' : 'Offline');
    }
}

export function forceHeaderStatusRefresh(userId) {
    const selectedUserId = Number($('#selectedUserId').val());
    if (userId === selectedUserId && receiverUserRef) {
        renderConversationHeader(receiverUserRef);
    }
}

function renderConversationHeader(user) {
    const isOnline = onlineUsersSet.has(user.authUserId);
    const status = isOnline ? 'Active' : 'Offline';

    $('#chatHeaderTitle').html(`
    <div class="d-flex align-items-center gap-2 p-2 rounded justify-content-between"
         style="background-color: var(--sidebar-bg); color: var(--bs-body-color);">

        <div class="d-flex align-items-center gap-2">
            <img src="/messages/media/profile/${user.authUserId}" class="rounded-circle" width="36" height="36">
            <div>
                <div class="fw-semibold">${user.fullName}</div>
                <div class="small text-muted">${status}</div>
            </div>
        </div>

        <div class="position-relative">
            <div class="info-icon-circle d-flex align-items-center justify-content-center" id="infoActionTrigger">
                <i class="bi bi-info-circle"></i>
            </div>

            <div class="info-actions" id="infoActionMenu">
                <button class="btn btn-sm btn-primary info-action action-1" 
                        data-bs-toggle="tooltip" 
                        data-bs-placement="left" 
                        title="See Profile">
                    <i class="bi bi-person-circle"></i>
                </button>
                <button class="btn btn-sm btn-secondary info-action action-2" 
                        data-bs-toggle="tooltip" 
                        data-bs-placement="left" 
                        title="Archive Conversation">
                    <i class="bi bi-archive"></i>
                </button>
                <button class="btn btn-sm btn-danger info-action action-3" 
                        data-bs-toggle="tooltip" 
                        data-bs-placement="left" 
                        title="Delete Conversation">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    </div>
`);

    attachInfoMenuListeners();
}

$('#infoActionMenu .action-1').off('click').on('click', () => {
    const selectedUserId = Number($('#selectedUserId').val());
    handleSeeProfile(selectedUserId);
});

function attachInfoMenuListeners() {
    const $trigger = $('#infoActionTrigger');
    const $menu = $('#infoActionMenu');

    $trigger.off('click').on('click', function () {
        $menu.toggleClass('show');
    });

    // ✅ Register button click listeners after rendering
    $menu.find('.action-1').off('click').on('click', () => {
        const selectedUserId = Number($('#selectedUserId').val());
        handleSeeProfile(selectedUserId);
    });

    // Close when clicking outside
    $(document).on('click', function (e) {
        if (!$(e.target).closest('#infoActionMenu, #infoActionTrigger').length) {
            $menu.removeClass('show');
        }
    });
}

function renderMessages(messages, receiverUser) {
    const $container = $('#chatMessages');
    const sessionUserId = Number($('#sessionUserId').val());
    $container.empty();

    let lastRenderedDate = null;
    const now = new Date();

    messages.forEach(msg => {
        const msgDateStr = new Date(msg.sentAt).toDateString();
        if (lastRenderedDate !== msgDateStr) {
            $container.append(`
                <div class="text-center my-3 small text-muted fw-semibold">
                    ${formatDateDivider(msg.sentAt)}
                </div>
            `);
            lastRenderedDate = msgDateStr;
        }

        const isRecent = now - new Date(msg.sentAt) < 2000;
        const isOwnRecentFile = msg.senderId === sessionUserId && msg.messageContent === '[file]' && isRecent;
        if (isOwnRecentFile)
            return;

        const isSender = msg.senderId === sessionUserId;
        $container.append(renderMessageBubble(msg, isSender, receiverUser));
    });
}


function renderMessageBubble(msg, isSender, receiverUser) {
    const isDeleted = msg.messageContent === 'deleted message';
    const alignment = isSender ? 'text-end' : 'text-start';
    const seen = isValidSeenAt(msg.seenAt);

    const isAttachmentOnly = !msg.messageContent || msg.messageContent === '[file]';

    const content = isDeleted
            ? `<div class="text-muted fst-italic">deleted message</div>`
            : (isAttachmentOnly ? '' : `<div>${escapeHTML(msg.messageContent || msg.content || '')}</div>`);

    const attachmentHtml = isDeleted ? '' : renderAttachmentOnly(msg);

    const statusHtml = isSender && !isDeleted
            ? `<div class="chat-status ${seen ? 'seen' : ''}">
            ${seen
            ? `<i class="bi bi-check2-circle"></i> Seen <small>${formatTimestamp(msg.seenAt)}</small>`
            : 'Delivered'}
        </div>`
            : '';

    const timestampHtml = `
        <div class="chat-timestamp">
            ${isDeleted && msg.deletedAt
            ? `Deleted at: ${formatTimestamp(msg.deletedAt)}`
            : formatTimestamp(msg.sentAt)}
        </div>
    `;

    // ✅ Add avatar above bubble for RECEIVER messages only
    const avatarHtml = !isSender && receiverUser
            ? `<div class="chat-avatar mb-1 text-start">
                <img src="/messages/media/profile/${receiverUser.authUserId}" class="rounded-circle shadow-sm" style="width: 40px; height: 40px;">
           </div>`
            : '';

    return `
        <div class="chat-message-wrapper ${alignment}" id="message-${msg.messageId}" data-id="${msg.messageId}">
            ${avatarHtml}
            <div class="chat-bubble">
                ${content}
                ${attachmentHtml}
                ${statusHtml}
            </div>
            ${timestampHtml}
        </div>
    `;
}

function renderAttachmentOnly(msg) {
    if (!msg.attachmentType)
        return '';

    const type = msg.attachmentType;
    const fileUrl = `/messages/attachment/${msg.messageId}`;
    const fileName = msg.attachmentName;
    const safeFileName = escapeHTML(fileName);
    const icon = getAttachmentIcon(type);

    if (type.startsWith('image/')) {
        return `
            <div class="attachment-image mt-2">
                <a href="${fileUrl}" target="_blank">
                    <img src="${fileUrl}" alt="${safeFileName}" class="img-fluid rounded shadow-sm" />
                </a>
            </div>
        `;
    }

    // Skip dangerous types
    if (
            type.includes('msdownload') ||
            type.includes('x-ms') ||
            type.includes('x-exe') ||
            type.includes('octet-stream')
            )
        return '';

    return `
        <div class="attachment-file mt-2">
        <a href="${fileUrl}" class="attachment-link d-inline-flex align-items-center text-decoration-none" download>
            <i class="${icon} fs-5 me-2"></i> ${safeFileName}
            </a>
        </div>
    `;
}

function getAttachmentIcon(type) {
    if (type.startsWith('image/'))
        return 'bi bi-file-image';
    if (type === 'application/pdf')
        return 'bi bi-file-pdf';
    if (type.includes('word'))
        return 'bi bi-file-word';
    if (type.includes('excel'))
        return 'bi bi-file-excel';
    if (type.includes('powerpoint'))
        return 'bi bi-file-ppt';
    if (type.startsWith('video/'))
        return 'bi bi-file-play';
    if (type.startsWith('audio/'))
        return 'bi bi-file-music';
    if (type === 'application/zip')
        return 'bi bi-file-zip';
    if (type.startsWith('text/'))
        return 'bi bi-file-text';
    return 'bi bi-file-earmark';
}

function escapeHTML(text) {
    if (!text)
        return '';
    return $('<div>').text(text).html();
}

function formatTimestamp(ts) {
    if (!ts)
        return '';
    const date = new Date(ts);
    if (isNaN(date))
        return '';
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}`;
}

function formatDateDivider(ts) {
    const date = new Date(ts);
    return date.toLocaleDateString(undefined, {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function isValidSeenAt(seenAt) {
    if (!seenAt || typeof seenAt !== 'string')
        return false;
    const time = Date.parse(seenAt);
    return !isNaN(time) && time > 0;
}

export function scrollChatToBottom(retry = 0) {
    const $chat = $('#chatMessages');
    if ($chat.length === 0 || !$chat[0]) {
        if (retry < 5) {
            setTimeout(() => scrollChatToBottom(retry + 1), 100);
        }
        return;
    }

    $chat.scrollTop($chat[0].scrollHeight);
}

function isScrolledToBottom($el) {
    return $el.scrollTop() + $el.innerHeight() >= $el[0].scrollHeight - 50;
}

function attemptMarkSeen(socket) {
    const sessionUserId = Number($('#sessionUserId').val());
    const selectedUserId = Number($('#selectedUserId').val());

    const $chatMessages = $('#chatMessages');

    if ($chatMessages.length === 0 || !$chatMessages[0])
        return;

    const isChatOpen = !$chatMessages.hasClass('d-none');
    const isAtBottom = $chatMessages.scrollTop() + $chatMessages.innerHeight() >= $chatMessages[0].scrollHeight - 50;

    // 🚫 Abort if not at bottom or chat is not visible
    if (!(selectedUserId && document.visibilityState === 'visible' && isChatOpen && isAtBottom))
        return;

    // 🧠 Make sure latest message is from the other user
    if (currentMessages.length === 0)
        return;

    const lastMessage = currentMessages[currentMessages.length - 1];

    if (lastMessage.senderId !== selectedUserId) {
        console.log('⏩ Not marking seen — last message was not from the other user.');
        return;
    }

    handleSeenConfirmation(selectedUserId, socket);
}
