// ==========================
// AJAX UTILITY FUNCTIONS
// ==========================
function fetchData(url, params) {
    const request = $.ajax({
        url: url,
        method: 'GET',
        data: params,
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

function postData(url, data = {}, callbacks = []) {
    const request = $.ajax({
        url: url,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        dataType: 'json',
        beforeSend: function () {
            console.log('Request is pending...');
        },
        success: function (response) {
            if (callbacks.length !== 0) {
                callbacks.forEach((callback) => {
                    callback();
                });
            }
            return response;
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

function patchData(url, data = {}, callbacks = []) {
    const request = $.ajax({
        url: url,
        method: 'PATCH',
        contentType: 'application/json',
        data: JSON.stringify(data),
        dataType: 'json',
        beforeSend: function () {
            console.log('Request is pending...');
        },
        success: function (response) {
            if (callbacks.length !== 0) {
                callbacks.forEach((callback) => {
                    callback();
                });
            }
            return response;
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

function deleteData(url, data = {}, callbacks = []) {
    const request = $.ajax({
        url: url,
        method: 'DELETE',
        data: data,
        dataType: 'json',
        beforeSend: function () {
            console.log('Request is pending...');
        },
        success: function (response) {
            if (callbacks.length !== 0) {
                callbacks.forEach((callback) => {
                    callback();
                });
            }
            return response;
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