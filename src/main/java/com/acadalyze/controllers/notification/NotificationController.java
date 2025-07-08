package com.acadalyze.controllers.notification;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.beans.admin.notification.NotificationBean;
import com.acadalyze.utils.NotificationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    NotificationService notificationService;

    @GetMapping
    public ModelAndView showNotificationPage(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return new ModelAndView("redirect:/login");
        }

        UsersBean user = (UsersBean) session.getAttribute("user");
        if (user.getAuthUserId() == null) {
            return new ModelAndView("redirect:/login");
        }

        int userId = user.getAuthUserId().intValue();
        List<NotificationBean> notifications = notificationService.getNotificationsByUserId(userId);

        ModelAndView mav = new ModelAndView("pages/notification");
        mav.addObject("notifications", notifications);
        return mav;
    }

    @GetMapping("/api")
    public void getNotifications(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        UsersBean user = (UsersBean) session.getAttribute("user");
        if (user == null || user.getAuthUserId() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        int userId = user.getAuthUserId().intValue();
        List<NotificationBean> notifications = notificationService.getNotificationsByUserId(userId);
        writeJsonResponse(response, true, "Notifications fetched successfully", notifications);
    }

    @PatchMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable int notificationId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        UsersBean user = (UsersBean) session.getAttribute("user");
        if (user == null || user.getAuthUserId() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        boolean updated = notificationService.markAsRead(notificationId);
        if (updated) {
            writeJsonResponse(response, true, "Notification marked as read", null);
        } else {
            writeJsonResponse(response, false, "Failed to mark notification as read", null);
        }
    }

    @PatchMapping("/mark-all-read")
    public void markAllAsRead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }
        UsersBean user = (UsersBean) session.getAttribute("user");
        if (user == null || user.getAuthUserId() == null) {
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        boolean updated = notificationService.markAllAsRead(user.getAuthUserId().intValue());
        if (updated) {
            writeJsonResponse(response, true, "All notifications marked as read", null);
        } else {
            writeJsonResponse(response, false, "Failed to mark all as read", null);
        }
    }

    @DeleteMapping("/{notificationId}")
    public void deleteNotification(@PathVariable int notificationId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        boolean deleted = notificationService.deleteNotification(notificationId);
        if (deleted) {
            writeJsonResponse(response, true, "Notification deleted successfully", null);
        } else {
            writeJsonResponse(response, false, "Failed to delete notification", null);
        }
    }

    @GetMapping("/unread-count")
    public void getUnreadCount(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        UsersBean user = (UsersBean) session.getAttribute("user");
        if (user == null || user.getAuthUserId() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonResponse(response, false, "Unauthorized", null);
            return;
        }

        int userId = user.getAuthUserId().intValue();
        int count = notificationService.countUnread(userId);

        writeJsonResponse(response, true, "Unread count fetched successfully", count);
    }

    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .create();

    private void writeJsonResponse(HttpServletResponse response, boolean success, String message, Object data) throws IOException {
        response.setContentType("application/json");
        var json = new JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message);
        if (data != null) {
            json.add("data", gson.toJsonTree(data));
        }
        response.getWriter().write(json.toString());
    }
}
