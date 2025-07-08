package com.acadalyze.controllers.admin.manage_courses;

import com.acadalyze.beans.admin.manage_courses.CoursesBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_courses.CoursesDAO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
@RequestMapping(value = "/admin")
public class ManageCoursesController {

    @Autowired
    CoursesDAO coursesDao;

    @GetMapping("/courses")
    public ModelAndView showManageCoursesPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/admin/manage_courses/manage_courses");
    }

    @GetMapping("/courses/list")
    public void getAllCourses(HttpServletResponse response) throws IOException {
        List<CoursesBean> list = coursesDao.getAllCourses();
        writeJsonResponse(response, true, "Courses fetched", list);
    }

    @PostMapping("/courses/add")
    public void addCourse(HttpServletResponse response,
            @RequestBody Map<String, Object> data) throws IOException {

        String code = data.get("code") != null ? data.get("code").toString().trim() : "";
        String name = data.get("name") != null ? data.get("name").toString().trim() : "";
        String description = data.get("description") != null ? data.get("description").toString().trim() : "";

        if (code.isEmpty()) {
            writeJsonResponse(response, false, "Course code is required.", null);
            return;
        }

        if (name.isEmpty()) {
            writeJsonResponse(response, false, "Course name is required.", null);
            return;
        }

        if (description.isEmpty()) {
            writeJsonResponse(response, false, "Description is required.", null);
            return;
        }

        if (coursesDao.isDuplicateCourse(code, name, description)) {
            writeJsonResponse(response, false, "Course already exists with the same code, name, or description.", null);
            return;
        }

        CoursesBean bean = new CoursesBean();
        bean.setCourse_code(code);
        bean.setCourse_title(name);
        bean.setCourse_description(description);

        coursesDao.addCourse(bean);
        writeJsonResponse(response, true, "Course added successfully.", null);
    }

    @GetMapping("/courses/get/{id}")
    public void getCourseById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        try {
            CoursesBean course = coursesDao.getCourseById(id);
            if (course != null) {
                writeJsonResponse(response, true, null, course);
            } else {
                writeJsonResponse(response, false, "Course not found.", null);
            }
        } catch (Exception e) {
            writeJsonResponse(response, false, "Error retrieving course.", null);
        }
    }

    @PatchMapping("/courses/update")
    public void updateCourse(HttpServletResponse response,
            @RequestBody Map<String, Object> data) throws IOException {

        Long id = data.get("id") != null ? Long.valueOf(data.get("id").toString()) : null;
        String code = data.get("code") != null ? data.get("code").toString().trim() : "";
        String name = data.get("name") != null ? data.get("name").toString().trim() : "";
        String description = data.get("description") != null ? data.get("description").toString().trim() : "";

        if (id == null) {
            writeJsonResponse(response, false, "Invalid course ID.", null);
            return;
        }
        if (code.isEmpty()) {
            writeJsonResponse(response, false, "Course code is required.", null);
            return;
        }
        if (name.isEmpty()) {
            writeJsonResponse(response, false, "Course name is required.", null);
            return;
        }
        if (description.isEmpty()) {
            writeJsonResponse(response, false, "Description is required.", null);
            return;
        }

        if (coursesDao.isDuplicateCourseExcludingId(id, code, name, description)) {
            writeJsonResponse(response, false, "A course with the same code, name, or description already exists.", null);
            return;
        }

        CoursesBean bean = new CoursesBean();
        bean.setCourse_id(id);
        bean.setCourse_code(code);
        bean.setCourse_title(name);
        bean.setCourse_description(description);

        coursesDao.updateCourse(bean);
        writeJsonResponse(response, true, "Course updated successfully.", null);
    }

    @DeleteMapping("/courses/delete/{id}")
    public void deleteCourse(HttpServletResponse response, @PathVariable Long id) throws IOException {
        coursesDao.deleteCourse(id);
        writeJsonResponse(response, true, "Course deleted", null);
    }

    private void writeJsonResponse(HttpServletResponse response, boolean success, String message, Object data) throws IOException {
        response.setContentType("application/json");
        var json = new com.google.gson.JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message);
        if (data != null) {
            json.add("data", new com.google.gson.Gson().toJsonTree(data));
        }
        response.getWriter().write(json.toString());
    }

}
