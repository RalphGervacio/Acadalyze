// DATATABLE
const initDataTable = (users) => {
    $('#dt_users').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[0, 'desc']],
        deferRender: true,
        pageLength: 10,
        lengthMenu: [10, 25, 50, 100],
        responsive: true,
        data: users,
        columns: [
            {data: 'authUserId', visible: false},
            {
                data: function (data) {
                    return `${data.firstName} ${data.middleName ?? ''} ${data.lastName}`.trim();
                }
            },
            {data: 'userName'},
            {data: 'email'},
            {
                data: function (data) {
                    const role = data.role?.roleName || '—';
                    const roleClass =
                            role === 'SUPER_ADMIN' || role === 'ADMIN' ? 'text-danger fw-bold' :
                            role === 'STUDENT' ? 'text-success fw-bold' :
                            role === 'INSTRUCTOR' ? 'text-warning fw-bold' : 'text-muted';
                    return `<span class="${roleClass} text-uppercase">${role}</span>`;
                }
            },
            {
                data: function (data) {
                    return data.studentId?.trim()
                            ? data.studentId
                            : '<span class="text-muted">N/A</span>';
                }
            },
            {
                data: function (data) {
                    return data.isVerified
                            ? '<span class="text-success fw-bold">Yes</span>'
                            : '<span class="text-danger fw-bold">No</span>';
                }
            },
            {
                data: function (data) {
                    return data.isActive
                            ? '<span class="text-success fw-bold">Active</span>'
                            : '<span class="text-danger fw-bold">Inactive</span>';
                }
            },
            {
                data: function (data) {
                    const editBtn = `
                        <a href="javascript:void(0);" class="btn btn-sm btn-edit me-2" title="Edit"
                            onclick="editModal(${data.authUserId})">
                            <i class="fa-solid fa-pencil"></i>
                        </a>`;

                    const deleteBtn = `
                        <a href="javascript:void(0);" class="btn btn-sm btn-delete me-2" title="Delete"
                            onclick="softDeleteUserById(${data.authUserId}, '${data.email}')">
                            <i class="fa-solid fa-trash"></i>
                        </a>`;

                    const reactivateBtn = `
                        <a href="javascript:void(0);" class="btn btn-sm btn-success" title="Resend Reactivation Email"
                            onclick="resendReactivationEmail('${data.email}')">
                            <i class="fa-solid fa-envelope"></i>
                        </a>`;

                    return `<div class="d-flex justify-content-center">
                        ${editBtn}
                        ${data.isActive ? deleteBtn : ''}
                        ${!data.isActive ? reactivateBtn : ''}
                    </div>`;
                },
                orderable: false,
                width: "5%",
                className: 'text-center'
            }
        ]
    });
};

// FETCH USERS
function fetchData(url, data) {
    return $.ajax({
        url: url,
        method: 'GET',
        data: data,
        dataType: 'json',
        beforeSend: () => console.log('Fetching...'),
        success: res => console.log('Fetched:', res),
        error: err => console.error('Error:', err),
        complete: () => console.log('Done.')
    });
}

async function getAllUsers() {
    try {
        const res = await fetchData('/admin/users/list');
        if (res.success) {
            initDataTable(res.users);
        } else {
            showToast(res.message || "Failed to load users.", "error");
        }
    } catch (err) {
        console.error('Error loading user data:', err);
        showToast("Server error occurred.", "error");
    }
}

// DELETE USER
function deleteData(url, id, callbacks = []) {
    return $.ajax({
        url: url,
        method: 'DELETE',
        contentType: 'application/json',
        data: id,
        success: function (response) {
            callbacks.forEach(cb => cb());
        },
        error: function (error) {
            console.error('Delete failed:', error);
        }
    });
}

async function softDeleteUserById(authUserId, email) {
    const confirmResult = await Swal.fire({
        title: 'Are you sure?',
        text: "This will deactivate the user account.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, deactivate',
        background: '#2a2f37',
        color: '#fff'
    });

    if (!confirmResult.isConfirmed)
        return;

    Swal.fire({
        title: 'Processing...',
        html: '<b>Please wait while we deactivate the user.</b>',
        allowOutsideClick: false,
        showConfirmButton: false,
        background: '#2a2f37',
        color: '#fff',
        didOpen: () => Swal.showLoading()
    });

    try {
        const response = await deleteData('/admin/users/deactivate', JSON.stringify({authUserId, email}), [getAllUsers]);
        Swal.close();

        if (response.success) {
            showToast("User has been deactivated.");
        } else {
            showToast(response.message || "Deactivation failed.", "error");
        }
    } catch (error) {
        Swal.close();
        showToast("An unexpected error occurred.", "error");
    }
}

// RESEND EMAIL
function resendReactivationEmail(email) {
    Swal.fire({
        title: 'Send Reactivation Link?',
        text: `Send a reactivation email to ${email}?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Yes, send',
        background: '#2a2f37',
        color: '#fff'
    }).then((result) => {
        if (result.isConfirmed) {
            Swal.fire({
                title: 'Sending...',
                html: 'Please wait while we send the email.',
                allowOutsideClick: false,
                showConfirmButton: false,
                background: '#2a2f37',
                color: '#fff',
                didOpen: () => {
                    Swal.showLoading();

                    $.post('/resend-reactivation', {email})
                            .done(res => {
                                Swal.close();
                                showToast(res.message || "Email sent.");
                            })
                            .fail(err => {
                                Swal.close();
                                showToast(err.responseJSON?.message || 'Failed to resend email.', 'error');
                            });
                }
            });
        }
    });
}

// EDIT MODAL
function editModal(userId) {
    $.ajax({
        url: '/admin/manage-users/fetch-user',
        method: 'GET',
        data: {userId},
        success: function (data) {
            if (!data.success) {
                showToast(data.message || "User not found.", "error");
                return;
            }

            const user = data.user;
            $('#editUserId').val(user.authUserId);
            $('#editFirstName').val(user.firstName);
            $('#editMiddleName').val(user.middleName);
            $('#editLastName').val(user.lastName);
            $('#editEmail').val(user.email).removeClass('is-invalid is-valid');
            $('#editUserModal').modal('show');
        },
        error: function () {
            showToast("Could not fetch user data.", "error");
        }
    });
}

// UPDATE FORM SUBMIT
$('#editUserForm').on('submit', function (e) {
    e.preventDefault();

    const {validateField, regex} = window.AcadalyzeValidator;

    const $firstName = $('#editFirstName');
    const $middleName = $('#editMiddleName');
    const $lastName = $('#editLastName');
    const $email = $('#editEmail');
    const authUserId = $('#editUserId').val();

    const isFirstNameValid = validateField($firstName, regex.name, 'First name must start with a capital letter and contain only letters.');
    const isLastNameValid = validateField($lastName, regex.name, 'Last name must start with a capital letter and contain only letters.');
    const isEmailValid = validateField($email, regex.email, 'Enter a valid email address.');

    // Make middle name optional
    let isMiddleNameValid = true;
    const middleNameVal = $middleName.val().trim();
    if (middleNameVal.length > 0) {
        isMiddleNameValid = validateField($middleName, regex.name, 'Middle name must start with a capital letter and contain only letters.');
    } else {
        $middleName.removeClass('is-invalid');
        $middleName.siblings('.invalid-feedback').hide();
    }

    if (!(isFirstNameValid && isMiddleNameValid && isLastNameValid && isEmailValid))
        return;

    $.ajax({
        url: '/admin/manage-users/check-email',
        method: 'GET',
        data: {
            email: $email.val().trim(),
            userId: authUserId
        },
        success: function (res) {
            if (!res.success) {
                $email.addClass('is-invalid');
                $email.siblings('.invalid-feedback').text(res.message).show();
                return;
            }

            Swal.fire({
                title: 'Confirm Update',
                text: 'Are you sure you want to update this user?',
                icon: 'question',
                showCancelButton: true,
                background: '#212529',
                color: '#fff',
                confirmButtonText: 'Yes, update',
                cancelButtonText: 'Cancel',
                customClass: {
                    confirmButton: 'btn btn-danger mx-1',
                    cancelButton: 'btn btn-secondary'
                },
                buttonsStyling: false
            }).then((result) => {
                if (!result.isConfirmed)
                    return;

                const formData = {
                    authUserId,
                    firstName: $firstName.val().trim(),
                    middleName: middleNameVal || null,
                    lastName: $lastName.val().trim(),
                    email: $email.val().trim()
                };

                $.ajax({
                    url: '/admin/manage-users/update-information',
                    method: 'PATCH',
                    contentType: 'application/json',
                    data: JSON.stringify(formData),
                    success: function (res) {
                        if (res.success) {
                            $('#editUserModal').modal('hide');
                            showToast(res.message || "User updated.");
                            getAllUsers();
                        } else {
                            showToast(res.message || "Update failed.", "error");
                        }
                    },
                    error: function () {
                        showToast("Server error during update.", "error");
                    }
                });
            });
        },
        error: function () {
            showToast("Email uniqueness check failed.", "error");
        }
    });
});

// RELOAD USERS
const reloadUserTable = (e) => {
    getAllUsers();
};

$(document).ready(function () {
    getAllUsers();
});
