// FETCHING DATATABLE
function fetchData(url) {
    return $.ajax({
        url: url,
        method: 'GET',
        dataType: 'json'
    });
}

async function getAllSubjects() {
    try {
        const response = await fetchData('/admin/subjects/list');
        if (response.success) {
            initSubjectsDataTable(response.data);
        } else {
            showToast(response.message || 'Failed to load subjects.', 'error');
        }
    } catch (error) {
        console.error('Failed to load subjects:', error);
        showToast('Unable to fetch subjects.', 'error');
    }
}

const initSubjectsDataTable = (subjects) => {
    $('#dt_subjects').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[1, 'asc']],
        deferRender: true,
        responsive: true,
        data: subjects,
        columns: [
            {data: 'subjectId', visible: false},
            {data: 'subjectCode'},
            {data: 'subjectName'},
            {data: 'description'},
            {
                data: function (data) {
                    const editBtn = `<a href="javascript:void(0);" class="btn btn-icon btn-edit btn-sm me-2 btn-primary" onclick="editSubjectModal(${data.subjectId})" title="Edit Subject"><i class="fa-solid fa-pencil"></i></a>`;
                    const deleteBtn = `<a href="javascript:void(0);" class="btn btn-icon btn-delete btn-sm btn-danger" onclick="deleteSubject(${data.subjectId})" title="Delete Subject"><i class="fa-solid fa-minus"></i></a>`;
                    return editBtn + deleteBtn;
                },
                width: "5%",
                title: 'Actions',
                orderable: false,
                className: 'text-center'
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

async function createSubject(data) {
    try {
        const response = await postData("/admin/subjects/add", data, [getAllSubjects, reloadSubjectTable]);
        if (response.success) {
            $('#subjectAddModal').modal('hide');
            resetAddSubjectForm();
            showToast("Subject created successfully.");
        } else {
            showToast(response.message || "Failed to create subject.", "error");
        }
    } catch (error) {
        console.error('Error creating subject:', error);
        showToast("Unexpected error occurred while adding subject.", "error");
    }
}

function handleAddSubjectFormSubmission() {
    $('#addSubjectForm').submit(async function (e) {
        e.preventDefault();

        const subjectNamePattern = /^.+\s\((1st|2nd|3rd)\s(Semester|Trimester),\s(1st|2nd|3rd|4th)\sYear\)$/;
        const data = {
            code: $('#addSubjectCode').val().trim(),
            name: $('#addSubjectName').val().trim(),
            description: $('#addSubjectDescription').val().trim()
        };

        let isValid = true;
        const subjectName = data.name;

        $('#addSubjectForm .form-control').removeClass('is-invalid');
        $('#addSubjectForm .invalid-feedback').text('');

        if (!data.code) {
            $('#addSubjectCode').addClass('is-invalid').siblings('.invalid-feedback').text('Subject code is required.');
            isValid = false;
        }

        if (!subjectName) {
            $('#addSubjectName').addClass('is-invalid').siblings('.invalid-feedback')
                    .text('Subject name is required.');
            isValid = false;
        } else if (!subjectNamePattern.test(subjectName)) {
            $('#addSubjectName').addClass('is-invalid').siblings('.invalid-feedback')
                    .text('Format must be: e.g. Java Fundamentals (1st Semester, 2nd Year). Make sure there\'s a space before the parenthesis and use only 1st to 4th.');
            isValid = false;
        }

        if (!data.description) {
            $('#addSubjectDescription').addClass('is-invalid').siblings('.invalid-feedback').text('Description is required.');
            isValid = false;
        }

        if (!isValid)
            return;

        const confirm = await Swal.fire({
            title: 'Add Subject?',
            text: `Are you sure you want to add this subject?`,
            icon: 'question',
            background: '#1f1f1f',
            color: '#ffffff',
            iconColor: '#17a2b8',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, add it!',
            cancelButtonText: 'Cancel'
        });

        if (!confirm.isConfirmed)
            return;

        await createSubject(data);
    });
}

// PATCH DATA
function patchData(url, data = {}, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'PATCH',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (response) {
            callbacks.forEach(callback => callback());
            return response;
        }
    });
}

async function updateSubject(data) {
    $('#editSubjectForm .form-control').removeClass('is-invalid');
    $('#editSubjectForm .invalid-feedback').text('');

    const subjectNamePattern = /^.+\s\((1st|2nd|3rd)\s(Semester|Trimester),\s(1st|2nd|3rd|4th)\sYear\)$/;

    let isValid = true;

    // Subject Code Validation
    if (!data.code) {
        $('#editSubjectCode').addClass('is-invalid').siblings('.invalid-feedback')
                .text('Subject code is required.');
        isValid = false;
    }

    // Subject Name Validation
    if (!data.name) {
        $('#editSubjectName').addClass('is-invalid').siblings('.invalid-feedback')
                .text('Subject name is required.');
        isValid = false;
    } else if (!subjectNamePattern.test(data.name)) {
        $('#editSubjectName').addClass('is-invalid').siblings('.invalid-feedback')
                .text('Format must be: e.g. Java Fundamentals (1st Semester, 2nd Year). Make sure there\'s a space before the parenthesis and use only 1st to 4th.');
        isValid = false;
    }

    // Description Validation
    if (!data.description) {
        $('#editSubjectDescription').addClass('is-invalid').siblings('.invalid-feedback')
                .text('Description is required.');
        isValid = false;
    }

    if (!isValid)
        return;

    const confirm = await Swal.fire({
        title: 'Update Subject?',
        text: 'Do you want to save these changes?',
        icon: 'question',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#17a2b8',
        showCancelButton: true,
        confirmButtonColor: '#28a745',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, update it!',
        cancelButtonText: 'Cancel'
    });

    if (!confirm.isConfirmed)
        return;

    try {
        const response = await patchData("/admin/subjects/update", data, [reloadSubjectTable]);
        if (response.success) {
            $('#subjectEditModal').modal('hide');
            showToast(response.message || 'Subject updated successfully.');
        } else {
            showToast(response.message || 'Failed to update subject.', 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('Unable to update subject.', 'error');
    }
}

function editSubjectModal(subjectId) {
    $.ajax({
        url: `/admin/subjects/get/${subjectId}`,
        method: 'GET',
        dataType: 'json',
        success: function (response) {
            if (response.success) {
                const subject = response.data;
                $('#editSubjectId').val(subject.subjectId);
                $('#editSubjectCode').val(subject.subjectCode);
                $('#editSubjectName').val(subject.subjectName);
                $('#editSubjectDescription').val(subject.description);
                $('#subjectEditModal').modal('show');
            } else {
                showToast(response.message || 'Subject not found.', 'error');
            }
        },
        error: function () {
            showToast('Could not fetch subject details.', 'error');
        }
    });
}

function handleEditSubjectFormSubmission() {
    $('#editSubjectForm').on('submit', function (e) {
        e.preventDefault();
        const data = {
            id: $('#editSubjectId').val(),
            code: $('#editSubjectCode').val().trim(),
            name: $('#editSubjectName').val().trim(),
            description: $('#editSubjectDescription').val().trim()
        };
        updateSubject(data);
    });
}

// DELETE DATA
function deleteData(url, _, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'DELETE',
        success: function (response) {
            callbacks.forEach(callback => callback());
            return response;
        }
    });
}

async function deleteDataById(id) {
    try {
        const response = await deleteData(`/admin/subjects/delete/${id}`, null, [reloadSubjectTable]);
        if (response.success) {
            showToast(response.message || 'Subject deleted successfully.');
        } else {
            showToast(response.message || 'Failed to delete subject.', 'error');
        }
    } catch (error) {
        console.error('Error deleting subject:', error);
        showToast('Could not delete subject. Please try again.', 'error');
    }
}

function deleteSubject(id) {
    Swal.fire({
        title: 'Delete Subject?',
        text: 'This action cannot be undone.',
        icon: 'warning',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#ffc107',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, delete it!',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteDataById(id);
        }
    });
}

function resetAddSubjectForm() {
    $('#addSubjectForm')[0].reset();
    $('#addSubjectForm .form-control').removeClass('is-invalid');
    $('#addSubjectForm .invalid-feedback').text('');
}

function resetModalOnClose() {
    $('#subjectAddModal').on('hidden.bs.modal', function () {
        resetAddSubjectForm();
    });
}

// RELOAD TABLE
const reloadSubjectTable = () => getAllSubjects();

$(document).ready(function () {
    getAllSubjects();
    resetModalOnClose();
    handleAddSubjectFormSubmission();
    handleEditSubjectFormSubmission();
});
