// ✅ /js/messages/handleSoundNotification.js

// ✅ Function to play the notification sound
export function playNotificationSound() {
    const $audio = $('#messageSound');
    console.log('🔊 Attempting to play sound...', $audio.length, document.visibilityState);

    if ($audio.length && document.visibilityState === 'visible') {
        const audio = $audio[0];
        audio.currentTime = 0;
        audio.play().then(() => {
            console.log('✅ Sound played');
        }).catch(err => {
            console.warn('🔇 Sound blocked:', err);
        });
    }
}


// ✅ Unlock notification sound on first user interaction
$(document).ready(function () {
    $(document).one('click keydown', function () {
        const $audio = $('#messageSound');
        if ($audio.length) {
            const audio = $audio[0];
            audio.play().then(() => {
                audio.pause();
                audio.currentTime = 0;
                console.log('🔓 Notification sound unlocked.');
            }).catch(err => {
                console.warn('🔇 Still blocked:', err);
            });
        } else {
            console.warn('⚠️ #messageSound not found');
        }
    });
});

