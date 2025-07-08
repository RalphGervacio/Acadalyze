package com.acadalyze.utils;

import com.acadalyze.beans.admin.notification.NotificationBean;
import com.acadalyze.dao.notification.NotificationDAO;
import com.google.gson.JsonObject;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Ralph Gervacio
 */
@Service
public class NotificationService {

    @Autowired
    NotificationDAO notificationDAO;

    public boolean notifyCourseEnrollment(int userId, Long courseId, String courseCode, String courseTitle, String courseDescription) {
        NotificationBean notification = new NotificationBean();
        notification.setUserId(userId);
        notification.setType("COURSE_ENROLLMENT");
        notification.setTitle("Enrolled in Course");

        String message = String.format(
                "You have been enrolled in the course:\nCourse Code: %s\nCourse Title: %s\nDescription: %s",
                courseCode, courseTitle, courseDescription
        );
        notification.setMessage(message);

        notification.setUrl("/notifications");

        notification.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        notification.setReadStatus(false);

        JsonObject extra = new JsonObject();
        extra.addProperty("courseId", courseId);
        extra.addProperty("courseCode", courseCode);
        extra.addProperty("courseTitle", courseTitle);
        extra.addProperty("courseDescription", courseDescription);
        notification.setExtraData(extra.toString());

        return notificationDAO.insert(notification);
    }

    //FOR BULK DELETITION NOTIFICATION
    public boolean notifyCourseEnrollmentRemoved(int userId, Long courseId, String courseCode, String courseTitle, String courseDescription) {
        NotificationBean notification = new NotificationBean();
        notification.setUserId(userId);
        notification.setType("COURSE_ENROLLMENT_REMOVED");
        notification.setTitle("Enrollment Removed from Course");

        String message = String.format(
                "Your enrollment in the course has been removed:\nCourse Code: %s\nCourse Title: %s\nDescription: %s",
                courseCode, courseTitle, courseDescription
        );
        notification.setMessage(message);

        notification.setUrl("/notifications");
        notification.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        notification.setReadStatus(false);

        JsonObject extra = new JsonObject();
        extra.addProperty("courseId", courseId);
        extra.addProperty("courseCode", courseCode);
        extra.addProperty("courseTitle", courseTitle);
        extra.addProperty("courseDescription", courseDescription);
        notification.setExtraData(extra.toString());

        return notificationDAO.insert(notification);
    }

    public boolean notifyCourseRemoval(int userId, Long courseId, String courseCode, String courseTitle, String courseDescription) {
        NotificationBean notification = new NotificationBean();
        notification.setUserId(userId);
        notification.setType("COURSE_REMOVAL");
        notification.setTitle("Removed from Course");

        String message = String.format(
                "Your enrollment in the course has been removed:\nCourse Code: %s\nCourse Title: %s\nDescription: %s",
                courseCode, courseTitle, courseDescription
        );
        notification.setMessage(message);

        notification.setUrl("/notifications");

        notification.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        notification.setReadStatus(false);

        JsonObject extra = new JsonObject();
        extra.addProperty("courseId", courseId);
        extra.addProperty("courseCode", courseCode);
        extra.addProperty("courseTitle", courseTitle);
        extra.addProperty("courseDescription", courseDescription);
        notification.setExtraData(extra.toString());

        return notificationDAO.insert(notification);
    }

    public boolean notifyProfilePhotoUpdate(int userId) {
        NotificationBean notification = new NotificationBean();
        notification.setUserId(userId);
        notification.setType("PROFILE_PHOTO_UPDATE");
        notification.setTitle("Profile Updated");
        notification.setMessage("Your profile or cover photo has been updated.");
        notification.setUrl("/profile");
        notification.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        notification.setReadStatus(false);
        notification.setExtraData("{}");

        return notificationDAO.insert(notification);
    }

    public boolean notifyBioUpdate(int userId, String newBio) {
        NotificationBean notification = new NotificationBean();
        notification.setUserId(userId);
        notification.setType("BIO_UPDATE");
        notification.setTitle("Bio Updated");
        notification.setMessage("You updated your bio: " + newBio.substring(0, Math.min(newBio.length(), 100)) + "...");
        notification.setUrl("/profile");
        notification.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        notification.setReadStatus(false);

        JsonObject extra = new JsonObject();
        extra.addProperty("newBio", newBio);
        notification.setExtraData(extra.toString());

        return notificationDAO.insert(notification);
    }

    public boolean notifyAllUsersSchedulePublished(String publishedBy) {
        List<Integer> userIds = notificationDAO.getAllUserIds();

        Timestamp now = new Timestamp(System.currentTimeMillis());

        for (int userId : userIds) {
            NotificationBean notification = new NotificationBean();
            notification.setUserId(userId);
            notification.setType("SCHEDULE_PUBLISH");
            notification.setTitle("Schedules Updated");
            notification.setMessage("New class schedules have been published. Please check your dashboard.");
            notification.setUrl("/Dashboard");
            notification.setCreatedAt(now);
            notification.setReadStatus(false);

            JsonObject extra = new JsonObject();
            extra.addProperty("publishedBy", publishedBy);
            extra.addProperty("publishedAt", now.toString());
            notification.setExtraData(extra.toString());

            notificationDAO.insert(notification);
        }

        return true;
    }

    public List<NotificationBean> getNotificationsByUserId(int userId) {
        return notificationDAO.getNotificationsByUserId(userId);
    }

    public boolean markAsRead(int notificationId) {
        return notificationDAO.markAsRead(notificationId);
    }

    public boolean markAllAsRead(int userId) {
        return notificationDAO.markAllAsRead(userId);
    }

    public boolean deleteNotification(int notificationId) {
        return notificationDAO.deleteNotification(notificationId);
    }

    public int countUnread(int userId) {
        return notificationDAO.countUnread(userId);
    }
}
