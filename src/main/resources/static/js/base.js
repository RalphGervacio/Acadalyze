$(document).ready(function () {
    // Centralized regex patterns
    const regex = {
        name: /^([A-Z][a-zA-Z]{1,49})(\s[A-Z][a-zA-Z]{1,49})*$/, 
        username: /^[a-zA-Z0-9_]{5,}$/,
        email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
        password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/,
        studentId: /^\d{11}$/
    };

    // Reusable validator
    function validateField($input, pattern, errorMessage) {
        const value = $input.val().trim();
        const feedback = $input.siblings('.invalid-feedback');

        if (!pattern.test(value)) {
            $input.addClass('is-invalid');
            feedback.text(errorMessage).show();
            return false;
        } else {
            $input.removeClass('is-invalid');
            feedback.text('').hide();
            return true;
        }
    }

    function validateSelect($select, errorMessage) {
        const feedback = $select.siblings('.invalid-feedback');
        if (!$select.val()) {
            $select.addClass('is-invalid');
            feedback.text(errorMessage).show();
            return false;
        } else {
            $select.removeClass('is-invalid');
            feedback.text('').hide();
            return true;
        }
    }

    function validateCheckbox($checkbox, errorMessage) {
        const feedback = $checkbox.closest('.form-check').find('.invalid-feedback');
        if (!$checkbox.is(':checked')) {
            $checkbox.addClass('is-invalid');
            feedback.text(errorMessage).show();
            return false;
        } else {
            $checkbox.removeClass('is-invalid');
            feedback.text('').hide();
            return true;
        }
    }

    // Export regex and validators globally if needed elsewhere
    window.AcadalyzeValidator = {
        regex,
        validateField,
        validateSelect,
        validateCheckbox
    };
});
