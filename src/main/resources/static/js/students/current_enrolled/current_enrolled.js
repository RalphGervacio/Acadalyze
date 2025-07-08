//FOR FETCHING DATATABLE
function fetchData(url) {
    const request = $.ajax({
        url: url,
        method: 'GET',
        dataType: 'json',
        beforeSend: function () {
            console.log('Request is pending...');
        },
        success: function (response) {
            console.log('Request successful:', response);
        },
        error: function (error) {
            console.error('Request failed:', error);
        },
        complete: function () {
            console.log('Request has completed (either fulfilled or rejected).');
        }
    });
    return request;
}

async function getAllCurrentEnrolled() {
    try {
        const response = await fetchData('/student/current-enrolled/list');
        if (response.success) {
            initCurrentEnrolledDataTable(response.data);
        } else {
            showErrorMsg(response.message);
        }
    } catch (error) {
        console.error('Failed to load current enrolled:', error);
    }
}

const initCurrentEnrolledDataTable = (current_enrolled) => {
    $('#dt_current_enrolled').DataTable({
        bAutoWidth: false,
        destroy: true,
        processing: true,
        order: [[2, 'desc']],
        deferRender: true,
        responsive: true,
        data: current_enrolled,
        columns: [
            {data: 'courseCode'},
            {data: 'courseTitle', title: 'Course Title'},
            {data: 'enrolledAtFormatted', title: 'Enrolled At'}
        ]
    });
};


//ON CLICK FUNCTION FOR RELOAD
const reloadCurrentEnrolledTable = (e) => {
    getAllCurrentEnrolled();
};

$(document).ready(function () {
    getAllCurrentEnrolled();
});