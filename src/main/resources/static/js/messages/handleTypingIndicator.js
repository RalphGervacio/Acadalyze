// ✅ Typing Indicator Logic
let typingTimeout = null;
let lastTypedUserId = null;

import { scrollChatToBottom } from './handleConversationWith.js';

export function initTypingIndicator(socket) {
    const $input = $('#messageInput');
    const senderId = Number($('#sessionUserId').val());

    $input.on('input', () => {
        const receiverId = Number($('#selectedUserId').val());
        if (!receiverId || socket.readyState !== 1)
            return;

        if (lastTypedUserId !== receiverId) {
            socket.send(JSON.stringify({
                type: 'TYPING',
                senderId,
                receiverId
            }));
            lastTypedUserId = receiverId;
        }

        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => {
            socket.send(JSON.stringify({
                type: 'STOP_TYPING',
                senderId,
                receiverId
            }));
            lastTypedUserId = null;
        }, 2000);
    });
}

export function showTypingIndicator(senderId) {
    const currentChatUserId = Number($('#selectedUserId').val());
    if (senderId !== currentChatUserId)
        return;

    // Only render if not already present
    if (!$('#typingIndicator').length) {
        const avatarUrl = `/messages/media/profile/${senderId}`;
        const typingHtml = `
            <div id="typingIndicator" class="d-flex align-items-center gap-2 ms-2 mb-2">
                <img src="${avatarUrl}" width="28" height="28" class="rounded-circle shadow-sm" alt="typing avatar">
                <div class="typing-dots breathing">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                </div>
            </div>
        `;
        $('#chatMessages').append(typingHtml);
        scrollChatToBottom();
    }
}

export function hideTypingIndicator(senderId) {
    const currentChatUserId = Number($('#selectedUserId').val());
    if (senderId !== currentChatUserId)
        return;

    $('#typingIndicator').remove();
}
