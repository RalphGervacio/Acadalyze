package com.acadalyze.controllers.admin.manage_instructors;

import com.acadalyze.beans.admin.manage_instructors.InstructorsBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.DropdownDAO;
import com.acadalyze.dao.admin.manage_instructors.InstructorsDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class ManageInstructorsController {

    @Autowired
    InstructorsDAO instructorsDAO;

    @Autowired
    DropdownDAO dropdownDAO;

    @GetMapping("/instructors")
    public ModelAndView showManageInstructorsPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/admin/manage_instructors/manage_instructors");
    }

    @GetMapping("/instructors/list")
    public void getAllAssignedInstructors(HttpServletResponse response) throws IOException {
        List<InstructorsBean> list = instructorsDAO.getAllAssignedInstructors();
        writeJsonResponse(response, true, "Instructors fetched", list);
    }

    @PostMapping("/instructors/assign")
    public void assignInstructor(HttpServletResponse response, @RequestBody Map<String, Object> data) throws IOException {

        Long subjectId = data.get("subjectId") != null ? Long.valueOf(data.get("subjectId").toString()) : null;
        Long instructorId = data.get("instructorId") != null ? Long.valueOf(data.get("instructorId").toString()) : null;

        if (subjectId == null) {
            writeJsonResponse(response, false, "Subject is required.", null);
            return;
        }

        if (instructorId == null) {
            writeJsonResponse(response, false, "Instructor is required.", null);
            return;
        }

        if (instructorsDAO.isAlreadyAssigned(subjectId, instructorId)) {
            writeJsonResponse(response, false, "Instructor is already assigned to this subject.", null);
            return;
        }

        InstructorsBean bean = new InstructorsBean();
        bean.setSubjectId(subjectId);
        bean.setInstructorId(instructorId);

        boolean success = instructorsDAO.assignInstructorToSubject(bean);
        writeJsonResponse(response, success, success ? "Instructor assigned successfully." : "Failed to assign instructor.", null);
    }

    @DeleteMapping("/instructors/remove")
    public void removeInstructor(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Long subjectId = Long.valueOf(req.getParameter("subjectId"));
        Long instructorId = Long.valueOf(req.getParameter("instructorId"));

        boolean success = instructorsDAO.removeInstructorFromSubject(subjectId, instructorId);
        writeJsonResponse(res, success, success ? "Instructor removed successfully." : "Failed to remove instructor.", null);
    }

    @GetMapping("/instructors/dropdown")
    public void getInstructorDropdown(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> instructors = dropdownDAO.getInstructors();
        writeJsonResponse(response, true, "Instructors fetched", instructors);
    }

    @GetMapping("/instructors/subjects")
    public void getSubjects(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = dropdownDAO.getSubjects();
        writeJsonResponse(response, true, "Subjects fetched", list);
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
