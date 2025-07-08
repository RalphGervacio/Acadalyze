$(document).ready(function () {
    const {regex, validateField} = window.AcadalyzeValidator;

    $('#resendForm').on('submit', function (e) {
        e.preventDefault();

        const $email = $('#email');
        const email = $email.val().trim();
        const isEmailValid = validateField($email, regex.email, 'Please enter a valid email address.');

        if (!isEmailValid) {
            Swal.fire({
                icon: 'error',
                title: '<span class="text-danger fw-bold">Oops...</span>',
                html: '<p class="text-white">Invalid email format.</p>',
                background: '#212529',
                confirmButtonText: 'OK',
                buttonsStyling: false,
                customClass: {
                    confirmButton: 'btn btn-danger px-4 py-2 fw-bold',
                    popup: 'p-4 rounded-3 shadow-lg',
                    title: 'mb-2',
                    htmlContainer: 'mb-3'
                }
            });
            return;
        }

        Swal.fire({
            title: '<span class="text-light">Sending...</span>',
            background: '#212529',
            allowOutsideClick: false,
            showConfirmButton: false,
            customClass: {
                popup: 'p-4 rounded-3 shadow-lg',
                title: 'mb-3'
            },
            didOpen: () => {
                Swal.showLoading();

                const style = document.createElement('style');
                style.innerHTML = `
            .swal2-loader {
                border-color: #eb1616 !important;
                border-top-color: transparent !important;
            }
        `;
                document.head.appendChild(style);
            }
        });

        $.ajax({
            url: '/resend-reactivation',
            method: 'POST',
            data: {email},
            success: function (data) {
                Swal.close();
                Swal.fire({
                    icon: data.success ? 'success' : 'error',
                    title: data.success
                            ? '<span class="text-success fw-bold">Success!</span>'
                            : '<span class="text-danger fw-bold">Failed</span>',
                    html: `<p class="text-white">${data.message}</p>`,
                    background: '#212529',
                    confirmButtonText: 'OK',
                    buttonsStyling: false,
                    customClass: {
                        confirmButton: `btn ${data.success ? 'btn-success' : 'btn-danger'} px-4 py-2 fw-bold`,
                        popup: 'p-4 rounded-3 shadow-lg',
                        title: 'mb-2',
                        htmlContainer: 'mb-3'
                    }
                });
            },
            error: function () {
                Swal.close();
                Swal.fire({
                    icon: 'error',
                    title: '<span class="text-danger fw-bold">Error</span>',
                    html: '<p class="text-white">Unexpected error occurred. Please try again later.</p>',
                    background: '#212529',
                    confirmButtonText: 'OK',
                    buttonsStyling: false,
                    customClass: {
                        confirmButton: 'btn btn-danger px-4 py-2 fw-bold',
                        popup: 'p-4 rounded-3 shadow-lg',
                        title: 'mb-2',
                        htmlContainer: 'mb-3'
                    }
                });
            }
        });
    });
});
