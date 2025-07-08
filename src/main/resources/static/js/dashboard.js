let isCalendarView = false;
function loadPublishedSchedules() {
    $.get('/dashboard/published-schedules', function (res) {
        if (!res.success) {
            showToast("Failed to load published schedules.", "error");
            return;
        }

        const authRoleId = res.auth_role_id;
        const isRestricted = authRoleId === 3 || authRoleId === 4;
        if ($.fn.DataTable.isDataTable('#dashboardScheduleTable')) {
            $('#dashboardScheduleTable').DataTable().destroy();
        }

        $('#removeSelectedSchedulesBtn').hide();
        const table = $('#dashboardScheduleTable').DataTable({
            destroy: true,
            bAutoWidth: false,
            processing: true,
            deferRender: true,
            responsive: true,
            paging: true,
            pageLength: 10,
            ordering: true,
            order: [[6, 'desc']],
            data: res.data,
            columns: [
                {
                    data: function (row) {
                        return `<input type="checkbox" class="schedule-checkbox ${isRestricted ? 'd-none' : ''}" data-id="${row.scheduleId}">`;
                    },
                    orderable: false,
                    className: 'text-center',
                    width: '1%',
                    title: ''
                },
                {
                    data: function (row) {
                        return `
                            <strong>${row.subjectCode}</strong><br>
                            <small class="text-muted">${row.subjectName}</small>
                        `;
                    },
                    className: 'text-start',
                    title: 'Subject'
                },
                {
                    data: function (row) {
                        return `
                            ${row.dayOfWeek}<br>
                            <small class="text-muted">${row.startTimeDisplay} - ${row.endTimeDisplay}</small>
                        `;
                    },
                    className: 'text-start',
                    title: 'Schedule'
                },
                {data: 'room', className: 'text-start', title: 'Room'},
                {data: 'section', className: 'text-start', title: 'Section'},
                {
                    data: function (row) {
                        return row.instructorFullName || '-';
                    },
                    className: 'text-start',
                    title: 'Instructor'
                },
                {
                    data: function (row) {
                        return `<small>${row.createdAt}</small>`;
                    },
                    className: 'text-start',
                    title: 'Published'
                }
            ],
            drawCallback: function () {
                if (!isRestricted) {
                    $('.schedule-checkbox').off('change').on('change', function () {
                        const anyChecked = $('.schedule-checkbox:checked').length > 0;
                        $('#removeSelectedSchedulesBtn').toggle(anyChecked);
                    });
                }
            }
        });
    });
}

function loadCalendarSchedules() {
    $.get('/dashboard/published-schedules', function (res) {
        if (!res.success) {
            showToast("Failed to load published schedules.", "error");
            return;
        }

        const events = res.data.map(item => ({
                id: item.scheduleId,
                title: `${item.subjectCode} - ${item.section}`,
                start: item.startDateTime,
                end: item.endDateTime,
                extendedProps: {
                    instructor: item.instructorFullName || '-',
                    room: item.room || '-',
                    section: item.section || '-',
                    subjectName: item.subjectName || '-'
                }
            }));

        const calendarEl = document.getElementById('publishedCalendar');
        calendarEl.innerHTML = "";

        const isMobile = window.innerWidth < 768;

        const calendar = new FullCalendar.Calendar(calendarEl, {
            initialView: isMobile ? 'listWeek' : 'dayGridMonth',
            themeSystem: 'bootstrap5',
            height: 'auto',
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,listWeek'
            },
            events: events,
            eventTimeFormat: {
                hour: 'numeric',
                minute: '2-digit',
                hour12: true
            },
            eventClick: function (info) {
                const props = info.event.extendedProps;
                const isDark = document.documentElement.getAttribute("data-bs-theme") === "dark";

                Swal.fire({
                    title: '',
                    html: `
                        <div class="text-start">
                            <div class="d-flex align-items-center flex-wrap mb-2 fw-bold" style="font-size: 18px;">
                                <i class="fa fa-calendar-alt text-primary me-2"></i>
                                <span class="fw-semibold">${info.event.title}</span>
                            </div>
                            <hr class="my-1"/>
                            <div style="font-size: 15px;">
                                <p class="mb-1"><i class="fa fa-book-open me-2 text-muted"></i><strong>Subject:</strong> ${props.subjectName}</p>
                                <p class="mb-1"><i class="fa fa-users me-2 text-muted"></i><strong>Section:</strong> <span class="badge bg-secondary">${props.section}</span></p>
                                <p class="mb-1"><i class="fa fa-door-open me-2 text-muted"></i><strong>Room:</strong> <span class="badge bg-dark">${props.room}</span></p>
                                <p class="mb-2"><i class="fa fa-chalkboard-teacher me-2 text-muted"></i><strong>Instructor:</strong> ${props.instructor}</p>
                                <hr class="my-2"/>
                                <p class="mb-0"><i class="fa fa-clock me-2 text-muted"></i><strong>Time:</strong> ${formatTimeCalendar(info.event.start)} - ${formatTimeCalendar(info.event.end)}</p>
                            </div>
                        </div>
                    `,
                    background: isDark ? '#1f1f1f' : '#ffffff',
                    color: isDark ? '#e2e8f0' : '#2a2a2a',
                    confirmButtonText: 'Okay',
                    customClass: {
                        popup: 'border border-secondary rounded-3 shadow-sm px-3 pt-2 pb-2 text-start',
                        confirmButton: 'btn btn-sm btn-primary px-4 rounded-pill'
                    },
                    buttonsStyling: false,
                    width: isMobile ? '90%' : '600px'
                });
            },
            eventDidMount: function (info) {
                const props = info.event.extendedProps;
                const tooltipText = `
                Subject: ${props.subjectName}
                Section: ${props.section}
                Room: ${props.room}
                Instructor: ${props.instructor}
                Time: ${formatTimeCalendar(info.event.start)} - ${formatTimeCalendar(info.event.end)}`;
                info.el.setAttribute('title', tooltipText.trim());
            }
        });

        calendar.render();
    });
}

function formatTimeCalendar(dateObj) {
    const date = new Date(dateObj);
    if (isNaN(date))
        return '';
    const hours = date.getHours();
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const ampm = hours >= 12 ? 'PM' : 'AM';
    const hour12 = hours % 12 || 12;
    return `${hour12}:${minutes} ${ampm}`;
}

function removeSelectedSchedules() {
    const selectedIds = [];
    $('.schedule-checkbox:checked').each(function () {
        selectedIds.push($(this).data('id'));
    });
    if (selectedIds.length === 0) {
        showToast("Please select at least one schedule to unpublish.", "error");
        return;
    }

    Swal.fire({
        icon: 'warning',
        title: 'Are you sure?',
        text: `This will unpublish ${selectedIds.length} selected schedule${selectedIds.length > 1 ? 's' : ''}.`,
        showCancelButton: true,
        confirmButtonText: 'Yes, unpublish',
        cancelButtonText: 'Cancel',
        background: '#343a40',
        color: '#f8f9fa',
        iconColor: '#dc3545',
        customClass: {
            popup: 'border border-secondary rounded-3',
            confirmButton: 'btn btn-sm btn-danger me-2',
            cancelButton: 'btn btn-sm btn-outline-light'
        },
        buttonsStyling: false
    }).then(result => {
        if (!result.isConfirmed)
            return;
        $.ajax({
            url: '/dashboard/unpublish-multiple',
            method: 'PATCH',
            contentType: 'application/json',
            data: JSON.stringify({ids: selectedIds}),
            xhrFields: {withCredentials: true},
            success: function (response) {
                if (response.success) {
                    showToast("Selected schedule(s) unpublished successfully.");
                    loadPublishedSchedules();
                } else {
                    showToast(response.message || "Failed to unpublish the selected schedule(s).", "error");
                }
            },
            error: function () {
                showToast("An error occurred while unpublishing the schedule(s).", "error");
            }
        });
    });
}

function triggerUnpublishButton() {
    $('.schedule-checkbox').prop('checked', true);
    removeSelectedSchedules();
}

function toggleView() {
    isCalendarView = !isCalendarView;
    if (isCalendarView) {
        $('#publishedScheduleContainer').hide();
        $('#calendarContainer').show();
        $('#toggleViewBtn').html('<i class="fa fa-table"></i> Table View');
        loadCalendarSchedules();
    } else {
        $('#calendarContainer').hide();
        $('#publishedScheduleContainer').show();
        $('#toggleViewBtn').html('<i class="fa fa-calendar-alt"></i> Calendar View');
        loadPublishedSchedules();
    }
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

$(document).ready(function () {
    loadPublishedSchedules();
});
