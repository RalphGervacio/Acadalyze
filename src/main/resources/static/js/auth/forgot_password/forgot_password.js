$(document).ready(function () {
    // Handle Forgot Password Form
    $('#forgotPasswordForm').submit(function (e) {
        e.preventDefault();
        $('#forgotEmailError').text('');

        const email = $('#forgotEmail').val().trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (!email) {
            $('#forgotEmailError').text('Email is required.');
            return;
        }

        if (!emailRegex.test(email)) {
            $('#forgotEmailError').text('Please enter a valid email address.');
            return;
        }

        Swal.fire({
            title: 'Send Password Reset Link?',
            text: `We will send a reset link to: ${email}`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#0d6efd',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, send it!',
            background: '#343a40',
            color: '#f8f9fa',
            iconColor: '#0d6efd',
            customClass: {
                popup: 'border border-secondary rounded-3 shadow',
                title: 'fs-5',
                confirmButton: 'btn btn-primary mx-1',
                cancelButton: 'btn btn-secondary'
            },
            buttonsStyling: false
        }).then((result) => {
            if (result.isConfirmed) {
                Swal.fire({
                    title: 'Sending...',
                    text: 'Please wait while we send the reset link.',
                    background: '#343a40',
                    color: '#f8f9fa',
                    iconColor: '#0d6efd',
                    allowOutsideClick: false,
                    showConfirmButton: false,
                    didOpen: () => {
                        Swal.showLoading();
                    }
                });

                $.ajax({
                    type: 'POST',
                    url: '/api/forgot-password',
                    contentType: 'application/json',
                    dataType: 'json',
                    data: JSON.stringify({email}),
                    success: function (res) {
                        $('#forgotEmailError').text('');
                        Swal.fire({
                            title: 'Success!',
                            text: res.message,
                            icon: 'success',
                            background: '#343a40',
                            color: '#f8f9fa',
                            iconColor: '#198754',
                            confirmButtonColor: '#198754',
                            customClass: {
                                popup: 'border border-secondary rounded-3 shadow',
                                title: 'fs-5',
                                confirmButton: 'btn btn-success'
                            },
                            buttonsStyling: false
                        });
                    },
                    error: function (xhr) {
                        const msg = xhr.responseJSON?.message || "Something went wrong.";
                        $('#forgotEmailError').text(msg);
                        Swal.close();
                    }
                });
            }
        });
    });

    // Autofill token in reset form
    const resetToken = new URLSearchParams(window.location.search).get('token');
    if (resetToken && $('#resetToken').length) {
        $('#resetToken').val(resetToken);
    }

    // Handle Reset Password Form
    $('#resetPasswordForm').submit(function (e) {
        e.preventDefault();

        $('#resetPasswordError').text('');

        const token = $('#resetToken').val();
        const newPassword = $('#resetPassword').val();
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,}$/;

        if (!newPassword) {
            $('#resetPasswordError').text('Password is required.');
            return;
        }

        if (!passwordRegex.test(newPassword)) {
            $('#resetPasswordError').text('Password must be at least 8 characters and include uppercase, lowercase, number, and symbol.');
            return;
        }

        Swal.fire({
            title: 'Reset Password?',
            text: 'Are you sure you want to change your password?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, reset it!',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#0d6efd',
            cancelButtonColor: '#6c757d',
            background: '#343a40',
            color: '#f8f9fa',
            iconColor: '#ffc107',
            customClass: {
                popup: 'border border-secondary rounded-3 shadow',
                title: 'fs-5',
                confirmButton: 'btn btn-primary mx-1',
                cancelButton: 'btn btn-secondary'
            },
            buttonsStyling: false
        }).then((result) => {
            if (result.isConfirmed) {
                Swal.fire({
                    title: 'Processing...',
                    text: 'Resetting your password...',
                    background: '#343a40',
                    color: '#f8f9fa',
                    allowOutsideClick: false,
                    showConfirmButton: false,
                    didOpen: () => {
                        Swal.showLoading();
                    }
                });

                $.ajax({
                    type: 'POST',
                    url: '/reset-password',
                    data: {
                        token: token,
                        newPassword: newPassword
                    },
                    dataType: 'json',
                    success: function (response) {
                        if (response.success) {
                            Swal.fire({
                                title: 'Success!',
                                text: response.message,
                                icon: 'success',
                                confirmButtonColor: '#198754',
                                background: '#343a40',
                                color: '#f8f9fa',
                                iconColor: '#198754',
                                customClass: {
                                    popup: 'border border-secondary rounded-3 shadow',
                                    title: 'fs-5',
                                    confirmButton: 'btn btn-success'
                                },
                                buttonsStyling: false
                            }).then(() => {
                                window.location.href = "/reset-success";
                            });
                        } else {
                            Swal.fire({
                                title: 'Error',
                                text: response.message,
                                icon: 'error',
                                confirmButtonColor: '#dc3545',
                                background: '#343a40',
                                color: '#f8f9fa',
                                iconColor: '#dc3545',
                                customClass: {
                                    popup: 'border border-secondary rounded-3 shadow',
                                    title: 'fs-5',
                                    confirmButton: 'btn btn-danger'
                                },
                                buttonsStyling: false
                            });
                        }
                    },
                    error: function (xhr) {
                        const msg = xhr.responseJSON?.message || "Something went wrong.";
                        Swal.fire({
                            title: 'Error',
                            text: msg,
                            icon: 'error',
                            confirmButtonColor: '#dc3545',
                            background: '#343a40',
                            color: '#f8f9fa',
                            iconColor: '#dc3545',
                            customClass: {
                                popup: 'border border-secondary rounded-3 shadow',
                                title: 'fs-5',
                                confirmButton: 'btn btn-danger'
                            },
                            buttonsStyling: false
                        });
                    }
                });
            }
        });
    });

    $('#resetPassword').on('input', function () {
        const password = $(this).val();
        const $strength = $('#passwordStrength');
        const $feedback = $('#resetPasswordError');

        if (password.trim() === '') {
            $strength.text('').removeClass();
            $feedback.show();
            return;
        }

        $feedback.hide();

        let strength = 0;
        if (password.length >= 8)
            strength++;
        if (/[A-Z]/.test(password))
            strength++;
        if (/[a-z]/.test(password))
            strength++;
        if (/\d/.test(password))
            strength++;
        if (/[^A-Za-z0-9]/.test(password))
            strength++;

        let text = '';
        let color = '';

        if (strength <= 2) {
            text = 'Weak';
            color = 'text-danger';
        } else if (strength === 3 || strength === 4) {
            text = 'Moderate';
            color = 'text-warning';
        } else {
            text = 'Strong';
            color = 'text-success';
        }

        $strength.text(text)
                .removeClass('text-danger text-warning text-success')
                .addClass(color);
    });

});
