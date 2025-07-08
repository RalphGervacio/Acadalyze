// FOR FETCHING DATATABLE
function fetchData(url) {
    return $.ajax({
        url: url,
        method: 'GET',
        dataType: 'json'
    });
}

async function getAllCourses() {
    try {
        const response = await fetchData('/admin/courses/list');
        if (response.success) {
            initCoursesDataTable(response.data);
        } else {
            showToast(response.message, 'error');
        }
    } catch (error) {
        console.error('Failed to load courses:', error);
    }
}

const initCoursesDataTable = (courses) => {
    $('#dt_courses').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[1, 'asc']],
        deferRender: true,
        responsive: true,
        data: courses,
        columns: [
            {data: 'course_id', visible: false},
            {data: 'course_code'},
            {data: 'course_title'},
            {data: 'course_description'},
            {
                data: function (data) {
                    return `
                        <a href="javascript:void(0);" class="btn btn-icon btn-edit btn-sm me-2 btn-primary" onclick="editCourseModal(${data.course_id})" title="Edit Course">
                            <i class="fa-solid fa-pencil"></i>
                        </a>
                        <a href="javascript:void(0);" class="btn btn-icon btn-delete btn-sm btn-danger" onclick="deleteCourse(${data.course_id})" title="Delete Course">
                            <i class="fa-solid fa-minus"></i>
                        </a>
                    `;
                },
                width: "5%",
                title: 'Actions',
                orderable: false,
                className: 'text-center'
            }
        ]
    });
};

function postData(url, data = {}, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (response) {
            callbacks.forEach((cb) => cb());
            return response;
        }
    });
}

async function createCourse(data) {
    try {
        const response = await postData("/admin/courses/add", data, [getAllCourses, reloadCourseTable]);
        if (response.success) {
            $('#courseAddModal').modal('hide');
            resetAddCourseForm();
            showToast("Course created successfully.");
        } else {
            showToast(response.message, 'error');
        }
    } catch (error) {
        console.error('Error creating course:', error);
    }
}

function handleAddCourseFormSubmission() {
    $('#addCourseForm').submit(async function (e) {
        e.preventDefault();

        const data = {
            code: $('#addCourseCode').val().trim(),
            name: $('#addCourseTitle').val().trim(),
            description: $('#addCourseDescription').val().trim()
        };

        let isValid = true;

        $('#addCourseForm .form-control').removeClass('is-invalid');
        $('#addCourseForm .invalid-feedback').text('');

        if (!data.code) {
            $('#addCourseCode').addClass('is-invalid').siblings('.invalid-feedback').text('Course code is required.');
            isValid = false;
        }

        if (!data.name) {
            $('#addCourseTitle').addClass('is-invalid').siblings('.invalid-feedback').text('Course name is required.');
            isValid = false;
        }

        if (!data.description) {
            $('#addCourseDescription').addClass('is-invalid').siblings('.invalid-feedback').text('Description is required.');
            isValid = false;
        }

        if (!isValid)
            return;

        const confirm = await Swal.fire({
            title: 'Add Course?',
            text: `Are you sure you want to add this course?`,
            icon: 'question',
            background: '#1f1f1f',
            color: '#ffffff',
            iconColor: '#17a2b8',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, add it!'
        });

        if (!confirm.isConfirmed)
            return;

        await createCourse(data);
    });
}

function patchData(url, data = {}, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'PATCH',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (response) {
            callbacks.forEach((cb) => cb());
            return response;
        }
    });
}

async function updateCourse(data) {
    $('#editCourseForm .form-control').removeClass('is-invalid');
    $('#editCourseForm .invalid-feedback').text('');

    let isValid = true;

    if (!data.code) {
        $('#editCourseCode').addClass('is-invalid').siblings('.invalid-feedback').text('Course code is required.');
        isValid = false;
    }

    if (!data.name) {
        $('#editCourseTitle').addClass('is-invalid').siblings('.invalid-feedback').text('Course name is required.');
        isValid = false;
    }

    if (!data.description) {
        $('#editCourseDescription').addClass('is-invalid').siblings('.invalid-feedback').text('Description is required.');
        isValid = false;
    }

    if (!isValid)
        return;

    const confirm = await Swal.fire({
        title: 'Update Course?',
        text: 'Do you want to save these changes?',
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
        const response = await patchData("/admin/courses/update", data, [reloadCourseTable]);

        if (response.success) {
            $('#courseEditModal').modal('hide');
            showToast(response.message);
        } else {
            showToast(response.message || 'Update failed.', 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('Unable to update course.', 'error');
    }
}

function editCourseModal(course_id) {
    $.get(`/admin/courses/get/${course_id}`, function (response) {
        if (response.success) {
            const course = response.data;
            $('#editCourseId').val(course.course_id);
            $('#editCourseCode').val(course.course_code);
            $('#editCourseTitle').val(course.course_title);
            $('#editCourseDescription').val(course.course_description);
            $('#courseEditModal').modal('show');
        } else {
            showToast(response.message || 'Course not found.', 'error');
        }
    }).fail(() => showToast('Could not fetch course details.', 'error'));
}

function handleEditCourseFormSubmission() {
    $('#editCourseForm').submit(function (e) {
        e.preventDefault();

        const data = {
            id: $('#editCourseId').val(),
            code: $('#editCourseCode').val().trim(),
            name: $('#editCourseTitle').val().trim(),
            description: $('#editCourseDescription').val().trim()
        };

        updateCourse(data);
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

async function deleteDataById(id) {
    try {
        const response = await deleteData(`/admin/courses/delete/${id}`, null, [reloadCourseTable]);
        if (response.success) {
            showToast(response.message);
        } else {
            showToast(response.message || 'Unable to delete course.', 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('Could not delete course. Some students might still be enrolled.', 'error');
    }
}

function deleteCourse(id) {
    Swal.fire({
        title: 'Delete Course?',
        text: 'This action cannot be undone.',
        icon: 'warning',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#ffc107',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, delete it!'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteDataById(id);
        }
    });
}

function resetAddCourseForm() {
    $('#addCourseForm')[0].reset();
    $('#addCourseForm .form-control').removeClass('is-invalid');
    $('#addCourseForm .invalid-feedback').text('');
}

function resetModalOnClose() {
    $('#courseAddModal').on('hidden.bs.modal', function () {
        resetAddCourseForm();
    });
}

const reloadCourseTable = () => getAllCourses();

$(document).ready(function () {
    getAllCourses();
    resetModalOnClose();
    handleAddCourseFormSubmission();
    handleEditCourseFormSubmission();
});
