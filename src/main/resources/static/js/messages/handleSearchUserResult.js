// ✅ WebSocket-based User Search Logic
// File: /js/messages/handleSearchUser.js
import { formatMessagePreview } from './messages.js';
let searchTimeout = null;
let socket = null;
const userId = Number($('#sessionUserId').val());
const chatHeadMoveLocks = new Set();

export function initWebSocketUserSearch(socket, openCallback) {
    const $searchInput = $('#chatSearchInput');
    const $contactList = $('#contactList');

    $searchInput.on('input', function () {
        const query = $(this).val().trim();
        clearTimeout(searchTimeout);

        if (query.length < 2) {
            loadChatHeads(openCallback, socket);
            return;
        }

        searchTimeout = setTimeout(() => {
            if (socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({
                    type: 'SEARCH_USER',
                    query
                }));
            }
        }, 300);
    });

    $searchInput.on('blur', function () {
        setTimeout(() => {
            const query = $(this).val().trim();
            if (query.length === 0) {
                $(this).val('');
                loadChatHeads(openCallback, socket);
            }
        }, 100);
    });

    // 🧠 Bind click handler once after rendering all chat heads
    $contactList.off('click', '.user-result').on('click', '.user-result', function () {
        const selectedUserId = $(this).data('id');
        console.log('✅ Clicked chat head:', selectedUserId);

        // 🔁 Highlight the clicked chat head
        $contactList.find('.chat-head').removeClass('active-chat');
        $(this).addClass('active-chat');

        $('#chatSearchInput').val('').blur();

        if (typeof openCallback === 'function') {
            openCallback(selectedUserId, socket);
        }
    });
}

export function handleSearchUserResult(msg, openCallback) {
    const {results, query} = msg;
    const $contactList = $('#contactList').empty();

    if (!results || results.length === 0) {
        $contactList.html(`<div class="text-muted px-3">No users found for "${query}"</div>`);
        return;
    }

    results.forEach(user => {
        const avatarUrl = user.avatar || `/media/profile/${user.userId}`;
        const html = `
            <div class="chat-head d-flex align-items-center gap-2 py-2 px-3 border-bottom user-result"
                 data-id="${user.userId}" style="cursor:pointer;">
                <img src="${avatarUrl}" class="rounded-circle chat-head-avatar" width="36" height="36">
                <div class="chat-head-details flex-grow-1">
                    <div class="chat-head-name fw-semibold">${user.name || 'Unnamed'}</div>
                    <div class="chat-head-status small text-muted">${user.status || ''}</div>
                </div>
            </div>
        `;
        $contactList.append(html);
    });

    // Delegated event binding to avoid multiple handlers
    $contactList.off('click', '.user-result').on('click', '.user-result', function () {
        const selectedUserId = $(this).data('id');
        console.log('✅ Clicked user-result with ID:', selectedUserId);

        // 🔁 Highlight the clicked chat head
        $contactList.find('.chat-head').removeClass('active-chat');
        $(this).addClass('active-chat');

        $('#chatSearchInput').val('').blur();

        if (typeof openCallback === 'function') {
            openCallback(selectedUserId, socket);
        } else {
            console.warn('⚠️ openCallback is not a function');
        }
    });
}

export function removeUnreadBadge(userId) {
    const $chatHead = $(`.chat-head[data-id="${userId}"]`);
    $chatHead.find('.badge-on-avatar, .badge-in-details').fadeOut(200, function () {
        $(this).remove();
    });
}

export function moveChatHeadToTop(userId) {
    const $contactList = $('#contactList');
    const $chatHead = $contactList.find(`.chat-head[data-id="${userId}"]`);

    if ($chatHead.length === 0)
        return;
    if ($contactList.children().first().is($chatHead))
        return;
    if (chatHeadMoveLocks.has(userId))
        return;

    chatHeadMoveLocks.add(userId);

    // Step 1: Animate current chat head out to the left
    $chatHead.addClass('moving-head').css({
        transition: 'transform 0.2s ease, opacity 0.2s ease',
        transform: 'translateX(-100%)',
        opacity: 0,
    });

    setTimeout(() => {
        const $clone = $chatHead.clone(true, true); // Copy content
        $chatHead.remove(); // Remove original

        // Step 2: Insert at top, hidden initially, then slide in
        $clone
                .css({
                    transform: 'translateX(-100%)',
                    opacity: 0,
                    transition: 'transform 0.3s ease, opacity 0.3s ease'
                })
                .addClass('moving-head');

        $contactList.prepend($clone);

        // Force reflow to trigger transition
        $clone[0].offsetHeight;

        // Step 3: Animate in (left to right)
        $clone.css({
            transform: 'translateX(0)',
            opacity: 1
        });

        setTimeout(() => {
            $clone.removeClass('moving-head').css('transform', '');
            chatHeadMoveLocks.delete(userId);
        }, 300);
    }, 200); // Wait for the initial fade out before cloning in
}

export function loadChatHeads(openCallback, socket) {
    const $contactList = $('#contactList');

    fetchData('/messages/chat-heads')
            .then(json => {
                if (!json.success || !json.data) {
                    $contactList.html('<div class="text-muted px-3">Failed to load conversations</div>');
                    return;
                }

                const chatHeads = json.data;

                if (chatHeads.length === 0) {
                    $contactList.html('<div class="text-muted px-3">No conversations yet</div>');
                    return;
                }

                chatHeads.forEach(user => {
                    const avatarUrl = `/messages/media/profile/${user.authUserId}`;
                    const previewText = formatMessagePreview(user.lastMessage, user.lastSenderId, user.attachmentType);
                    const timeFormatted = user.lastMessageTimeFormatted || '';

                    const badgeOnAvatar = user.unreadCount > 0
                            ? `<div class="badge-on-avatar">${user.unreadCount}</div>`
                            : '';

                    const badgeInDetails = user.unreadCount > 0
                            ? `<div class="badge-in-details">${user.unreadCount}</div>`
                            : '';

                    const newHtml = `
                    <div class="chat-head d-flex align-items-center gap-2 py-2 px-3 border-bottom user-result"
                         data-id="${user.authUserId}" style="cursor:pointer;">
                        <div class="chat-head-avatar position-relative">
                            <img src="${avatarUrl}" class="rounded-circle" width="36" height="36">
                            ${badgeOnAvatar}
                        </div>
                        <div class="chat-head-details flex-grow-1 d-flex align-items-center">
                            <div class="flex-grow-1">
                                <div class="chat-head-name fw-semibold">${user.fullName || 'Unnamed'}</div>
                                <div class="chat-head-status small text-muted">
                                    <div>${previewText}</div>
                                    <div class="text-end"><small class="text-muted">${formatTimeOnly(timeFormatted)}</small></div>
                                </div>
                            </div>
                            ${badgeInDetails}
                        </div>
                    </div>
                `;

                    const $chatHead = $contactList.find(`.chat-head[data-id="${user.authUserId}"]`);
                    if ($chatHead.length) {
                        $chatHead.replaceWith(newHtml);
                    } else {
                        const $existingChatHead = $contactList.find(`.chat-head[data-id="${user.authUserId}"]`);
                        if ($existingChatHead.length) {
                            $existingChatHead.replaceWith(newHtml);
                        } else {
                            $contactList.append(newHtml);
                        }
                    }
                });

                const selectedUserId = Number($('#selectedUserId').val());
                if (selectedUserId) {
                    $('#contactList').find(`.chat-head[data-id="${selectedUserId}"]`).addClass('active-chat');
                }

            })
            .catch(err => {
                console.error('❌ Failed to fetch chat heads:', err);
                $contactList.html('<div class="text-muted px-3">Error loading conversations</div>');
            });
}

function formatTimeOnly(dateTimeString) {
    if (!dateTimeString)
        return '';
    return dateTimeString.slice(-8);
}

export function updateChatHeadBadge(userId, count) {
    const $chatHead = $(`.chat-head[data-id="${userId}"]`);
    const $avatar = $chatHead.find('.chat-head-avatar');
    const $details = $chatHead.find('.chat-head-details');
    const $existingBadgeOnAvatar = $avatar.find('.badge-on-avatar');
    const $existingBadgeInDetails = $details.find('.badge-in-details');

    if (count > 0) {
        // Update or create badge on avatar
        if ($existingBadgeOnAvatar.length) {
            $existingBadgeOnAvatar.text(count);
        } else {
            const badgeOnAvatar = $(`<div class="badge-on-avatar">${count}</div>`);
            $avatar.append(badgeOnAvatar.hide().fadeIn(200));
        }

        // Update or create badge in details
        if ($existingBadgeInDetails.length) {
            $existingBadgeInDetails.text(count);
        } else {
            const badgeInDetails = $(`<div class="badge-in-details">${count}</div>`);
            $details.append(badgeInDetails.hide().fadeIn(200));
        }
    } else {
        // Remove both badges
        $existingBadgeOnAvatar.fadeOut(200, function () {
            $(this).remove();
        });
        $existingBadgeInDetails.fadeOut(200, function () {
            $(this).remove();
        });
    }
}

export function updateChatHeadLastMessage(userId, lastMessage, timestamp) {
    const $chatHead = $(`.chat-head[data-id="${userId}"]`);
    let timeFormatted = '';

    if (timestamp) {
        const date = new Date(timestamp);
        timeFormatted = date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit', hour12: true});
    }

    const html = `
        <div>${lastMessage}</div>
        <div class="text-end"><small class="text-muted">${timeFormatted}</small></div>
    `;
    $chatHead.find('.chat-head-status').html(html);
}