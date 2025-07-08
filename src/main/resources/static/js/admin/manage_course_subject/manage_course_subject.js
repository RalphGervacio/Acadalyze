let courseSubjectList = [];
// FETCHING DATATABLE
function fetchData(url) {
    return $.ajax({
        url: url,
        method: 'GET',
        dataType: 'json'
    });
}

function getCourseSubjects() {
    $.ajax({
        url: '/admin/course-subjects/list',
        method: 'GET',
        dataType: 'json',
        success: function (res) {
            console.log("Course Subject List Response:", res);
            if (res.success && Array.isArray(res.data)) {
                courseSubjectList = res.data;
                initCourseSubjectsDataTable(res.data);
            } else {
                courseSubjectList = [];
                showToast(res.message || 'Failed to load subjects.', 'error');
            }
        },
        error: function (xhr) {
            courseSubjectList = [];
            console.error('Failed to load course subjects:', xhr);
            showToast('Unable to fetch course subjects.', 'error');
        }
    });
}

const initCourseSubjectsDataTable = (data) => {
    $('#dt_course_subjects').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[0, 'asc']],
        deferRender: true,
        responsive: true,
        data: data,
        columns: [
            {data: 'course_code', title: 'Course Code'},
            {data: 'course_title', title: 'Course Title'},
            {data: 'subject_code', title: 'Subject Code'},
            {data: 'subject_name', title: 'Subject Name'},
            {data: 'semester', title: 'Semester'},
            {data: 'year_level', title: 'Year Level'},
            {
                data: function (row) {
                    return `
                        <button class="btn btn-secondary btn-sm me-1" onclick="editCourseSubject(${row.course_subject_id}, '${row.semester}', '${row.year_level}', ${row.course_id}, ${row.subject_id})">
                            <i class="fa-solid fa-pencil"></i>
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="confirmDeleteCourseSubject(${row.course_subject_id})">
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

function handleFilterCourseChange(event) {
    const selectedCourseId = event.target.value;

    if (selectedCourseId) {
        $('#clearCourseFilterBtn').removeClass('d-none');
    } else {
        $('#clearCourseFilterBtn').addClass('d-none');
    }

    // Filter the data
    const filtered = selectedCourseId
            ? courseSubjectList.filter(item => item.course_id == selectedCourseId)
            : courseSubjectList;

    initCourseSubjectsDataTable(filtered);
    updateClearButtonTheme();
}

// Load dropdowns for Course and Subject
function loadCourseAndSubjectDropdowns() {
    fetchData('/admin/course-subjects/courses')
            .then(response => {
                if (response.success) {
                    const $selectCourse = $('#selectCourse, #editCourse');
                    $selectCourse.empty().append('<option value="">-- Select Course --</option>');
                    response.data.forEach(course => {
                        $selectCourse.append(`<option value="${course.courseId}">${course.courseCode} - ${course.courseTitle}</option>`);
                    });
                }
            });

    fetchData('/admin/course-subjects/subjects')
            .then(response => {
                if (response.success) {
                    const $selectSubject = $('#selectSubject, #editSubject');
                    $selectSubject.empty().append('<option value="">-- Select Subject --</option>');
                    response.data.forEach(subject => {
                        $selectSubject.append(`<option value="${subject.subjectId}">${subject.subjectCode} - ${subject.subjectName}</option>`);
                    });
                }
            });

    fetchData('/admin/course-subjects/courses')
            .then(response => {
                if (response.success) {
                    const $selectCourse = $('#selectCourse, #editCourse, #filterCourse');
                    $selectCourse.empty().append('<option value="">Filter by Course</option>');
                    response.data.forEach(course => {
                        const display = `${course.courseCode} - ${course.courseTitle}`;
                        $selectCourse.append(`<option value="${course.courseId}">${display}</option>`);
                    });
                }
            });

}

// POST DATA
function postData(url, data = {}, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data)
    }).done(function (response) {
        console.log("Response from post data:" + response);
        callbacks.forEach(callback => callback());
    });
}

async function assignCourseSubject(data) {
    try {
        const response = await postData("/admin/course-subjects/add", data, [getCourseSubjects]);
        if (response.success) {
            $('#courseSubjectAddModal').modal('hide');
            resetAssignCourseSubjectForm();
            showToast("Subject assigned to course successfully.");
        } else {
            showToast(response.message || "Failed to assign subject.", "error");
        }
    } catch (xhr) {
        const err = xhr.responseJSON;
        if (err && err.message) {
            showToast(err.message, "error");
        } else {
            console.error('Error assigning subject to course:', xhr);
            showToast("Unexpected error occurred while assigning subject.", "error");
        }
    }
}

function validateAssignCourseSubjectForm(data) {
    let isValid = true;
    let message = "";

    $('#assignCourseSubjectForm .form-select').removeClass('is-invalid');
    $('#assignCourseSubjectForm .invalid-feedback').text('');

    if (!data.course_id) {
        $('#selectCourse').addClass('is-invalid').siblings('.invalid-feedback').text('Course is required.');
        isValid = false;
    }
    if (!data.subject_id) {
        $('#selectSubject').addClass('is-invalid').siblings('.invalid-feedback').text('Subject is required.');
        isValid = false;
    }
    if (!data.semester) {
        $('#semester').addClass('is-invalid').siblings('.invalid-feedback').text('Semester is required.');
        isValid = false;
    }
    if (!data.year_level) {
        $('#yearLevel').addClass('is-invalid').siblings('.invalid-feedback').text('Year level is required.');
        isValid = false;
    }

    return {valid: isValid, message};
}

function handleAssignCourseSubjectFormSubmission() {
    $('#assignCourseSubjectForm').submit(async function (e) {
        e.preventDefault();

        const data = {
            course_id: $('#selectCourse').val(),
            subject_id: $('#selectSubject').val(),
            semester: $('#semester').val(),
            year_level: $('#yearLevel').val()
        };

        const validation = validateAssignCourseSubjectForm(data);
        if (!validation.valid)
            return;

        // Duplicate check
        const isDuplicate = courseSubjectList.some(item =>
            item.course_id == data.course_id &&
                    item.subject_id == data.subject_id &&
                    item.semester == data.semester &&
                    item.year_level == data.year_level
        );

        if (isDuplicate) {
            showToast('This subject is already assigned to the selected course, semester, and year level.', 'error');
            return;
        }

        const confirm = await Swal.fire({
            title: 'Assign Subject?',
            text: `Are you sure you want to assign this subject to the course?`,
            icon: 'question',
            background: '#1f1f1f',
            color: '#ffffff',
            iconColor: '#17a2b8',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, assign',
            cancelButtonText: 'Cancel'
        });

        if (!confirm.isConfirmed)
            return;

        await assignCourseSubject(data);
    });
}

// PATCH DATA
function patchData(url, data = {}, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'PATCH',
        contentType: 'application/json',
        data: JSON.stringify(data)
    }).done(function (response) {
        callbacks.forEach(callback => callback());
    });
}

// Show Edit Modal and Populate Fields
function editCourseSubject(id, semester, yearLevel, courseId, subjectId) {
    $('#editCourseSubjectId').val(id);
    $('#editSemester').val(semester);
    $('#editYearLevel').val(yearLevel);

    fetchData('/admin/course-subjects/courses').then(res => {
        if (res.success) {
            const $c = $('#editCourse');
            $c.empty().append('<option value="">-- Select --</option>');
            res.data.forEach(course => {
                const display = `${course.courseCode} - ${course.courseTitle}`;
                $c.append(`<option value="${course.courseId}">${display}</option>`);
            });
            console.log("Setting course to:", courseId);
            setTimeout(() => {
                $c.val(String(courseId)).trigger('change');
                console.log("Dropdown now:", $c.html());
            }, 100);
        }
    });

    fetchData('/admin/course-subjects/subjects').then(res => {
        if (res.success) {
            const $s = $('#editSubject');
            $s.empty().append('<option value="">-- Select --</option>');
            res.data.forEach(subject => {
                const display = `${subject.subjectCode} - ${subject.subjectName}`;
                $s.append(`<option value="${subject.subjectId}">${display}</option>`);
            });
            console.log("Setting subject to:", subjectId);
            setTimeout(() => {
                $s.val(String(subjectId)).trigger('change');
                console.log("Dropdown now:", $s.html());
            }, 100);
        }
    });

    $('#courseSubjectEditModal').modal('show');
}

// Handle Edit Form Submission
function handleEditCourseSubjectFormSubmission() {
    $('#editCourseSubjectForm').submit(async function (e) {
        e.preventDefault();

        const data = {
            course_subject_id: $('#editCourseSubjectId').val(),
            semester: $('#editSemester').val(),
            year_level: $('#editYearLevel').val(),
            course_id: $('#editCourse').val(),
            subject_id: $('#editSubject').val()
        };

        $('#editCourseSubjectForm .form-select').removeClass('is-invalid');
        $('#editCourseSubjectForm .invalid-feedback').text('');

        let isValid = true;

        if (!data.semester) {
            $('#editSemester').addClass('is-invalid').siblings('.invalid-feedback').text('Semester is required.');
            isValid = false;
        }

        if (!data.year_level) {
            $('#editYearLevel').addClass('is-invalid').siblings('.invalid-feedback').text('Year level is required.');
            isValid = false;
        }

        if (!isValid)
            return;

        const confirm = await Swal.fire({
            title: 'Update Assignment?',
            text: 'Do you want to update this subject assignment?',
            icon: 'question',
            background: '#1f1f1f',
            color: '#ffffff',
            iconColor: '#17a2b8',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, update it!'
        });

        if (!confirm.isConfirmed)
            return;

        try {
            const response = await patchData("/admin/course-subjects/update", data, [getCourseSubjects]);

            if (response.success) {
                $('#courseSubjectEditModal').modal('hide');
                showToast(response.message);
            } else {
                showToast(response.message || 'Update failed.', 'error');
            }
        } catch (xhr) {
            const err = xhr.responseJSON;
            if (err && err.message) {
                showToast(err.message, 'error');
            } else {
                console.error('Error updating course subject:', xhr);
                showToast('Unexpected error occurred while updating.', 'error');
            }
        }
    });
}

function deleteData(url, _, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'DELETE',
        success: function (response) {
            callbacks.forEach((cb) => cb());
            return response;
        }
    });
}

async function deleteCourseSubjectById(id) {
    try {
        const response = await deleteData(`/admin/course-subjects/delete/${id}`, null, [getCourseSubjects]);
        if (response.success) {
            showToast(response.message);
        } else {
            showToast(response.message || 'Unable to delete course subject.', 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('Could not delete course subject. It may be linked to other records.', 'error');
    }
}

function confirmDeleteCourseSubject(id) {
    Swal.fire({
        title: 'Remove Subject from Course?',
        text: 'This action cannot be undone.',
        icon: 'warning',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#ffc107',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, remove it!'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteCourseSubjectById(id);
        }
    });
}

// RESET FUNCTIONS
function resetAssignCourseSubjectForm() {
    $('#assignCourseSubjectForm')[0].reset();
    $('#assignCourseSubjectForm .form-select').removeClass('is-invalid');
    $('#assignCourseSubjectForm .invalid-feedback').text('');
}

function resetEditCourseSubjectForm() {
    $('#editCourseSubjectForm')[0].reset();
    $('#editCourseSubjectForm .form-select').removeClass('is-invalid');
    $('#editCourseSubjectForm .invalid-feedback').text('');
}

function reloadCourseSubjectTable() {
    getCourseSubjects();
}

function handleClearCourseFilter() {
    $('#filterCourse').val('').trigger('change');
    $('#clearCourseFilterBtn').addClass('d-none');
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

// INIT
$(document).ready(function () {
    getCourseSubjects();
    reloadCourseSubjectTable();
    handleAssignCourseSubjectFormSubmission();
    handleEditCourseSubjectFormSubmission();
    loadCourseAndSubjectDropdowns();
    updateClearButtonTheme();
    observeThemeChanges();
    $('#clearCourseFilterBtn').on('click', handleClearCourseFilter);

    $('#courseSubjectEditModal').on('hidden.bs.modal', function () {
        resetEditCourseSubjectForm();
    });

    $('#courseSubjectAddModal').on('hidden.bs.modal', function () {
        resetAssignCourseSubjectForm();
    });
});
