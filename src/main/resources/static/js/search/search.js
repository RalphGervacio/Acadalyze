$(document).ready(function () {

    // ====================
    // Global Variables
    // ====================
    const $searchInput = $('#searchInput');
    const $searchDropdown = $('#searchResultsDropdown');
    const $searchButton = $('#searchForm button[type="submit"]');
    const $searchIcon = $('#searchIcon');
    const $spinnerIcon = $('#spinnerIcon');
    const $clearSearch = $('#clearSearch'); // desktop

    const $mobileSearchInput = $('#mobileSearchInput');
    const $mobileSearchDropdown = $('#mobileSearchResultsDropdown');
    const $mobileSearchButton = $('#mobileSearchForm button[type="submit"]');
    const $clearMobileSearch = $('#clearMobileSearch');

    // ====================
    // Handle Search
    // ====================
    function handleSearch(input, dropdown, isMobile = false) {
        const query = input.val().trim();
        if (!query) {
            dropdown.hide();
            return;
        }

        if (!isMobile) {
            $searchIcon.addClass('d-none');
            $spinnerIcon.removeClass('d-none');
        }

        $.ajax({
            url: '/search',
            type: 'GET',
            data: {Search: query},
            success: function (response) {
                if (response.redirect) {
                    showLoginToast();
                    return;
                }
                displaySearchResults(response, dropdown);
            },
            error: function () {
                dropdown.html('<div class="text-danger">Search failed.</div>').show();
            },
            complete: function () {
                if (!isMobile) {
                    $searchIcon.removeClass('d-none');
                    $spinnerIcon.addClass('d-none');
                }
            }
        });
    }

    // ====================
    // Display Results
    // ====================
    function displaySearchResults(data, dropdown) {
        let html = '';

        if (data.students?.length > 0) {
            html += '<h6 class="search-section-title">Students</h6><ul class="list-unstyled">';
            $.each(data.students, (_, s) => {
                html += `<li><a href="/profile/view/${s.auth_user_id}" class="dropdown-item">${s.first_name} ${s.last_name} (${s.student_id})</a></li>`;
            });
            html += '</ul><hr>';
        }

        if (data.subjects?.length > 0) {
            html += '<h6 class="search-section-title">Subjects</h6><ul class="list-unstyled">';
            $.each(data.subjects, (_, sub) => {
                html += `<li><a href="#" class="dropdown-item">${sub.subject_name}</a></li>`;
            });
            html += '</ul><hr>';
        }

        if (data.grades?.length > 0) {
            html += '<h6 class="search-section-title">Grades</h6><ul class="list-unstyled">';
            $.each(data.grades, (_, g) => {
                html += `<li><a href="#" class="dropdown-item">${g.subject_name}: ${g.grade}</a></li>`;
            });
            html += '</ul>';
        }

        if (!html) {
            html = '<div class="text-muted">No results found.</div>';
        }

        dropdown.html(html).show();
    }

    // ====================
    // Show Login Required Toast
    // ====================
    function showLoginToast() {
        const toastHtml = `
        <div class="toast align-items-center text-bg-danger border-0 position-fixed bottom-0 end-0 m-3" role="alert" aria-live="assertive" aria-atomic="true" id="loginToast" style="z-index: 9999;">
            <div class="d-flex">
                <div class="toast-body">
                    Please log in to search grades.
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>`;
        $('#loginToast').remove();
        $('body').append(toastHtml);
        new bootstrap.Toast(document.getElementById('loginToast')).show();
    }

    // ====================
    // Submit Handlers
    // ====================
    $searchButton.on('click', () => handleSearch($searchInput, $searchDropdown));
    $mobileSearchButton?.on('click', () => handleSearch($mobileSearchInput, $mobileSearchDropdown, true));

    // ====================
    // Enter Key Trigger
    // ====================
    $searchInput.on('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            $searchButton.trigger('click');
        }
    });

    $mobileSearchInput.on('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            $mobileSearchButton.trigger('click');
        }
    });

    // ====================
    // Clear Button Toggle
    // ====================
    $searchInput.on('input', function () {
        $clearSearch.toggleClass('d-none', !$searchInput.val().trim());
    });

    $clearSearch.on('click', function () {
        $searchInput.val('').focus();
        $searchDropdown.hide();
        $(this).addClass('d-none');
    });

    $mobileSearchInput.on('input', function () {
        $clearMobileSearch?.toggleClass('d-none', !$mobileSearchInput.val().trim());
    });

    $clearMobileSearch?.on('click', function () {
        $mobileSearchInput.val('').focus();
        $mobileSearchDropdown.hide();
        $(this).addClass('d-none');
    });

    // ====================
    // Hide Dropdowns on Outside Click
    // ====================
    $('body').on('click', function (e) {
        if (!$(e.target).closest('#searchForm, #searchResultsDropdown').length) {
            $searchDropdown.hide();
        }
    });

    // ====================
    // Mobile Search Modal
    // ====================
    $('#searchToggleBtn').on('click', function () {
        const modal = new bootstrap.Modal(document.getElementById('mobileSearchModal'));
        modal.show();
        setTimeout(() => $mobileSearchInput.trigger('focus'), 300);
    });

    $('#mobileSearchModal').on('hidden.bs.modal', function () {
        $('body').removeClass('modal-open');
        $('.modal-backdrop').remove();
        $mobileSearchDropdown.hide();
    });
});