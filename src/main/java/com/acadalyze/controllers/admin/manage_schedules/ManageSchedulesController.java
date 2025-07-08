package com.acadalyze.controllers.admin.manage_schedules;

import com.acadalyze.beans.admin.manage_schedules.ScheduleBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.DropdownDAO;
import com.acadalyze.dao.admin.manage_shedules.SchedulesDAO;
import com.acadalyze.utils.NotificationService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
@RequestMapping("/admin")
public class ManageSchedulesController {

    @Autowired
    SchedulesDAO scheduleDAO;

    @Autowired
    DropdownDAO dropdownDAO;

    @Autowired
    NotificationService notificationService;

    @GetMapping("/schedules")
    public ModelAndView showManageSchedulesPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/admin/manage_schedules/manage_schedules");
    }

    @GetMapping("/schedules/list")
    public void listSchedules(@RequestParam(value = "courseId", required = false) Long courseId,
            HttpServletResponse response) throws IOException {
        List<ScheduleBean> schedules;

        if (courseId != null) {
            schedules = scheduleDAO.getSchedulesFilterByCourse(courseId);
        } else {
            schedules = scheduleDAO.getAllSchedules();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", true);
        jsonResponse.put("data", schedules);

        Gson gson = new Gson();
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }

    @GetMapping("/schedules/find/{id}")
    public void findScheduleById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        try {
            ScheduleBean schedule = scheduleDAO.findById(id);
            writeJsonResponse(response, true, "Schedule found", schedule);
        } catch (Exception e) {
            writeJsonResponse(response, false, "Schedule not found: " + e.getMessage(), null);
        }
    }

    @PostMapping("/schedules/add")
    public void addSchedule(@RequestBody ScheduleBean bean, HttpServletResponse response) throws IOException {
        try {
            int rows = scheduleDAO.insertSchedule(bean);
            boolean success = rows > 0;
            writeJsonResponse(response, success, success ? "Schedule added successfully." : "Failed to add schedule.", null);
        } catch (Exception e) {
            writeJsonResponse(response, false, "Error: " + e.getMessage(), null);
        }
    }

    @PatchMapping("/schedules/update")
    public void updateSchedule(@RequestBody Map<String, Object> body, HttpServletResponse response) throws IOException {
        Long scheduleId = Long.parseLong(body.get("scheduleId").toString());
        String dayOfWeek = (String) body.get("dayOfWeek");
        String startTime = (String) body.get("startTime");
        String endTime = (String) body.get("endTime");
        String room = (String) body.get("room");
        String section = (String) body.get("section");

        JsonObject res = new JsonObject();
        try {
            int result = scheduleDAO.updateScheduleTime(scheduleId, dayOfWeek, startTime, endTime, room, section);
            res.addProperty("success", result > 0);
            res.addProperty("message", result > 0 ? "Schedule updated." : "No record updated.");
        } catch (Exception e) {
            res.addProperty("success", false);
            res.addProperty("message", "Error: " + e.getMessage());
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(new Gson().toJson(res));
        out.flush();
    }

    @DeleteMapping("/schedules/delete")
    public void deleteSchedule(HttpServletRequest req, HttpServletResponse response) throws IOException {
        try {
            Long scheduleId = Long.valueOf(req.getParameter("scheduleId"));
            int rows = scheduleDAO.deleteSchedule(scheduleId);
            boolean success = rows > 0;
            writeJsonResponse(response, success, success ? "Schedule deleted." : "Failed to delete schedule.", null);
        } catch (Exception e) {
            writeJsonResponse(response, false, "Error: " + e.getMessage(), null);
        }
    }

    @PostMapping("/schedules/publish")
    public void publishSchedules(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject res = new JsonObject();

        try {
            UsersBean sessionUser = (UsersBean) request.getSession().getAttribute("user");

            if (sessionUser == null) {
                res.addProperty("success", false);
                res.addProperty("message", "Session expired. Please log in again.");
                response.setContentType("application/json");
                response.getWriter().write(res.toString());
                return;
            }

            int updatedCount = scheduleDAO.publishAllUnpublishedSchedules();

            if (updatedCount > 0) {
                String fullName = sessionUser.getFirstName()
                        + (sessionUser.getMiddleName() != null && !sessionUser.getMiddleName().isBlank()
                        ? " " + sessionUser.getMiddleName() : "")
                        + " " + sessionUser.getLastName();

                boolean notified = notificationService.notifyAllUsersSchedulePublished(fullName);

                if (notified) {
                    res.addProperty("success", true);
                    res.addProperty("message", "Schedules successfully published and all students have been notified.");
                } else {
                    res.addProperty("success", false);
                    res.addProperty("message", "Schedules published, but failed to notify users.");
                }
            } else {
                res.addProperty("success", false);
                res.addProperty("message", "No unpublished schedules found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.addProperty("success", false);
            res.addProperty("message", "Error while publishing schedules.");
        }

        response.setContentType("application/json");
        response.getWriter().write(res.toString());
    }

    @GetMapping(value = "/schedules/unpublished", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getUnpublishedSchedules(HttpSession session) {
        Map<String, Object> res = new HashMap<>();

        try {
            UsersBean user = (UsersBean) session.getAttribute("user");
            if (user == null) {
                res.put("success", false);
                res.put("message", "Session expired. Please log in again.");
                return res;
            }

            List<ScheduleBean> unpublished = scheduleDAO.getUnpublishedSchedules();

            res.put("success", true);
            res.put("data", unpublished);
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Error fetching unpublished schedules.");
        }

        return res;
    }

    @GetMapping("/schedules/subjects")
    public void getSubjects(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = dropdownDAO.getSubjects();
        writeJsonResponse(response, true, "Subjects fetched", list);
    }

    @GetMapping("/schedules/courses")
    public void getCourses(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = dropdownDAO.getCourses();
        writeJsonResponse(response, true, "Courses fetched", list);
    }

    @GetMapping("/schedules/subjects/by-course")
    public void getSubjectsByCourseId(@RequestParam("courseId") Long courseId, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = dropdownDAO.getSubjectsByCourseId(courseId);
        writeJsonResponse(response, true, "Filtered subjects fetched", list);
    }

    @GetMapping("/schedules/instructors")
    public void getInstructorDropdown(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> instructors = dropdownDAO.getInstructors();
        writeJsonResponse(response, true, "Instructors fetched", instructors);
    }

    private void writeJsonResponse(HttpServletResponse response, boolean success, String message, Object data) throws IOException {
        response.setContentType("application/json");
        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message);
        if (data != null) {
            json.add("data", new com.google.gson.Gson().toJsonTree(data));
        }
        response.getWriter().write(json.toString());
    }
}
