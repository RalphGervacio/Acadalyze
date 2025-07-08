package com.acadalyze.controllers;

import com.acadalyze.beans.admin.manage_schedules.ScheduleBean;
import com.acadalyze.beans.admin.manage_users.RolesBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.DashboardDAO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
public class DashboardController {

    @Autowired
    DashboardDAO dashboardDAO;

    @Autowired
    JdbcTemplate jdbc;

    Gson gson = new Gson();

    @GetMapping("/Dashboard")
    public ModelAndView showHomePage(HttpSession session) {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/dashboard");
    }

    @GetMapping(value = "/dashboard/published-schedules", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Object getRecentlyPublishedSchedules(HttpSession session) {
        UsersBean user = (UsersBean) session.getAttribute("user");
        RolesBean role = user.getRole();

        Map<String, Object> json = new HashMap<>();

        try {
            List<ScheduleBean> schedules = dashboardDAO.getLatestPublishedSchedules();
            json.put("success", true);
            json.put("data", schedules);
            json.put("auth_user_id", user.getAuthUserId());
            json.put("auth_role_id", role.getAuthRoleId());
            System.out.println("Auth Role Id from Dashboard: " + role.getAuthRoleId());
            System.out.println("Auth User Id from Dashboard: " + user.getAuthUserId());
        } catch (Exception e) {
            e.printStackTrace();
            json.put("success", false);
            json.put("message", "Failed to fetch schedules.");
        }

        return gson.toJson(json);
    }

    @PatchMapping("/dashboard/unpublish-multiple")
    public void unpublishPublishedSchedules(@RequestBody Map<String, List<Long>> payload,
            HttpServletResponse response,
            HttpSession session) throws IOException {

        JsonObject res = new JsonObject();
        UsersBean user = (UsersBean) session.getAttribute("user");

        System.out.println("[DEBUG] Entered /dashboard/unpublish-multiple");

        if (user == null || user.getAuthRoleId() == null) {
            System.out.println("[WARN] Session is null or user role is null.");
            res.addProperty("success", false);
            res.addProperty("message", "Session expired or invalid role. Please log in again.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(res.toString());
            return;
        }

        long roleId = user.getAuthRoleId();
        long userId = user.getAuthUserId();

        System.out.println("[INFO] Authenticated user ID: " + userId);
        System.out.println("[INFO] Authenticated role ID: " + roleId);

        if (roleId == 3L || roleId == 4L) {
            System.out.println("[WARN] Unauthorized role attempted to unpublish schedules: roleId=" + roleId);
            res.addProperty("success", false);
            res.addProperty("message", "You are not authorized to perform this action.");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(res.toString());
            return;
        }

        try {
            List<Long> ids = payload.get("ids");
            System.out.println("[INFO] Received IDs for unpublishing: " + ids);

            if (ids == null || ids.isEmpty()) {
                System.out.println("[WARN] No schedule IDs provided.");
                res.addProperty("success", false);
                res.addProperty("message", "No schedule IDs provided.");
            } else {
                String joinedIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                System.out.println("[SQL] Updating schedules with IDs: " + joinedIds);

                int updated = jdbc.update(
                        "UPDATE subject_schedule SET published = FALSE WHERE schedule_id IN (" + joinedIds + ")"
                );

                System.out.println("[RESULT] Number of schedules unpublished: " + updated);

                if (updated > 0) {
                    res.addProperty("success", true);
                    res.addProperty("message", "Schedules successfully unpublished.");
                } else {
                    res.addProperty("success", false);
                    res.addProperty("message", "No schedules were updated.");
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Exception during schedule unpublishing:");
            e.printStackTrace();
            res.addProperty("success", false);
            res.addProperty("message", "Error occurred while unpublishing schedules.");
        }

        response.setContentType("application/json");
        response.getWriter().write(res.toString());
    }

}
