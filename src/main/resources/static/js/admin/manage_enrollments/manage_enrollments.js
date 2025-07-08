var search = {};

// FETCH DATA
function fetchData(url) {
    return $.ajax({
        url,
        method: 'GET',
        dataType: 'json'
    });
}

async function getAllEnrollments() {
    try {
        const queryString = new URLSearchParams(search).toString();
        const url = `/admin/enrollments/list${queryString ? '?' + queryString : ''}`;

        const response = await fetchData(url);
        if (response.success) {
            initEnrollmentsDataTable(response.data);
            $('#btnBulkDelete').addClass('d-none');
        } else {
            showToast(response.message, 'error');
        }
    } catch (error) {
        console.error('Failed to load enrollments:', error);
        showToast('Something went wrong.', 'error');
    }
}

const initEnrollmentsDataTable = (enrollments) => {
    $('#dt_enrollments').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[2, 'desc']],
        deferRender: true,
        responsive: true,
        data: enrollments,
        columns: [
            {
                data: 'enrollmentId',
                title: `<input type="checkbox" id="selectAllEnrollments" />`,
                orderable: false,
                className: 'text-center',
                width: '3%',
                render: data => `<input type="checkbox" class="enrollment-checkbox" value="${data}" />`
            },
            {data: 'studentId', title: 'Student ID'},
            {data: 'fullName', title: 'Name'},
            {data: 'email', title: 'Email'},
            {data: 'courseCode', title: 'Course Code'},
            {data: 'courseTitle', title: 'Course Title'},
            {data: 'enrolledAtFormatted', title: 'Enrolled At'},
            {
                data: data => `
                    <a href="javascript:void(0);" 
                       class="btn btn-icon btn-delete btn-sm btn-danger" 
                       onclick="deleteEnrollment(${data.enrollmentId})" 
                       title="Unenroll Student">
                       <i class="fa-solid fa-trash"></i>
                    </a>`,
                title: 'Actions',
                orderable: false,
                className: 'text-center',
                width: '5%'
            }
        ],
        initComplete: function () {
            if (!$('#btnBulkDelete').length) {
                const $deleteBtn = $(`
                    <button id="btnBulkDelete" 
                            class="btn btn-sm btn-danger ms-2 d-none" 
                            onclick="bulkDeleteEnrollments()">
                        <i class="bi bi-trash3"></i> Delete Selected
                    </button>
                `);
                $('#dt_enrollments_info').before($deleteBtn);
            }
        }
    });

    $('#dt_enrollments').on('change', '#selectAllEnrollments', function () {
        const isChecked = $(this).is(':checked');
        $('input.enrollment-checkbox').prop('checked', isChecked).trigger('change');
    });

    $('#dt_enrollments').on('change', 'input.enrollment-checkbox, #selectAllEnrollments', function () {
        const selectedCount = $('input.enrollment-checkbox:checked').length;
        $('#btnBulkDelete').toggleClass('d-none', selectedCount < 2);
    });
};

// POST DATA
function postData(url, data = {}, callbacks = []) {
    return $.ajax({
        url,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (response) {
            callbacks.forEach(cb => cb());
        }
    });
}

// HANDLE ADD ENROLLMENT
function handleAddEnrollmentFormSubmission() {
    $('#addEnrollmentForm').submit(async function (e) {
        e.preventDefault();

        const data = {
            authUserId: $('#selectStudent').val(),
            courseId: $('#selectCourseInModal').val()
        };

        $('#addEnrollmentForm .form-select').removeClass('is-invalid');
        $('#addEnrollmentForm .invalid-feedback').text('');

        let isValid = true;

        if (!data.authUserId) {
            $('#selectStudent').addClass('is-invalid')
                    .siblings('.invalid-feedback').text('Student selection is required.');
            isValid = false;
        }

        if (!data.courseId) {
            $('#selectCourseInModal').addClass('is-invalid')
                    .siblings('.invalid-feedback').text('Course selection is required.');
            isValid = false;
        }

        if (!isValid)
            return;

        const confirm = await Swal.fire({
            title: 'Enroll Student?',
            text: `Are you sure you want to enroll this student in the selected course?`,
            icon: 'question',
            background: '#1f1f1f',
            color: '#ffffff',
            iconColor: '#17a2b8',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, enroll!',
            cancelButtonText: 'Cancel'
        });

        if (!confirm.isConfirmed)
            return;

        try {
            const response = await postData("/admin/enrollments/add", data, [getAllEnrollments]);

            if (response.success) {
                // Close modal
                $('#enrollmentAddModal').modal('hide');

                // Reset form fields and validation
                const $form = $('#addEnrollmentForm');
                $form[0].reset();
                $form.find('.form-select').removeClass('is-invalid');
                $form.find('.invalid-feedback').text('');

                // Reset Select2 dropdowns properly
                $('#selectStudent').val(null).trigger('change');
                $('#selectCourseInModal').val(null).trigger('change');

                loadStudentsDropdown();
                loadFilterStudentDropdown();

                showToast(response.message);
            }
        } catch (error) {
            console.error(error);
            showToast('Unable to enroll student.', 'error');
        }
    });
}

// DELETE DATA
function deleteData(url, _, callbacks = []) {
    return $.ajax({
        url,
        method: 'DELETE',
        success: function (response) {
            callbacks.forEach(cb => cb());
        }
    });
}

async function deleteEnrollmentById(id) {
    try {
        const response = await deleteData(`/admin/enrollments/delete/${id}`, null, [reloadEnrollmentTable]);

        if (response.success) {
            loadStudentsDropdown();
            loadFilterStudentDropdown();
            showToast(response.message, 'success');
        } else {
            showToast(response.message || 'Unable to delete enrollment.', 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('Request failed. Try again.', 'error');
    }
}

function deleteEnrollment(id) {
    Swal.fire({
        title: 'Unenroll Student?',
        text: 'This will permanently remove the enrollment.',
        icon: 'warning',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#ffc107',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, delete it!',
        cancelButtonText: 'Cancel'
    }).then(result => {
        if (result.isConfirmed)
            deleteEnrollmentById(id);
    });
}

// BULK DELETE
function bulkDeleteEnrollments() {
    const selectedIds = $('.enrollment-checkbox:checked').map(function () {
        return $(this).val();
    }).get();

    if (selectedIds.length === 0)
        return;

    Swal.fire({
        title: 'Confirm Deletion',
        text: `Are you sure you want to unenroll ${selectedIds.length} student(s)?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, delete them!',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#ffc107'
    }).then(async result => {
        if (result.isConfirmed) {
            try {
                const response = await $.ajax({
                    url: '/admin/enrollments/bulk-delete',
                    method: 'DELETE',
                    contentType: 'application/json',
                    data: JSON.stringify({enrollmentIds: selectedIds})
                });

                if (response.success) {
                    loadStudentsDropdown();
                    getAllEnrollments();
                    showToast(response.message, 'success');
                } else {
                    throw new Error(response.message || 'Failed to delete');
                }
            } catch (error) {
                showToast(error.message || 'Something went wrong', 'error');
            }
        }
    });
}

// DROPDOWNS
async function loadStudentsDropdown() {
    try {
        const response = await fetchData('/admin/enrollments/students');
        const $select = $('#selectStudent');
        $select.empty().append(`<option value="">-- Select Student --</option>`);
        response.success && response.data.length
                ? response.data.forEach(student =>
                    $select.append(`<option value="${student.authUserId}">${student.studentId} - ${student.fullName}</option>`))
                : $select.append(`<option value="">No students found</option>`);
    } catch (error) {
        console.error(error);
    }
}

async function loadCoursesDropdown() {
    try {
        const response = await fetchData('/admin/enrollments/courses');
        const $select = $('#selectCourseInModal');
        $select.empty().append(`<option value="">-- Select Course --</option>`);
        response.success && response.data.length
                ? response.data.forEach(course =>
                    $select.append(`<option value="${course.courseId}">${course.courseCode} - ${course.courseTitle}</option>`))
                : $select.append(`<option value="">No courses found</option>`);
    } catch (error) {
        console.error(error);
    }
}

async function loadFilterStudentDropdown() {
    try {
        const response = await fetchData('/admin/enrollments/students/with-enrollments');
        const $filter = $('#filterStudent');
        $filter.empty().append(`<option value="">-- All Students --</option>`);
        response.success && response.data.length
                ? response.data.forEach(student =>
                    $filter.append(`<option value="${student.studentId}">${student.studentId} - ${student.fullName}</option>`))
                : $filter.append(`<option value="">No enrolled students found</option>`);
    } catch (error) {
        console.error(error);
    }
}

function resetEnrollmentModal() {
    const $form = $('#addEnrollmentForm');

    // Reset form values
    $form[0].reset();

    // Remove validation errors
    $form.find('.form-select').removeClass('is-invalid');
    $form.find('.invalid-feedback').text('');

    // Reset Select2 dropdowns
    $('#selectStudent').val(null).trigger('change');
    $('#selectCourseInModal').val(null).trigger('change');
}

// RELOAD & FILTER
const reloadEnrollmentTable = () => getAllEnrollments();

const handleChange = (e) => {
    const {name, value} = e.target;
    value === "" ? delete search[name] : search[name] = value;
    getAllEnrollments();
};

const handleClear = () => {
    const selectedFilter = $('#filterStudent').val();

    if (!selectedFilter) {
        showToast('There is nothing to clear.', 'info');
        return;
    }

    $('#filterStudent').val(null).trigger('change');
    delete search['studentId'];
    getAllEnrollments();
    showToast('Filter cleared.');
};

$(document).ready(function () {
    getAllEnrollments();
    handleAddEnrollmentFormSubmission();
    loadFilterStudentDropdown();
    loadStudentsDropdown();
    loadCoursesDropdown();

    $('#enrollmentAddModal').on('hidden.bs.modal', resetEnrollmentModal);

    $('#filterStudent').select2({
        placeholder: '-- All Students --',
        allowClear: true,
        width: '100%'
    });

    $('#enrollmentAddModal').on('shown.bs.modal', function () {
        $('#selectStudent, #selectCourseInModal').select2({
            dropdownParent: $('#enrollmentAddModal'),
            width: '100%',
            placeholder: function () {
                return $(this).attr('id') === 'selectStudent' ? '-- Select Student --' : '-- Select Course --';
            }
        });
    });
});
