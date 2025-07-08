$(document).ready(function () {
    const regex = {
        name: /^([A-Z][a-z]+(?:[-'][A-Z][a-z]+)*)(\s[A-Z][a-z]+(?:[-'][A-Z][a-z]+)*)*(\s(Jr\.|Sr\.|[IVXLCDM]+|[2-9](nd|rd|th)))?$/,
        username: /^[a-zA-Z0-9_]{5,}$/,
        email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
        password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/,
        studentId: /^\d{11}$/
    };

    $('#loginForm').on('submit', function (e) {
        e.preventDefault();

        const username = $('#loginUsername').val().trim();
        const password = $('#loginPassword').val().trim();
        const rememberMe = $('#rememberMe').is(':checked');
        const $btn = $(this).find('button[type="submit"]');

        $('#loginError').addClass('d-none').text('');
        $('#resendVerificationWrapper').addClass('d-none');
        $('#resendStatus').text('');

        if (!username || !password) {
            $('#loginError').removeClass('d-none').text('Username and password are required!');
            return;
        }

        $btn.prop('disabled', true).text('Signing In...');

        $.ajax({
            url: '/auth/login/authenticate',
            type: 'POST',
            data: {
                userName: username,
                password: password,
                rememberMe: rememberMe
            },
            success: function (res) {
                if (res.success) {
                    window.location.href = "/Dashboard";
                } else {
                    $('#loginError').removeClass('d-none').text(res.message);
                    if (res.message === "Please verify your email before logging in.") {
                        $('#resendVerificationWrapper').removeClass('d-none');
                        $('#resendVerificationBtn').data('email', username);
                    }
                }
            },
            error: function (xhr) {
                const message = xhr.responseJSON?.message || "Login failed.";
                $('#loginError').removeClass('d-none').text(message);
            },
            complete: function () {
                $btn.prop('disabled', false).text('Sign In');
            }
        });
    });

    $('#loginUsername, #loginPassword').on('input', function () {
        $('#loginError').addClass('d-none').text('');
        $('#resendVerificationWrapper').addClass('d-none');
        $('#resendStatus').text('');
    });

    $('#resendVerificationBtn').on('click', function () {
        const email = $(this).data('email');
        $('#resendStatus').removeClass('text-success text-danger').text('Sending...');

        $.post('/resend-verification', {email: email})
                .done(function () {
                    $('#resendStatus').addClass('text-success').text('Verification email sent!');
                })
                .fail(function () {
                    $('#resendStatus').addClass('text-danger').text('Failed to resend verification email.');
                });
    });

    function validateField(input, isValid, message) {
        const feedback = input.siblings('.invalid-feedback');
        const wrapper = input.closest('.password-wrapper');

        if (!isValid) {
            input.addClass('is-invalid');
            feedback.text(message).show();

            if (input.attr('id') === 'signuppassword') {
                $('#passwordStrength').text('').removeClass('text-danger text-warning text-success');
            }

            if (wrapper.length)
                wrapper.addClass('with-validation');
        } else {
            input.removeClass('is-invalid');
            feedback.text('').hide();
            if (wrapper.length)
                wrapper.removeClass('with-validation');
        }
    }

    function validateRoleSelect(select, isValid, message) {
        const feedback = select.siblings('.invalid-feedback');
        if (!isValid) {
            select.addClass('is-invalid');
            feedback.text(message).show();
        } else {
            select.removeClass('is-invalid');
            feedback.text('').hide();
        }
    }

    function validateCheckbox(checkbox, isValid, message) {
        const feedback = checkbox.closest('.form-check').find('.invalid-feedback');
        if (!isValid) {
            checkbox.addClass('is-invalid');
            feedback.text(message).show();
        } else {
            checkbox.removeClass('is-invalid');
            feedback.text('').hide();
        }
    }

    $('#signupForm').on('submit', function (e) {
        e.preventDefault();
        let valid = true;

        const firstName = $('#firstName');
        const middleName = $('#middleName');
        const lastName = $('#lastName');
        const userName = $('#userName');
        const email = $('#email');
        const password = $('#signuppassword');
        const role = $('#roleSelect');
        const studentId = $('#studentId');
        const agree = $('#agreeCheck');

        const isStudent = role.val() === 'STUDENT';

        const isFirstNameValid = regex.name.test(firstName.val());
        const middleVal = middleName.val().trim();
        const isMiddleNameValid = middleVal === '' || regex.name.test(middleVal);
        const isLastNameValid = regex.name.test(lastName.val());
        const isUserNameValid = regex.username.test(userName.val());
        const isEmailValid = regex.email.test(email.val());
        const isPasswordValid = regex.password.test(password.val());

        validateField(firstName, isFirstNameValid, 'First letter must be capitalized');
        validateField(middleName, isMiddleNameValid, 'Must be capitalized if provided');
        validateField(lastName, isLastNameValid, 'First letter must be capitalized');
        validateField(userName, isUserNameValid, 'Min of 5 characters');
        validateField(email, isEmailValid, 'Please input a valid email format');
        validateField(password, isPasswordValid, 'Password must contain uppercase, lowercase, number, and special character.');

        if (!isFirstNameValid || !isMiddleNameValid || !isLastNameValid ||
                !isUserNameValid || !isEmailValid || !isPasswordValid) {
            valid = false;
        }

        const isRoleValid = role.val() && role.val() !== '';
        validateRoleSelect(role, isRoleValid, 'Choose a role');
        if (!isRoleValid)
            valid = false;

        if (isStudent) {
            $('#studentIdWrapper').removeClass('d-none');
            const isStudentIdValid = regex.studentId.test(studentId.val());
            validateField(studentId, isStudentIdValid, 'Studednt ID must be 11 digits only');
            if (!isStudentIdValid)
                valid = false;
        }

        if (isUserNameValid) {
            $.ajax({
                url: '/auth/check-username',
                method: 'GET',
                data: {userName: userName.val().trim()},
                success: function (res) {
                    if (!res.available) {
                        validateField(userName, false, res.message);
                        valid = false;
                    }
                },
                error: function () {
                    validateField(userName, false, 'Error checking username availability.');
                    valid = false;
                },
                async: false
            });
        }

        if (isEmailValid) {
            $.ajax({
                url: '/auth/check-email',
                method: 'GET',
                data: {email: email.val().trim()},
                success: function (res) {
                    if (!res.available) {
                        validateField(email, false, res.message);
                        valid = false;
                    }
                },
                error: function () {
                    validateField(email, false, 'Error checking email availability.');
                    valid = false;
                },
                async: false
            });
        }

        const isAgreeValid = agree.is(':checked');
        validateCheckbox(agree, isAgreeValid, 'Required');
        if (!isAgreeValid)
            valid = false;

        if (!valid)
            return;

        // AJAX with Swal2 confirmation
        Swal.fire({
            title: 'Confirm Signup',
            text: 'Are you sure the details are correct?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#0d6efd',
            confirmButtonText: 'Yes, continue',
            background: '#1e1e2f',
            color: '#f8f9fa',
            customClass: {
                popup: 'border border-secondary shadow',
                confirmButton: 'btn btn-primary',
                cancelButton: ''
            },
            didRender: () => {
                const cancelBtn = document.querySelector('.swal2-cancel');
                if (cancelBtn) {
                    cancelBtn.style.backgroundColor = '#cccecf';
                    cancelBtn.style.color = '#000';
                    cancelBtn.style.border = 'none';
                    cancelBtn.style.padding = '0.5rem 1rem';
                    cancelBtn.style.borderRadius = '0.375rem';
                }
            }
        }).then((result) => {
            console.log(result);
            if (result.isConfirmed) {
                Swal.fire({
                    title: 'Creating Account...',
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    showConfirmButton: false,
                    background: '#1e1e2f',
                    color: '#f8f9fa',
                    didOpen: () => {
                        Swal.showLoading();

                        setTimeout(() => {
                            $.ajax({
                                url: '/auth/signup/authenticate',
                                type: 'POST',
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    email: email.val(),
                                    password: password.val(),
                                    firstName: firstName.val(),
                                    middleName: middleName.val(),
                                    lastName: lastName.val(),
                                    userName: userName.val(),
                                    role: role.val(),
                                    studentId: studentId.val() || ''
                                }),
                                success: function (res) {
                                    Swal.close();
                                    if (res.success) {
                                        Swal.fire({
                                            icon: 'success',
                                            title: 'Account Created!',
                                            text: res.message || 'Please check your email to verify your account.',
                                            background: '#1e1e2f',
                                            color: '#f8f9fa',
                                            confirmButtonColor: '#0d6efd',
                                            confirmButtonText: 'OK'
                                        });
                                    }
                                },
                                error: function (xhr) {
                                    Swal.close();
                                    const message = xhr.responseJSON?.message || "Signup failed.";
                                    const fieldMap = {
                                        "First": "#firstName",
                                        "Middle": "#middleName",
                                        "Last": "#lastName",
                                        "Password": "#password",
                                        "Email": "#email",
                                        "Username": "#userName",
                                        "Student": "#studentId",
                                        "Role": "#roleSelect"
                                    };

                                    let matched = false;
                                    for (const key in fieldMap) {
                                        if (message.includes(key)) {
                                            const field = $(fieldMap[key]);
                                            if (field.is('select')) {
                                                validateRoleSelect(field, false, message);
                                            } else {
                                                validateField(field, false, message);
                                            }
                                            matched = true;
                                            break;
                                        }
                                    }

                                    if (!matched) {
                                        Swal.fire({
                                            icon: 'error',
                                            title: 'Error',
                                            text: message,
                                            background: '#1e1e2f',
                                            color: '#f8f9fa',
                                            confirmButtonColor: '#dc3545'
                                        });
                                    }
                                }
                            });
                        }, 1500);
                    }
                });
            }
        });

    });

    $('#roleSelect').on('change', function () {
        validateRoleSelect($(this), true, '');

        if ($(this).val() === 'STUDENT') {
            $('#studentIdWrapper').removeClass('d-none');
        } else {
            $('#studentIdWrapper').addClass('d-none');
            $('#studentId').removeClass('is-invalid');
            $('#studentId').siblings('.invalid-feedback').text('').hide();
        }
    });

    $('#agreeCheck').on('change', function () {
        if ($(this).is(':checked')) {
            validateCheckbox($(this), true, '');
        }
    });

    function displayPasswordStrength(password) {
        const $passwordField = $('#signuppassword');
        const $strength = $('#passwordStrength');
        const $feedback = $passwordField.siblings('.invalid-feedback');

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
    }

    $('#signuppassword').on('input', function () {
        displayPasswordStrength($(this).val());
    });

});
