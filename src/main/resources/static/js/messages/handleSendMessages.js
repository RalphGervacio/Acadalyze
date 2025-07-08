// ✅ WebSocket-Only Message Send Handler
// File: /js/messages/handleSendMessages.js

export function initSendMessageHandler(socket) {
    $('#sendMessageForm').on('submit', async function (e) {
        e.preventDefault();

        const receiverId = $('#selectedUserId').val();
        const content = $('#messageInput').val().trim();
        const attachment = $('#messageAttachment')[0].files[0];

        if (!receiverId || (!content && !attachment)) {
            console.warn('⚠️ No content or attachment to send.');
            return;
        }

        const formData = new FormData();
        formData.append('receiverId', receiverId);
        if (content)
            formData.append('content', content);
        if (attachment)
            formData.append('attachment', attachment);

        try {
            const hasAttachment = !!attachment;

            // ✅ Only show loader if sending attachment
            if (hasAttachment) {
                $('#uploadProgressWrapper').show();
                $('#uploadProgressBar').css('width', '0%');
            }

            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/messages/send', true);

            // ✅ Disable send button before sending
            $('#sendButton').prop('disabled', true);

            xhr.upload.onprogress = function (e) {
                if (e.lengthComputable) {
                    const percent = (e.loaded / e.total) * 100;
                    $('#uploadProgressBar').css('width', `${percent}%`);
                }
            };

            xhr.onload = function () {
                if (hasAttachment)
                    $('#uploadProgressWrapper').hide();
                $('#sendButton').prop('disabled', false);

                if (xhr.status === 200) {
                    const res = JSON.parse(xhr.responseText);
                    if (res.success) {
                        $('#messageInput').val('');
                        $('#messageAttachment').val('');
                        $('#attachmentPreview').addClass('d-none');
                        $('#attachmentPreviewContent').empty();

                        // 🔊 Play send message sound
                        const sendSound = document.getElementById('sendmessageSound');
                        if (sendSound) {
                            sendSound.currentTime = 0;
                            sendSound.play().catch(err => console.warn('🔇 Cannot play send sound:', err));
                        }

                    } else {
                        console.warn('⚠️ Failed to send message:', res.message);
                    }
                } else {
                    console.error('❌ Error sending message:', xhr.status);
                }
            };

            xhr.onerror = function () {
                if (hasAttachment)
                    $('#uploadProgressWrapper').hide();
                // ✅ Re-enable send button on error
                $('#sendButton').prop('disabled', false);
                console.error('❌ Network error while sending message');
            };

            xhr.send(formData);
        } catch (err) {
            $('#uploadProgressWrapper').hide();
            // ✅ Re-enable send button on exception
            $('#sendButton').prop('disabled', false);
            console.error('❌ Failed to send message:', err);
        }
    });

    // ✅ Attachment cancel button
    $('.remove-attachment').on('click', function () {
        $('#messageAttachment').val('');
        $('#attachmentPreview').addClass('d-none');
        $('#attachmentPreviewContent').empty();
    });

    // ✅ Show attachment preview
    $('#messageAttachment').on('change', function () {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                let previewHtml = file.type.startsWith('image/')
                        ? `<img src="${e.target.result}" class="img-fluid rounded">`
                        : `<div class="small fw-bold text-muted">${file.name}</div>`;
                $('#attachmentPreviewContent').html(previewHtml);
                $('#attachmentPreview').removeClass('d-none');
            };
            reader.readAsDataURL(file);
        }
    });
}
