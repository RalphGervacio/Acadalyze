package com.acadalyze.controllers.admin.manage_course_subject;

import com.acadalyze.beans.admin.manage_course_subject.CourseSubjectBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.DropdownDAO;
import com.acadalyze.dao.admin.manage_course_subject.CourseSubjectDAO;
import com.acadalyze.dao.admin.manage_courses.CoursesDAO;
import com.acadalyze.dao.admin.manage_subjects.SubjectsDAO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@RestController
@RequestMapping(value = "/admin")
public class ManageCourseSubjectController {

    @Autowired
    CourseSubjectDAO courseSubjectDAO;

    @Autowired
    SubjectsDAO subjectsDAO;

    @Autowired
    CoursesDAO coursesDAO;

    @Autowired
    DropdownDAO dropdownDAO;

    @GetMapping("/course-subjects")
    public ModelAndView showManageCourseSubjectPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/admin/manage_course_subject/manage_course_subject");
    }

    // Get subjects assigned to a specific course
    @GetMapping("/course-subjects/list")
    public void list(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> subjects = courseSubjectDAO.getAllCourseSubjectBindings();
        writeJsonResponse(response, true, "All course-subjects retrieved successfully.", subjects);
    }

    // Add subject to course
    @PostMapping("/course-subjects/add")
    public void add(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        Long courseId = Long.valueOf(payload.get("course_id").toString());
        Long subjectId = Long.valueOf(payload.get("subject_id").toString());
        Integer semester = Integer.valueOf(payload.get("semester").toString());
        Integer yearLevel = Integer.valueOf(payload.get("year_level").toString());

        CourseSubjectBean bean = new CourseSubjectBean();
        bean.setCourse_id(courseId);
        bean.setSubject_id(subjectId);
        bean.setSemester(semester);
        bean.setYear_level(yearLevel);

        try {
            int rows = courseSubjectDAO.insert(bean);
            if (rows > 0) {
                writeJsonResponse(response, true, "Subject assigned to course successfully.", null);
            } else {
                writeJsonResponse(response, false, "Failed to assign subject.", null);
            }
        } catch (DuplicateKeyException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonResponse(response, false, "This subject is already assigned to the selected course, semester, and year level.", null);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJsonResponse(response, false, "An unexpected error occurred while assigning the subject.", null);
        }
    }

    // Update semester/year level
    @PatchMapping("/course-subjects/update")
    public void update(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        try {
            Long id = Long.valueOf(payload.get("course_subject_id").toString());
            Long courseId = Long.valueOf(payload.get("course_id").toString());
            Long subjectId = Long.valueOf(payload.get("subject_id").toString());
            Integer semester = Integer.valueOf(payload.get("semester").toString());
            Integer yearLevel = Integer.valueOf(payload.get("year_level").toString());

            CourseSubjectBean bean = new CourseSubjectBean();
            bean.setCourse_subject_id(id);
            bean.setCourse_id(courseId);
            bean.setSubject_id(subjectId);
            bean.setSemester(semester);
            bean.setYear_level(yearLevel);

            int rows = courseSubjectDAO.update(bean);
            if (rows > 0) {
                writeJsonResponse(response, true, "Subject assignment updated successfully.", null);
            } else {
                writeJsonResponse(response, false, "Update failed.", null);
            }

        } catch (DuplicateKeyException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonResponse(response, false, "This subject is already assigned to the selected course, semester, and year level.", null);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            writeJsonResponse(response, false, "Server error: " + e.getMessage(), null);
        }
    }

    // Delete assignment
    @DeleteMapping("/course-subjects/delete/{id}")
    public void delete(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        int rows = courseSubjectDAO.delete(id);
        if (rows > 0) {
            writeJsonResponse(response, true, "Subject removed from course.", null);
        } else {
            writeJsonResponse(response, false, "Delete failed.", null);
        }
    }

    @GetMapping("/course-subjects/subjects")
    public void getSubjects(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = dropdownDAO.getSubjects();
        writeJsonResponse(response, true, "Subjects fetched", list);
    }

    @GetMapping("/course-subjects/courses")
    public void getCourses(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = dropdownDAO.getCourses();
        writeJsonResponse(response, true, "Courses fetched", list);
    }

    private void writeJsonResponse(HttpServletResponse response, boolean success, String message, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message);
        if (data != null) {
            json.add("data", new Gson().toJsonTree(data));
        }

        PrintWriter out = response.getWriter();
        out.write(json.toString());
        out.flush();
    }

}
