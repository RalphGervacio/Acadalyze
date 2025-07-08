let lastRenderedIds = [];
const $notificationList = $('#notificationList');
const $notificationBadge = $('#notificationBadge');
const $fullNotificationContainer = $('#allNotifications');

function loadNotifications() {
    $.ajax({
        url: '/notifications/api',
        method: 'GET',
        dataType: 'json',
        success: function (res) {
            if (res.success) {
                const notifications = res.data || [];
                const latest = notifications.slice(0, 5);
                const newIds = latest.map(n => n.notificationId).join(',');
                if (newIds !== lastRenderedIds.join(',')) {
                    renderDropdownNotifications(latest);
                    lastRenderedIds = latest.map(n => n.notificationId);
                }

                if ($fullNotificationContainer.length) {
                    renderAllNotifications(notifications);
                }
            } else {
                showError(res.message);
            }
        },
        error: function () {
            showError('Failed to load notifications.');
        }
    });
    updateUnreadCount();
}

function renderDropdownNotifications(notifications) {
    $notificationList.empty();

    if (!notifications || notifications.length === 0) {
        $notificationList.append(`
            <p class="dropdown-item text-center text-muted mb-0">
                No notifications
            </p>
        `);
        $notificationBadge.addClass('d-none');
        return;
    }

    let hasUnread = false;

    notifications.forEach(notification => {
        const isUnread = !notification.readStatus;
        if (isUnread)
            hasUnread = true;

        const createdAt = new Date(notification.createdAt);
        const isValidDate = !isNaN(createdAt.getTime());
        const timeAgo = isValidDate ? timeSince(createdAt) : "just now";
        const isoTimestamp = isValidDate ? createdAt.toISOString() : "";
        const url = notification.url || '#';

        const $item = $(`
            <a href="#" class="dropdown-item px-3 py-2 rounded-2 d-flex flex-column gap-1 notification-item"
               data-id="${notification.notificationId}"
               data-url="${url}"
               data-read="${isUnread ? 'false' : 'true'}">
                <div class="notification-title">${notification.title}</div>
                <small class="notification-timestamp" data-timestamp="${isoTimestamp}">${timeAgo} ago</small>
            </a>
        `);

        $notificationList.append($item);
    });

    if (hasUnread) {
        $notificationBadge.removeClass('d-none');
    } else {
        $notificationBadge.addClass('d-none');
    }

    $notificationList.find('a.notification-item').on('click', function (e) {
        const $link = $(this);
        const notificationId = $link.data('id');
        const targetUrl = $link.data('url');

        if ($link.attr('data-read') === 'true')
            return;

        e.preventDefault();

        markAsRead(notificationId, () => {
            $link.attr('data-read', 'true');
            updateUnreadCount();
            if (targetUrl && targetUrl !== '#') {
                window.location.href = targetUrl;
            }
        });
    });
}

function renderAllNotifications(notifications) {
    $fullNotificationContainer.empty();
    if (!notifications || notifications.length === 0) {
        $fullNotificationContainer.html('<p class="text-center mb-0">No notifications found.</p>');
        return;
    }

    notifications.forEach(notification => {
        const readClass = notification.readStatus ? 'text-normal' : 'fw-bold';
        const createdAt = new Date(notification.createdAt);
        const isValidDate = !isNaN(createdAt.getTime());
        const timeAgo = isValidDate ? timeSince(createdAt) : "just now";
        const isoTimestamp = isValidDate ? createdAt.toISOString() : "";
        const url = notification.url || '#';

        let messageHtml = `<p class="mb-0">${notification.message}</p>`;

        try {
            const extra = notification.extraData ? JSON.parse(notification.extraData) : {};

            // 👇 SCHEDULE_PUBLISH details
            if (notification.type === 'SCHEDULE_PUBLISH') {
                const publishedBy = extra.publishedBy || 'Unknown';
                const publishedAt = extra.publishedAt ? new Date(extra.publishedAt).toLocaleString() : 'Unknown';
                messageHtml += `
                    <div class="mt-1 small text-success">
                        Published By: <strong>${publishedBy}</strong><br>
                        On: <em>${publishedAt}</em>
                    </div>`;
            }

            // 👇 COURSE-related messages
            const isCourseNotif = ['COURSE_ENROLLMENT', 'COURSE_ENROLLMENT_REMOVED'].includes(notification.type);
            if (isCourseNotif && extra.courseCode && extra.courseTitle) {
                const code = extra.courseCode;
                const title = extra.courseTitle;
                const desc = extra.courseDescription || '';
                const colorClass = notification.type === 'COURSE_ENROLLMENT' ? 'text-success' : 'text-danger';

                messageHtml = `
                    <div class="mb-1 ${colorClass}">
                        ${notification.type === 'COURSE_ENROLLMENT' ? 'You have been enrolled in the course:' : 'Your enrollment in the course has been removed:'}<br>
                        <strong>Course Code:</strong> ${code}<br>
                        <strong>Course Title:</strong> ${title}<br>
                        <strong>Description:</strong> ${desc}
                    </div>`;
            }
        } catch (e) {
            console.warn('Failed to parse extraData:', e);
        }

        const $item = $(`
            <div class="list-group-item bg-light border rounded mb-2 ${readClass}" data-id="${notification.notificationId}" data-url="${url}" style="cursor: pointer;">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h6 class="mb-1">${notification.title}</h6>
                        <small data-timestamp="${isoTimestamp}">${timeAgo} ago</small>
                    </div>
                    <div>
                        <button class="btn btn-sm btn-outline-danger btn-delete-notification" data-id="${notification.notificationId}" title="Delete">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
                ${messageHtml}
            </div>
        `);

        $fullNotificationContainer.append($item);
    });

    $fullNotificationContainer.find('.list-group-item').click(function (e) {
        const $link = $(this);
        const notificationId = $link.data('id');
        const targetUrl = $link.data('url') || '#';
        if (!$link.hasClass('fw-bold'))
            return;
        e.preventDefault();
        markAsRead(notificationId, () => {
            $link.removeClass('fw-bold').addClass('text-normal');
            if (targetUrl && targetUrl !== '#') {
                window.location.href = targetUrl;
            }
        });
    });
}

function markAsRead(notificationId, callback) {
    $.ajax({
        url: `/notifications/${notificationId}/read`,
        method: 'PATCH',
        success: function (res) {
            if (res.success && typeof callback === 'function')
                callback();
        },
        error: function () {
            showToast('Failed to mark as read.', 'error');
        }
    });
}

function markAllAsRead() {
    $.ajax({
        url: '/notifications/api',
        method: 'GET',
        dataType: 'json',
        success: function (res) {
            if (res.success) {
                const notifications = res.data || [];
                const unreadCount = notifications.filter(n => !n.readStatus).length;

                if (unreadCount === 0) {
                    showToast('No unread notifications to mark as read.', 'info');
                    loadNotifications();
                    return;
                }

                $.ajax({
                    url: '/notifications/mark-all-read',
                    method: 'PATCH',
                    success: function (res) {
                        if (res.success) {
                            showToast('All notifications marked as read.', 'success');
                            loadNotifications();
                        } else {
                            showToast('Failed to mark all notifications as read.', 'error');
                        }
                    },
                    error: function () {
                        showToast('Server error while marking notifications.', 'error');
                    }
                });

            } else {
                showToast(res.message || 'Failed to fetch notifications.', 'error');
            }
        },
        error: function () {
            showToast('Failed to check unread notifications.', 'error');
        }
    });
}

function deleteNotification(notificationId) {
    Swal.fire({
        icon: 'warning',
        title: 'Delete Notification',
        text: 'Are you sure you want to delete this notification?',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        background: '#2a2d3e',
        color: '#fff',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Delete',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            $.ajax({
                url: `/notifications/${notificationId}`,
                method: 'DELETE',
                success: function (res) {
                    if (res.success) {
                        showToast('Notification deleted.', 'success');
                        loadNotifications();
                    } else {
                        showToast('Failed to delete notification.', 'error');
                    }
                },
                error: function () {
                    showToast('Server error while deleting notification.', 'error');
                }
            });
        }
    });
}

function updateUnreadCount() {
    $.ajax({
        url: '/notifications/unread-count',
        method: 'GET',
        dataType: 'json',
        success: function (res) {
            const count = res.data || 0;
            if (count > 0) {
                $notificationBadge.text(count).removeClass('d-none');
            } else {
                $notificationBadge.addClass('d-none');
            }
        }
    });
}

function refreshTimeAgoLabels() {
    $('small[data-timestamp]').each(function () {
        const timestamp = $(this).data('timestamp');
        const timeAgo = timeSince(new Date(timestamp));
        $(this).text(`${timeAgo} ago`);
    });
}

function timeSince(date) {
    const seconds = Math.floor((new Date() - date) / 1000);
    if (isNaN(seconds))
        return "just now";
    let interval = Math.floor(seconds / 31536000);
    if (interval >= 1)
        return interval + " year" + (interval > 1 ? "s" : "");
    interval = Math.floor(seconds / 2592000);
    if (interval >= 1)
        return interval + " month" + (interval > 1 ? "s" : "");
    interval = Math.floor(seconds / 86400);
    if (interval >= 1)
        return interval + " day" + (interval > 1 ? "s" : "");
    interval = Math.floor(seconds / 3600);
    if (interval >= 1)
        return interval + " hour" + (interval > 1 ? "s" : "");
    interval = Math.floor(seconds / 60);
    if (interval >= 1)
        return interval + " minute" + (interval > 1 ? "s" : "");
    return Math.floor(seconds) + " second" + (seconds !== 1 ? "s" : "");
}

function showError(message) {
    $notificationList.html(`<p class="dropdown-item text-center text-danger mb-0">${message}</p>`);
    $notificationBadge.hide();
}

const refreshNotifications = () => loadNotifications();

$(document).ready(function () {
    loadNotifications();
    setInterval(loadNotifications, 30000);
    setInterval(refreshTimeAgoLabels, 60000);
    $(document).on('click', '.btn-delete-notification', function (e) {
        e.stopPropagation();
        deleteNotification($(this).data('id'));
    });
});
