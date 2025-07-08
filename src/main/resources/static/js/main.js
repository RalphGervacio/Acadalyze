$(document).ready(function () {
    "use strict";

    // Spinner Loader
    function initializeSpinner() {
        setTimeout(function () {
            var spinnerElement = $('#spinner');
            if (spinnerElement.length > 0) {
                spinnerElement.removeClass('show');
            }
        }, 1);
    }
    initializeSpinner();

    // Back to Top Button
    $(window).scroll(function () {
        if ($(this).scrollTop() > 300) {
            $('.back-to-top').fadeIn('slow');
        } else {
            $('.back-to-top').fadeOut('slow');
        }
    });

    $('.back-to-top').click(function (event) {
        event.preventDefault();
        $('html, body').animate({scrollTop: 0}, 1500, 'easeInOutExpo');
    });

    // Sidebar Toggler
    $('.sidebar-toggler').click(function (event) {
        event.preventDefault();
        $('.sidebar, .content').toggleClass("open");
    });

    // Progress Bar Animation
    $('.pg-bar').waypoint(function () {
        $('.progress .progress-bar').each(function () {
            var value = $(this).attr("aria-valuenow");
            $(this).css("width", value + '%');
        });
    }, {offset: '80%'});

    // Calendar Initialization
    if ($('#calender').length > 0) {
        $('#calender').datetimepicker({
            inline: true,
            format: 'L'
        });
    }

    // Testimonials Carousel
    if ($(".testimonial-carousel").length > 0) {
        $(".testimonial-carousel").owlCarousel({
            autoplay: true,
            smartSpeed: 1000,
            items: 1,
            dots: true,
            loop: true,
            nav: false
        });
    }

    // Chart.js Defaults
    Chart.defaults.color = "#6C7293";
    Chart.defaults.borderColor = "#000000";

    // Chart Initializer Utility
    function initializeChart(chartId, chartConfig) {
        if ($("#" + chartId).length > 0) {
            var context = $("#" + chartId).get(0).getContext("2d");
            return new Chart(context, chartConfig);
        }
    }

    // Worldwide Sales Chart
    initializeChart("worldwide-sales", {
        type: "bar",
        data: {
            labels: ["2016", "2017", "2018", "2019", "2020", "2021", "2022"],
            datasets: [
                {
                    label: "USA",
                    data: [15, 30, 55, 65, 60, 80, 95],
                    backgroundColor: "rgba(235, 22, 22, .7)"
                },
                {
                    label: "UK",
                    data: [8, 35, 40, 60, 70, 55, 75],
                    backgroundColor: "rgba(235, 22, 22, .5)"
                },
                {
                    label: "AU",
                    data: [12, 25, 45, 55, 65, 70, 60],
                    backgroundColor: "rgba(235, 22, 22, .3)"
                }
            ]
        },
        options: {responsive: true}
    });

    // Sales & Revenue Chart
    initializeChart("salse-revenue", {
        type: "line",
        data: {
            labels: ["2016", "2017", "2018", "2019", "2020", "2021", "2022"],
            datasets: [
                {
                    label: "Sales",
                    data: [15, 30, 55, 45, 70, 65, 85],
                    backgroundColor: "rgba(235, 22, 22, .7)",
                    fill: true
                },
                {
                    label: "Revenue",
                    data: [99, 135, 170, 130, 190, 180, 270],
                    backgroundColor: "rgba(235, 22, 22, .5)",
                    fill: true
                }
            ]
        },
        options: {responsive: true}
    });

    // Line Chart
    initializeChart("line-chart", {
        type: "line",
        data: {
            labels: [50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150],
            datasets: [{
                    label: "Sales",
                    fill: false,
                    backgroundColor: "rgba(235, 22, 22, .7)",
                    data: [7, 8, 8, 9, 9, 9, 10, 11, 14, 14, 15]
                }]
        },
        options: {responsive: true}
    });

    // Bar Chart
    initializeChart("bar-chart", {
        type: "bar",
        data: {
            labels: ["Italy", "France", "Spain", "USA", "Argentina"],
            datasets: [{
                    backgroundColor: [
                        "rgba(235, 22, 22, .7)",
                        "rgba(235, 22, 22, .6)",
                        "rgba(235, 22, 22, .5)",
                        "rgba(235, 22, 22, .4)",
                        "rgba(235, 22, 22, .3)"
                    ],
                    data: [55, 49, 44, 24, 15]
                }]
        },
        options: {responsive: true}
    });

    // Pie Chart
    initializeChart("pie-chart", {
        type: "pie",
        data: {
            labels: ["Italy", "France", "Spain", "USA", "Argentina"],
            datasets: [{
                    backgroundColor: [
                        "rgba(235, 22, 22, .7)",
                        "rgba(235, 22, 22, .6)",
                        "rgba(235, 22, 22, .5)",
                        "rgba(235, 22, 22, .4)",
                        "rgba(235, 22, 22, .3)"
                    ],
                    data: [55, 49, 44, 24, 15]
                }]
        },
        options: {responsive: true}
    });

    // Doughnut Chart
    initializeChart("doughnut-chart", {
        type: "doughnut",
        data: {
            labels: ["Italy", "France", "Spain", "USA", "Argentina"],
            datasets: [{
                    backgroundColor: [
                        "rgba(235, 22, 22, .7)",
                        "rgba(235, 22, 22, .6)",
                        "rgba(235, 22, 22, .5)",
                        "rgba(235, 22, 22, .4)",
                        "rgba(235, 22, 22, .3)"
                    ],
                    data: [55, 49, 44, 24, 15]
                }]
        },
        options: {responsive: true}
    });

    // Theme Toggle
    const $html = $('html');
    const $slider = $('#themeSlider');
    const $btn = $('#themeToggleBtn');

    const storedTheme = localStorage.getItem('theme') || 'light';
    $html.attr('data-bs-theme', storedTheme);
    if (storedTheme === 'dark')
        $slider.addClass('dark');

    $btn.on('click', function (e) {
        e.stopPropagation();
        const isDark = $html.attr('data-bs-theme') === 'dark';
        const newTheme = isDark ? 'light' : 'dark';
        $html.attr('data-bs-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        $slider.toggleClass('dark');
    });

    // Active nav link
    const currentPath = window.location.pathname;
    document.querySelectorAll('.navbar-nav .nav-link').forEach(link => {
        const linkPath = link.getAttribute('href');
        if (linkPath && currentPath === linkPath) {
            document.querySelectorAll('.navbar-nav .nav-link').forEach(l => l.classList.remove('active'));
            link.classList.add('active');
        }
    });

    // Global Modals
    $(document).on('hide.bs.modal', '.modal', function () {
        if (document.activeElement)
            document.activeElement.blur();
    });

    $(document).on('shown.bs.modal', '.modal', function () {
        $(this).find('input:not([type=hidden]):visible, textarea:visible, select:visible').first().trigger('focus');
    });

    // Global Toast
    window.showToast = function (message, type = 'success') {
        const isDark = document.documentElement.getAttribute('data-bs-theme') === 'dark';
        Swal.fire({
            toast: true,
            position: 'top-end',
            icon: type,
            title: message,
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true,
            background: isDark ? '#f8fafc' : '#1c1f26',
            color: isDark ? '#2a2a2a' : '#e2e8f0'
        });
    };
});