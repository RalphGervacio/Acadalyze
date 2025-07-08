// FETCHING DATATABLE
function fetchData(url) {
    return $.ajax({
        url: url,
        method: 'GET',
        dataType: 'json'
    });
}

async function getAllSchedules(courseId = null) {
    try {
        let url = '/admin/schedules/list';
        if (courseId) {
            url += `?courseId=${courseId}`;
        }

        const response = await fetchData(url);
        if (response.success) {
            initScheduleDataTable(response.data);
        } else {
            showToast(response.message || 'Failed to load schedules.', 'error');
        }
    } catch (err) {
        console.error('Error fetching schedules:', err);
        showToast('Unable to load schedules.', 'error');
}
}

const initScheduleDataTable = (schedules) => {
    $('#dt_schedules').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[4, 'asc']],
        deferRender: true,
        responsive: true,
        data: schedules,
        columns: [
            {data: 'scheduleId', visible: false},
            {data: 'courseDisplay', title: 'Course'},
            {data: 'subjectCode', title: 'Subject Code'},
            {data: 'subjectName', title: 'Subject Name'},
            {data: 'dayOfWeek', title: 'Day'},
            {
                data: function (row) {
                    return `${row.startTimeDisplay} - ${row.endTimeDisplay}`;
                },
                title: 'Time'
            },
            {data: 'room', title: 'Room'},
            {data: 'section', title: 'Section'},
            {data: 'instructorFullName', title: 'Instructor'},
            {
                data: function (row) {
                    return `
                        <button class="btn btn-secondary btn-sm me-1" onclick="editScheduleModal(${row.scheduleId})">
                            <i class="fa-solid fa-pencil"></i>
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="confirmDeleteSchedule(${row.scheduleId})">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    `;
                },
                orderable: false,
                className: 'text-center',
                width: "5%",
                title: 'Actions'
            }
        ]
    });
};

// POST DATA
function postData(url, data = {}, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (response) {
            callbacks.forEach(callback => callback());
            return response;
        }
    });
}

// ADD SCHEDULE
function addSchedule(data) {
    console.log("🛠️ Submitting schedule data via XHR:", data);

    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/admin/schedules/add", true);
    xhr.setRequestHeader("Content-Type", "application/json");

    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {

            if (xhr.status === 200) {
                try {
                    const response = JSON.parse(xhr.responseText);

                    if (response.success) {
                        $('#scheduleAddModal').modal('hide');
                        resetAddScheduleForm();
                        getAllSchedules();
                        showToast("Schedule added successfully.");
                    } else {
                        showToast(response.message || "Failed to add schedule.", "error");
                    }
                } catch (e) {
                    showToast("Invalid server response.", "error");
                }
            } else {
                showToast("Failed to send schedule data. Try again.", "error");
            }
        }
    };

    xhr.onerror = function () {
        showToast("Network error while adding schedule.", "error");
    };

    xhr.send(JSON.stringify(data));
}

// HANDLE ADD FORM SUBMIT
function handleAddScheduleFormSubmission() {
    $('#addScheduleForm').submit(async function (e) {
        e.preventDefault();

        const data = {
            courseId: $('#addScheduleCourse').val(),
            subjectId: $('#addScheduleSubject').val(),
            instructorId: $('#addScheduleInstructor').val(),
            dayOfWeek: $('#addScheduleDay').val(),
            startTime: $('#addStartTime').val(),
            endTime: $('#addEndTime').val(),
            room: $('#addRoom').val().trim(),
            section: $('#addSection').val().trim()
        };

        let isValid = true;
        $('#addScheduleForm .form-control, #addScheduleForm .form-select').removeClass('is-invalid');
        $('#addScheduleForm .invalid-feedback').text('');

        if (!data.courseId) {
            $('#addScheduleCourse').addClass('is-invalid').siblings('.invalid-feedback').text('Course is required.');
            isValid = false;
        }
        if (!data.subjectId) {
            $('#addScheduleSubject').addClass('is-invalid').siblings('.invalid-feedback').text('Subject is required.');
            isValid = false;
        }
        if (!data.instructorId) {
            $('#addScheduleInstructor').addClass('is-invalid').siblings('.invalid-feedback').text('Instructor is required.');
            isValid = false;
        }
        if (!data.dayOfWeek) {
            $('#addScheduleDay').addClass('is-invalid').siblings('.invalid-feedback').text('Day is required.');
            isValid = false;
        }
        if (!data.startTime) {
            $('#addStartTime').addClass('is-invalid').siblings('.invalid-feedback').text('Start time is required.');
            isValid = false;
        }
        if (!data.endTime) {
            $('#addEndTime').addClass('is-invalid').siblings('.invalid-feedback').text('End time is required.');
            isValid = false;
        }
        if (!data.room) {
            $('#addRoom').addClass('is-invalid').siblings('.invalid-feedback').text('Room is required.');
            isValid = false;
        }
        if (!data.section) {
            $('#addSection').addClass('is-invalid').siblings('.invalid-feedback').text('Section is required.');
            isValid = false;
        }

        if (!isValid)
            return;

        const confirm = await Swal.fire({
            title: 'Add Schedule?',
            text: 'Are you sure you want to add this schedule?',
            icon: 'question',
            background: '#1f1f1f',
            color: '#ffffff',
            iconColor: '#17a2b8',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, add',
            cancelButtonText: 'Cancel'
        });

        if (!confirm.isConfirmed)
            return;

        await addSchedule(data);
    });
}

// EDIT
async function editScheduleModal(scheduleId) {
    try {
        const response = await fetchData(`/admin/schedules/find/${scheduleId}`);
        if (response.success && response.data) {
            const sched = response.data;

            $('#editScheduleId').val(sched.scheduleId);
            $('#editScheduleDay').val(sched.dayOfWeek);
            $('#editStartTime').val(sched.startTimeRaw);
            $('#editEndTime').val(sched.endTimeRaw);
            $('#editRoom').val(sched.room);
            $('#editSection').val(sched.section);

            $('#scheduleEditModal').modal('show');
        } else {
            showToast(response.message || 'Failed to load schedule info.', 'error');
        }
    } catch (error) {
        showToast('Could not load schedule details.', 'error');
    }
}

function handleEditScheduleFormSubmission() {
    $('#editScheduleForm').submit(async function (e) {
        e.preventDefault();

        const data = {
            scheduleId: $('#editScheduleId').val(),
            dayOfWeek: $('#editScheduleDay').val(),
            startTime: $('#editStartTime').val(),
            endTime: $('#editEndTime').val(),
            room: $('#editRoom').val().trim(),
            section: $('#editSection').val().trim()
        };

        console.table(data);

        let isValid = true;
        $('#editScheduleForm .form-control, #editScheduleForm .form-select').removeClass('is-invalid');
        $('#editScheduleForm .invalid-feedback').text('');

        if (!data.dayOfWeek) {
            $('#editScheduleDay').addClass('is-invalid').siblings('.invalid-feedback').text('Day is required.');
            isValid = false;
        }
        if (!data.startTime) {
            $('#editStartTime').addClass('is-invalid').siblings('.invalid-feedback').text('Start time is required.');
            isValid = false;
        }
        if (!data.endTime) {
            $('#editEndTime').addClass('is-invalid').siblings('.invalid-feedback').text('End time is required.');
            isValid = false;
        }
        if (!data.room) {
            $('#editRoom').addClass('is-invalid').siblings('.invalid-feedback').text('Room is required.');
            isValid = false;
        }

        if (!isValid) {
            console.warn("Validation failed. Aborting submission.");
            return;
        }

        const confirm = await Swal.fire({
            title: 'Update Schedule?',
            text: 'Are you sure you want to update this schedule?',
            icon: 'question',
            background: '#1f1f1f',
            color: '#ffffff',
            iconColor: '#ffc107',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, update',
            cancelButtonText: 'Cancel'
        });

        if (!confirm.isConfirmed) {
            console.log("Update cancelled by user.");
            return;
        }

        $.ajax({
            url: '/admin/schedules/update',
            method: 'PATCH',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function (res) {
                console.log("Server response:", res);

                if (res.success) {
                    $('#scheduleEditModal').modal('hide');
                    getAllSchedules();
                    showToast("Schedule updated successfully.");
                } else {
                    console.warn("Update failed:", res.message);
                    showToast(res.message || "Failed to update schedule.", "error");
                }
            },
            error: function (xhr, status, error) {
                console.error("XHR error:", status, error);
                console.error("Full response text:", xhr.responseText);
                showToast("Error occurred while updating schedule.", "error");
            }
        });
    });
}

// DELETE
function confirmDeleteSchedule(scheduleId) {
    Swal.fire({
        title: 'Delete Schedule?',
        text: 'Are you sure you want to delete this schedule?',
        icon: 'warning',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#dc3545',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, delete',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteSchedule(scheduleId);
        }
    });
}

function deleteSchedule(scheduleId) {
    console.log("Deleting schedule ID:", scheduleId);

    const xhr = new XMLHttpRequest();
    xhr.open("DELETE", `/admin/schedules/delete?scheduleId=${scheduleId}`, true);

    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            console.log("Delete response:", xhr.status, xhr.responseText);

            if (xhr.status === 200) {
                try {
                    const response = JSON.parse(xhr.responseText);
                    if (response.success) {
                        showToast("Schedule deleted successfully.");
                        getAllSchedules(); // Refresh table
                    } else {
                        showToast(response.message || "Failed to delete schedule.", "error");
                    }
                } catch (e) {
                    console.error("JSON parse error:", e);
                    showToast("Invalid server response on delete.", "error");
                }
            } else {
                showToast("Failed to delete schedule. Try again.", "error");
            }
        }
    };

    xhr.onerror = function () {
        console.error("Network error while deleting schedule.");
        showToast("Network error while deleting.", "error");
    };

    xhr.send();
}

// DROPDOWNS
async function loadCourseDropdown() {
    try {
        const response = await fetchData('/admin/schedules/courses');
        const $select = $('#addScheduleCourse');
        const currentValue = $select.val();

        $select.empty().append(`<option value="">-- Select Course --</option>`);

        if (response.success && response.data.length) {
            response.data.forEach(course => {
                $select.append(`<option value="${course.courseId}">${course.courseCode} - ${course.courseTitle}</option>`);
            });

            if (currentValue)
                $select.val(currentValue).trigger('change');
        } else {
            $select.append(`<option value="">No courses found</option>`);
        }
    } catch (error) {
        console.error('Failed to load courses:', error);
    }
}

async function loadFilteredSubjectDropdown(courseId) {
    const $subjectSelect = $('#addScheduleSubject');

    $subjectSelect.prop('disabled', true);
    $subjectSelect.empty().append(`<option value="">-- Select Subject --</option>`);

    if (!courseId) {
        $subjectSelect.trigger('change.select2');
        return;
    }

    try {
        const response = await fetchData(`/admin/schedules/subjects/by-course?courseId=${courseId}`);

        if (response.success && response.data.length) {
            response.data.forEach(subject => {
                $subjectSelect.append(
                        `<option value="${subject.subjectId}">${subject.subjectCode} - ${subject.subjectName}</option>`
                        );
            });
        } else {
            $subjectSelect.append(`<option value="">No subjects found</option>`);
        }

        setTimeout(() => {
            $subjectSelect.val('').trigger('change.select2');
            $('.select2-selection__rendered').css('color', '');
        }, 0);
    } catch (error) {
        console.error('Failed to load filtered subjects:', error);
    } finally {
        $subjectSelect.prop('disabled', false);
    }
}

async function loadInstructorDropdown() {
    try {
        const response = await fetchData('/admin/schedules/instructors');
        const $select = $('#addScheduleInstructor');
        $select.empty().append(`<option value="">-- Select Instructor --</option>`);

        if (response.success && response.data.length) {
            response.data.forEach(inst => {
                $select.append(`<option value="${inst.authUserId}">${inst.fullName}</option>`);
            });
        } else {
            $select.append(`<option value="">No instructors found</option>`);
        }
    } catch (error) {
        console.error('Failed to load instructors:', error);
    }
}

async function loadFilterCourseDropdown() {
    try {
        const response = await fetchData('/admin/schedules/courses');
        const $filterSelect = $('#filterByCourse');

        $filterSelect.empty().append(`<option value="">Filter by Course</option>`);

        if (response.success && response.data.length) {
            response.data.forEach(course => {
                $filterSelect.append(
                        `<option value="${course.courseId}">${course.courseCode} - ${course.courseTitle}</option>`
                        );
            });
        } else {
            $filterSelect.append(`<option value="">No courses found</option>`);
        }
    } catch (err) {
        console.error('Error loading course filter:', err);
        showToast('Failed to load filter courses.', 'error');
    }
}

function initScheduleModal() {
    $('#scheduleAddModal').on('shown.bs.modal', function () {
        const $courseSelect = $('#addScheduleCourse');
        const $subjectSelect = $('#addScheduleSubject');
        const $instructorSelect = $('#addScheduleInstructor');

        // Clear subject dropdown initially
        $subjectSelect.empty().append('<option value="">-- Select Subject --</option>');

        // Load dropdown data
        loadCourseDropdown();
        loadInstructorDropdown();

        // Filter subject list when course changes
        $courseSelect.off('change').on('change', function () {
            const courseId = $(this).val();
            loadFilteredSubjectDropdown(courseId);
        });
    });
}

function initScheduleFilters() {
    $('#filterByCourse').on('change', function () {
        const selected = $(this).val();
        if (selected) {
            $('#clearCourseFilterBtn').removeClass('d-none');
        } else {
            $('#clearCourseFilterBtn').addClass('d-none');
        }
        getAllSchedules(selected || null);
        updateClearButtonTheme();
    });
}

function updateClearButtonTheme() {
    const theme = document.documentElement.getAttribute('data-bs-theme');
    const $btn = $('#clearCourseFilterBtn');

    $btn.removeClass('btn-theme-light btn-theme-dark');

    if (theme === 'dark') {
        $btn.addClass('btn-theme-dark');
    } else {
        $btn.addClass('btn-theme-light');
    }
}

function observeThemeChanges() {
    const observer = new MutationObserver((mutations) => {
        for (const mutation of mutations) {
            if (
                    mutation.type === 'attributes' &&
                    mutation.attributeName === 'data-bs-theme'
                    ) {
                updateClearButtonTheme();
            }
        }
    });

    observer.observe(document.documentElement, {
        attributes: true
    });
}

// PUBLISHE TO DAHBOARD BUTTON
function publishSchedules() {
    $.get('/admin/schedules/unpublished', function (response) {
        if (!response.success || !response.data || response.data.length === 0) {
            showToast(response.message || 'No unpublished schedules to publish.', 'error');
            return;
        }

        const scheduleList = response.data.map(s =>
                `<li><strong>${s.subjectCode}</strong> - ${s.subjectName} (${s.dayOfWeek}, ${s.startTimeDisplay} - ${s.endTimeDisplay})</li>`
        ).join('');

        Swal.fire({
            icon: 'question',
            title: 'Publish These Schedules?',
            html: `
                <p>This will notify all users about the latest schedule updates:</p>
                <ul class="text-start" style="max-height: 200px; overflow-y: auto;">${scheduleList}</ul>
            `,
            width: '700px',
            showCancelButton: true,
            confirmButtonText: 'Yes, publish it!',
            cancelButtonText: 'Cancel',
            background: '#343a40',
            color: '#f8f9fa',
            iconColor: '#ffc107',
            customClass: {
                popup: 'border border-secondary rounded-3',
                confirmButton: 'btn btn-sm btn-success me-2',
                cancelButton: 'btn btn-sm btn-outline-light'
            },
            buttonsStyling: false
        }).then(result => {
            if (!result.isConfirmed)
                return;

            $.post('/admin/schedules/publish', {}, function (res) {
                if (res.success) {
                    showToast(res.message || "Schedules published successfully.");
                    getAllSchedules(); // reload datatable
                } else {
                    showToast(res.message || "Failed to publish schedules.", "error");
                }
            }).fail(() => {
                showToast("Failed to publish schedules. Try again.", "error");
            });
        });
    }).fail(() => {
        showToast("Failed to load unpublished schedules.", "error");
    });
}

// RESET FORM FUNCTION
function resetAddScheduleForm() {
    $('#addScheduleForm')[0].reset();
    $('#addScheduleForm .form-control').removeClass('is-invalid');
    $('#addScheduleForm .invalid-feedback').text('');
    $('#addScheduleSubject').empty().append('<option value="">-- Select Subject --</option>');
}

function resetAddScheduleModalOnClose() {
    $('#scheduleAddModal').on('hidden.bs.modal', function () {
        resetAddScheduleForm();
    });
}

function resetEditScheduleForm() {
    $('#editScheduleForm')[0].reset();
    $('#editScheduleForm .form-control').removeClass('is-invalid');
    $('#editScheduleForm .invalid-feedback').text('');
}

function resetEditScheduleModalOnClose() {
    $('#scheduleEditModal').on('hidden.bs.modal', function () {
        resetEditScheduleForm();
    });
}

function clearCourseFilter() {
    $('#filterByCourse').val('').trigger('change');
    $('#clearCourseFilterBtn').addClass('d-none');
}

$(document).ready(function () {
    getAllSchedules();
    initScheduleFilters();
    loadFilterCourseDropdown();
    handleEditScheduleFormSubmission();
    handleAddScheduleFormSubmission();
    initScheduleModal();
    resetAddScheduleForm();
    resetAddScheduleModalOnClose();
    resetEditScheduleModalOnClose();
    updateClearButtonTheme();
    observeThemeChanges();
    $('#clearCourseFilterBtn').on('click', clearCourseFilter);
});
