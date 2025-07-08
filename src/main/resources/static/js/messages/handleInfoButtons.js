// ✅ File: /js/messages/handleInfoButtons.js

/**
 * Redirects to the profile page of the selected user
 * @param {number} userId - The ID of the selected user
 */
export function handleSeeProfile(userId) {
    if (!userId || isNaN(userId))
        return;

    window.open(`/profile/view/${userId}`, '_blank');
}

