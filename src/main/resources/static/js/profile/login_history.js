function confirmPasswordBeforeHistory() {
    $('#confirmPasswordInput').val('');
    $('#confirmPasswordError').addClass('d-none');
    $('#passwordConfirmModal').modal('show');
}

function verifyPassword() {
    const password = $('#confirmPasswordInput').val().trim();

    if (!password) {
        $('#confirmPasswordError').text('Password is required').removeClass('d-none');
        return;
    }

    $.ajax({
        url: '/profile/verify-password',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({password}),
        beforeSend: () => {
            $('#confirmPasswordError').addClass('d-none').text('');
        },
        success: function (res) {
            if (res.success) {
                $('#passwordConfirmModal').modal('hide');
                loadLoginHistory();
            } else {
                $('#confirmPasswordError').text(res.message || 'Incorrect password.').removeClass('d-none');
            }
        },
        error: function () {
            $('#confirmPasswordError').text('Something went wrong. Please try again.').removeClass('d-none');
        }
    });
}

function loadLoginHistory() {
    $('#loginHistoryTableWrapper').addClass('d-none');
    $('#noLoginHistory').addClass('d-none').html(`
        <i class="bi bi-info-circle fs-1 mb-3"></i>
        <p>No login history found.</p>
    `);
    $('#loginHistoryModal').modal('show');

    $.ajax({
        url: '/profile/login-history',
        method: 'GET',
        success: function (res) {
            const $tableBody = $('#loginHistoryTableBody').empty();

            if (!res || res.length === 0) {
                $('#noLoginHistory').removeClass('d-none');
            } else {
                res.forEach(item => {
                    const formattedTime = new Date(item.timestamp).toLocaleString('en-US', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: true
                    });

                    const row = `
                        <tr>
                            <td>${formattedTime}</td>
                            <td>${item.ip}</td>
                            <td>${item.device}</td>
                        </tr>`;
                    $tableBody.append(row);
                });
                $('#loginHistoryTableWrapper').removeClass('d-none');
            }
        },
        error: function () {
            $('#noLoginHistory')
                    .removeClass('d-none')
                    .html(`
                    <i class="bi bi-exclamation-triangle-fill text-danger fs-4 mb-2 d-block"></i>
                    <p class="text-danger">Failed to load login history.</p>
                `);
        }
    });
}
