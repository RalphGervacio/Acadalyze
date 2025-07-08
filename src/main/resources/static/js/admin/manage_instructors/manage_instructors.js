// FETCHING DATATABLE
function fetchData(url) {
    return $.ajax({
        url: url,
        method: 'GET',
        dataType: 'json'
    });
}

async function getAssignedInstructors() {
    try {
        const response = await fetchData('/admin/instructors/list');
        console.log("Instructor List Response:", response);
        if (response.success) {
            initInstructorDataTable(response.data);
        } else {
            showToast(response.message || 'Failed to load instructors.', 'error');
        }
    } catch (error) {
        console.error('Failed to load instructors:', error);
        showToast('Unable to fetch instructors.', 'error');
    }
}

const initInstructorDataTable = (instructors) => {
    $('#dt_instructors').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[1, 'asc']],
        deferRender: true,
        responsive: true,
        data: instructors,
        columns: [
            {data: 'subjectInstructorId', visible: false},
            {data: 'fullName', title: 'Instructor Name'},
            {data: 'email', title: 'Email'},
            {data: 'subjectCode', title: 'Subject Code'},
            {data: 'subjectName', title: 'Subject Name'},
            {data: 'assignedAtFormatted', title: 'Time Assigned'},
            {
                data: function (row) {
                    return `
                        <button class="btn btn-danger btn-sm" onclick="confirmDeleteInstructor(${row.subjectId}, ${row.instructorId})">
                            <i class="fa-solid fa-trash"></i> Remove
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

async function assignInstructor(data) {
    try {
        const response = await postData("/admin/instructors/assign", data, [getAssignedInstructors]);
        if (response.success) {
            $('#instructorAddModal').modal('hide');
            resetAssignInstructorForm();
            showToast("Instructor assigned successfully.");
        } else {
            showToast(response.message || "Failed to assign instructor.", "error");
        }
    } catch (error) {
        console.error('Error assigning instructor:', error);
        showToast("Unexpected error occurred while assigning instructor.", "error");
    }
}

function handleAssignInstructorFormSubmission() {
    $('#assignInstructorForm').submit(async function (e) {
        e.preventDefault();

        const data = {
            subjectId: $('#selectSubject').val(),
            instructorId: $('#selectInstructor').val()
        };

        let isValid = true;

        $('#assignInstructorForm .form-control').removeClass('is-invalid');
        $('#assignInstructorForm .invalid-feedback').text('');

        if (!data.subjectId) {
            $('#selectSubject').addClass('is-invalid').siblings('.invalid-feedback').text('Subject is required.');
            isValid = false;
        }

        if (!data.instructorId) {
            $('#selectInstructor').addClass('is-invalid').siblings('.invalid-feedback').text('Instructor is required.');
            isValid = false;
        }

        if (!isValid)
            return;

        const confirm = await Swal.fire({
            title: 'Assign Instructor?',
            text: `Are you sure you want to assign this instructor to the subject?`,
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

        await assignInstructor(data);
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

async function deleteInstructor(subjectId, instructorId) {
    try {
        const url = `/admin/instructors/remove?subjectId=${subjectId}&instructorId=${instructorId}`;
        const response = await deleteData(url, null, [getAssignedInstructors]);
        if (response.success) {
            showToast(response.message || 'Instructor removed successfully.');
        } else {
            showToast(response.message || 'Failed to remove instructor.', 'error');
        }
    } catch (error) {
        console.error('Error removing instructor:', error);
        showToast('Could not remove instructor. Please try again.', 'error');
    }
}

function confirmDeleteInstructor(subjectId, instructorId) {
    Swal.fire({
        title: 'Remove Instructor?',
        text: 'Are you sure you want to remove this instructor from the subject?',
        icon: 'warning',
        background: '#1f1f1f',
        color: '#ffffff',
        iconColor: '#ffc107',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, remove',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteInstructor(subjectId, instructorId);
        }
    });
}

// DROPDOWNS
async function loadSubjectDropdown() {
    try {
        const response = await fetchData('/admin/instructors/subjects');
        const $select = $('#selectSubject');
        $select.empty().append(`<option value="">-- Select Subject --</option>`);

        if (response.success && response.data.length) {
            response.data.forEach(subject => {
                $select.append(`<option value="${subject.subjectId}">${subject.subjectCode} - ${subject.subjectName}</option>`);
            });
        } else {
            $select.append(`<option value="">No subjects found</option>`);
        }
    } catch (error) {
        console.error('Failed to load subjects:', error);
    }
}


async function loadInstructorDropdown() {
    try {
        const response = await fetchData('/admin/instructors/dropdown');
        const $dropdown = $('#selectInstructor');
        $dropdown.empty().append('<option value="">-- Select Instructor --</option>');

        if (response.success && response.data.length) {
            response.data.forEach(i => {
                $dropdown.append(`<option value="${i.authUserId}">${i.fullName}</option>`);
            });
        } else {
            $dropdown.append('<option value="">No instructors found</option>');
        }
    } catch (error) {
        console.error('Failed to load instructors:', error);
    }
}

function initInstructorModalSelect2() {
    $('#instructorAddModal').on('shown.bs.modal', function () {
        $('#selectInstructor').select2({
            dropdownParent: $('#instructorAddModal'),
            width: '100%',
            placeholder: '-- Select Instructor --'
        });

        loadInstructorDropdown();
    });
}

function initSubjectModalSelect2() {
    $('#instructorAddModal').on('shown.bs.modal', function () {
        $('#selectSubject').select2({
            dropdownParent: $('#instructorAddModal'),
            width: '100%',
            placeholder: '-- Select Subject --'
        });

        loadSubjectDropdown();
    });
}

// FORM RESET
function resetAssignInstructorForm() {
    $('#assignInstructorForm')[0].reset();
    $('#assignInstructorForm .form-control').removeClass('is-invalid');
    $('#assignInstructorForm .invalid-feedback').text('');
}

function resetInstructorModalOnClose() {
    $('#instructorAddModal').on('hidden.bs.modal', function () {
        resetAssignInstructorForm();
    });
}

// RELOAD TABLE
const reloadInstructorTable = () => getAssignedInstructors();

// ON READY
$(document).ready(function () {
    getAssignedInstructors();
    initSubjectModalSelect2();
    initInstructorModalSelect2();
    handleAssignInstructorFormSubmission();
    resetAssignInstructorForm();
    resetInstructorModalOnClose();
});
