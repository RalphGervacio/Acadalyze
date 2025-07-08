$(document).ready(function () {

    //----- ADD MORE PASSWORD FIELD HERE IF NEEDED -----//
    setupPasswordToggle('#loginPassword', '#toggleLoginPassword');
    setupPasswordToggle('#signuppassword', '#toggleSignUpPassword');
    setupPasswordToggle('#resetPassword', '#toggleResetPassword');

    function setupPasswordToggle(inputSelector, toggleSelector) {
        const $input = $(inputSelector);
        const $toggle = $(toggleSelector);
        const $icon = $toggle.find('i'); 

        if ($icon.length === 0)
            return; 

        $toggle.on('click', function () {
            const isPassword = $input.attr('type') === 'password';
            $input.attr('type', isPassword ? 'text' : 'password');

            $icon.removeClass('bi-eye bi-eye-slash')
                    .addClass(isPassword ? 'bi-eye' : 'bi-eye-slash');
        });

        $input.on('input', function () {
            if ($input.attr('type') === 'password') {
                $icon.removeClass('bi-eye').addClass('bi-eye-slash');
            } else {
                $icon.removeClass('bi-eye-slash').addClass('bi-eye');
            }
        });
    }

});
