package com.acadalyze.controllers.admin.manage_enrollments;

import com.acadalyze.beans.admin.manage_courses.CoursesBean;
import com.acadalyze.beans.admin.manage_enrollments.EnrollmentsBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.DropdownDAO;
import com.acadalyze.dao.admin.manage_courses.CoursesDAO;
import com.acadalyze.dao.admin.manage_enrollments.EnrollmentsDAO;
import com.acadalyze.utils.NotificationService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@RestController
@RequestMapping(value = "/admin")
public class ManageEnrollmentsController {

    @Autowired
    DropdownDAO dropdownDao;

    @Autowired
    EnrollmentsDAO enrollmentsDAO;

    @Autowired
    CoursesDAO coursesDAO;

    @Autowired
    NotificationService notificationService;

    @GetMapping("/enrollments")
    public ModelAndView showManageEnrollmentsPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/admin/manage_enrollments/manage_enrollments");
    }

    @GetMapping("/enrollments/list")
    public void listEnrollments(@RequestParam(required = false) String studentId, HttpServletResponse response) throws IOException {
        List<EnrollmentsBean> enrollments;

        if (studentId != null && !studentId.isEmpty()) {
            enrollments = enrollmentsDAO.getEnrollmentsByStudentId(studentId);
        } else {
            enrollments = enrollmentsDAO.getAllEnrollments();
        }

        writeJsonResponse(response, true, "Enrollments fetched", enrollments);
    }

    @GetMapping("/enrollments/students")
    public void getStudents(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> students = dropdownDao.getStudents();
        writeJsonResponse(response, true, "Students fetched", students);
    }

    @GetMapping("/enrollments/students/with-enrollments")
    public void getStudentsWithEnrollments(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> students = dropdownDao.getStudentsWithEnrollments();
        writeJsonResponse(response, true, "Students with enrollments fetched", students);
    }

    @GetMapping("/enrollments/courses")
    public void getCourses(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> courses = dropdownDao.getCourses();
        writeJsonResponse(response, true, "Courses fetched", courses);
    }

    @PostMapping("/enrollments/add")
    public void enrollStudent(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        try {
            Long authUserId = Long.valueOf(payload.get("authUserId").toString());
            Long courseId = Long.valueOf(payload.get("courseId").toString());

            if (enrollmentsDAO.isStudentEnrolled(authUserId)) {
                writeJsonResponse(response, false, "Student is already enrolled in a course this semester.", null);
                return;
            }

            int rows = enrollmentsDAO.enrollStudent(authUserId, courseId);

            if (rows > 0) {
                CoursesBean course = coursesDAO.getCourseById(courseId);

                Map<String, String> courseDetails = Map.of(
                        "course_code", course.getCourse_code(),
                        "course_title", course.getCourse_title(),
                        "course_description", course.getCourse_description()
                );

                notificationService.notifyCourseEnrollment(authUserId.intValue(), course.getCourse_id(), course.getCourse_code(), course.getCourse_title(), course.getCourse_description());
                System.out.println(">> course description from DB: " + course.getCourse_description());

                writeJsonResponse(response, true, "Student enrolled successfully.", courseDetails);
            } else {
                writeJsonResponse(response, false, "Failed to enroll student.", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonResponse(response, false, "Server error during enrollment.", null);
        }
    }

    @DeleteMapping("/enrollments/delete/{id}")
    public void deleteEnrollment(@PathVariable Long id, HttpServletResponse response) throws IOException {
        try {
            // Get enrollment details BEFORE delete
            EnrollmentsBean enrollment = enrollmentsDAO.getEnrollmentById(id);

            int rows = enrollmentsDAO.deleteEnrollmentById(id);

            if (rows > 0) {
                // Send notification after deletion
                notificationService.notifyCourseEnrollmentRemoved(
                        enrollment.getAuthUserId().intValue(),
                        enrollment.getCourseId(),
                        enrollment.getCourseCode(),
                        enrollment.getCourseTitle(),
                        enrollment.getCourseDescription()
                );

                writeJsonResponse(response, true, "Enrollment deleted successfully.", null);
            } else {
                writeJsonResponse(response, false, "Enrollment not found or already deleted.", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonResponse(response, false, "Server error occurred while deleting.", null);
        }
    }

    @DeleteMapping("/enrollments/bulk-delete")
    public void bulkDeleteEnrollments(@RequestBody Map<String, Object> payload,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            writeJsonResponse(response, false, "Unauthorized access. Please login.", null);
            return;
        }

        UsersBean user = (UsersBean) session.getAttribute("user");
        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1 || roleId == 2)) {
            writeJsonResponse(response, false, "Forbidden. You are not authorized to perform this action.", null);
            return;
        }

        try {
            List<Object> rawIds = (List<Object>) payload.get("enrollmentIds");
            if (rawIds == null || rawIds.isEmpty()) {
                writeJsonResponse(response, false, "No enrollment IDs provided.", null);
                return;
            }

            List<Long> enrollmentIds = rawIds.stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .toList();

            int deletedCount = 0;
            for (Long id : enrollmentIds) {
                EnrollmentsBean enrollment = enrollmentsDAO.getEnrollmentById(id);

                // Attempt deletion
                int rows = enrollmentsDAO.deleteEnrollmentById(id);
                if (rows > 0) {
                    deletedCount++;

                    // Notify the student
                    notificationService.notifyCourseEnrollmentRemoved(
                            enrollment.getAuthUserId().intValue(),
                            enrollment.getCourseId(),
                            enrollment.getCourseCode(),
                            enrollment.getCourseTitle(),
                            enrollment.getCourseDescription()
                    );
                }
            }

            writeJsonResponse(response, true, deletedCount + " enrollment(s) successfully deleted.", null);

        } catch (Exception e) {
            e.printStackTrace();
            writeJsonResponse(response, false, "An error occurred while deleting enrollments.", null);
        }
    }

    private void writeJsonResponse(HttpServletResponse response, boolean success, String message, Object data) throws IOException {
        response.setContentType("application/json");
        var json = new com.google.gson.JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message);
        if (data != null) {
            json.add("data", new Gson().toJsonTree(data));
        }
        response.getWriter().write(json.toString());
    }

}
