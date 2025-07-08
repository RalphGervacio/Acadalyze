$(document).ready(function () {

    loadUserBio();
    const $profilePreview = $('#profilePreview');
    const $coverPreview = $('#coverPreview');
    const $expandedImage = $('#expandedProfileImage');
    const $bioTextarea = $('#userBio');
    const $bioActionButtons = $('#bioActionButtons');
    const $cancelBioBtn = $('#cancelBioBtn');
    let originalBio = $bioTextarea.val().trim();

    $('#profilePreview').on('error', function () {
        $(this).hide();
        $('#profileIconFallback').show();
    });

    $('#coverPreview').on('error', function () {
        $(this).hide();
        $('#coverFallback').show();
    });

    $('#uploadForm').off('submit').on('submit', function (e) {
        e.preventDefault();

        const fileInput = $('#profileImage')[0];
        const file = fileInput.files[0];

        if (!file)
            return $('#fileSizeError').text('Please select an image file.');
        if (file.size > 2 * 1024 * 1024)
            return $('#fileSizeError').text('File size must be less than 2MB.');
        if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type))
            return $('#fileSizeError').text('Only image files (JPEG, PNG, GIF, WEBP) are allowed.');

        $('#fileSizeError').text('');

        Swal.fire({
            title: 'Upload New Profile Image?',
            text: "This will replace your current image.",
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#0d6efd',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, upload it!',
            background: '#343a40',
            color: '#f8f9fa',
            iconColor: '#0d6efd'
        }).then((result) => {
            if (!result.isConfirmed)
                return;

            const $btn = $('#uploadButton');
            const $btnContent = $btn.find('.btn-content');
            let progress = 0;

            $btn.prop('disabled', true);
            $btnContent.html(`<span class="loader-text">Uploading... 0%</span>`);

            const loaderInterval = setInterval(() => {
                progress += Math.floor(Math.random() * 10) + 5;
                if (progress >= 100) {
                    progress = 100;
                    clearInterval(loaderInterval);
                    $btnContent.html(`<i class="bi bi-check-circle-fill me-1"></i>Done`);
                    setTimeout(() => {
                        $btnContent.html(`<i class="bi bi-upload me-1 upload-icon"></i>Upload`);
                        $btn.prop('disabled', false);
                    }, 1500);
                } else {
                    $btn.find('.loader-text').text(`Uploading... ${progress}%`);
                }
            }, 100);

            const formData = new FormData();
            formData.append("image", file);

            $.ajax({
                url: '/profile/upload-image',
                type: 'PATCH',
                data: formData,
                processData: false,
                contentType: false,
                success: function (res) {
                    setTimeout(() => {
                        if (res.success) {
                            showToast(res.message, 'success');
                            $('#profileImage').val('');
                            $('#profileIconFallback').hide();
                            $('#profilePreview')
                                    .attr('src', '/profile/profile-image/' + res.userId + '?' + new Date().getTime())
                                    .show();
                        } else {
                            showToast(res.message, 'error');
                        }
                        resetButton();
                    }, 1000);
                },
                error: function (xhr) {
                    const res = xhr.responseJSON;
                    showToast(res?.message || 'Something went wrong.', 'error');
                    resetButton();
                }
            });

            function resetButton() {
                $btn.prop('disabled', false);
                $btnContent.html(`<i class="bi bi-upload me-2 upload-icon"></i>Upload Image`);
            }
        });
    });

    $('#uploadCoverForm').off('submit').on('submit', function (e) {
        e.preventDefault();
        const fileInput = $('#coverImage')[0];
        const file = fileInput.files[0];

        if (!file)
            return $('#coverFileSizeError').text('Please select a cover image.');
        if (file.size > 2 * 1024 * 1024)
            return $('#coverFileSizeError').text('Cover photo must be less than 2MB.');
        if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type))
            return $('#coverFileSizeError').text('Only image files (JPEG, PNG, GIF, WEBP) are allowed.');
        $('#coverFileSizeError').text('');

        Swal.fire({
            title: 'Upload New Cover Photo?',
            text: "This will replace your current banner.",
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#0d6efd',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, upload it!',
            background: '#343a40',
            color: '#f8f9fa',
            iconColor: '#0d6efd'
        }).then((result) => {
            if (!result.isConfirmed)
                return;

            const $btn = $('#uploadCoverButton');
            const $btnContent = $btn.find('.btn-content');
            let progress = 0;
            $btn.prop('disabled', true);
            $btnContent.html(`<span class="loader-text">Uploading... 0%</span>`);

            const loaderInterval = setInterval(() => {
                progress += Math.floor(Math.random() * 10) + 5;
                if (progress >= 100) {
                    progress = 100;
                    clearInterval(loaderInterval);
                    $btnContent.html(`<i class="bi bi-check-circle-fill me-1"></i>Done`);
                    setTimeout(() => {
                        $btnContent.html(`<i class="bi bi-upload me-1 upload-icon"></i>Upload Cover`);
                        $btn.prop('disabled', false);
                    }, 1500);
                } else {
                    $btn.find('.loader-text').text(`Uploading... ${progress}%`);
                }
            }, 100);

            const formData = new FormData();
            formData.append("coverImage", file);

            $.ajax({
                url: '/profile/upload-cover',
                type: 'PATCH',
                data: formData,
                processData: false,
                contentType: false,
                success: function (res) {
                    setTimeout(() => {
                        if (res.success) {
                            showToast(res.message, 'success');
                            const newSrc = `/profile/cover-image/${res.userId}?t=${Date.now()}`;
                            $('#coverImage').val('');
                            $('#coverFallback').hide();
                            $('#coverPreview').attr('src', newSrc).show();
                        } else {
                            showToast(res.message, 'error');
                        }
                        resetButton();
                    }, 1000);
                },
                error: function (xhr) {
                    const res = xhr.responseJSON;
                    showToast(res?.message || 'Something went wrong.', 'error');
                    resetButton();
                }
            });

            function resetButton() {
                $btn.prop('disabled', false);
                $btnContent.html(`<i class="bi bi-upload me-1 upload-icon"></i>Upload Cover`);
            }
        });
    });

    const hash = window.location.hash;
    if (hash) {
        const $tabTrigger = $('button[data-bs-target="' + hash + '"]');
        if ($tabTrigger.length)
            new bootstrap.Tab($tabTrigger[0]).show();
    }

    $('#userInfoProfileImage, #profilePreview, #coverPreview').on('click', function () {
        const src = $(this).attr('src');
        if (src) {
            $expandedImage.attr('src', src);
            const modalEl = document.getElementById('imagePreviewModal');
            if (modalEl)
                new bootstrap.Modal(modalEl).show();
        }
    });

    $bioTextarea.on('input', function () {
        const currentBio = $(this).val().trim();
        currentBio !== originalBio ? $bioActionButtons.removeClass('d-none') : $bioActionButtons.addClass('d-none');
    });

    $cancelBioBtn.on('click', function () {
        $bioTextarea.val(originalBio);
        $bioActionButtons.addClass('d-none');
    });

    $('#saveBioBtn').on('click', function () {
        const bio = $('#userBio').val().trim();
        const theme = getCurrentTheme();
        const isDark = theme === 'dark';

        if (bio.length > 500)
            return showBioError('Bio should be 500 characters or less.');

        Swal.fire({
            title: 'Saving...',
            text: 'Please wait while we update your bio.',
            allowOutsideClick: false,
            allowEscapeKey: false,
            background: isDark ? '#121212' : '#ffffff',
            color: isDark ? '#f8f9fa' : '#212529',
            didOpen: () => Swal.showLoading(),
            showConfirmButton: false,
            customClass: {popup: 'rounded-3 shadow'},
            timer: 1500,
            timerProgressBar: true
        });

        setTimeout(() => {
            $.ajax({
                url: '/profile/update-bio',
                type: 'PATCH',
                data: {bio},
                success: function (res) {
                    if (res.success) {
                        showToast(res.message, 'success');
                        originalBio = bio;
                        $bioActionButtons.addClass('d-none');
                    } else {
                        showToast(res.message, 'error');
                    }
                },
                error: function (xhr) {
                    const res = xhr.responseJSON;
                    showToast(res?.message || 'Something went wrong.', 'error');
                }
            });
        }, 1500);

        function showBioError(msg) {
            showToast(msg, 'error');
        }

        function getCurrentTheme() {
            return document.documentElement.getAttribute('data-bs-theme') || 'light';
        }
    });

    function loadUserBio() {
        $.ajax({
            url: '/profile/get-bio',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    $('#userBio').val(res.bio || '');
                    originalBio = res.bio || '';
                }
            },
            error: function () {
                console.error("Failed to load bio.");
            }
        });
    }

});