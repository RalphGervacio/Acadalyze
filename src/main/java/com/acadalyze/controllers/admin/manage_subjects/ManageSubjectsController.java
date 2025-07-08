package com.acadalyze.controllers.admin.manage_subjects;

import com.acadalyze.beans.admin.manage_subjects.SubjectsBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_subjects.SubjectsDAO;
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
public class ManageSubjectsController {

    @Autowired
    SubjectsDAO subjectDao;

    @GetMapping("/subjects")
    public ModelAndView showManageSubjectsPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/admin/manage_subjects/manage_subjects");
    }

    @GetMapping("/subjects/list")
    public void getAllSubjects(HttpServletResponse response) throws IOException {
        List<SubjectsBean> list = subjectDao.getAllSubjects();
        writeJsonResponse(response, true, "Subjects fetched", list);
    }

    @PostMapping("/subjects/add")
    public void addSubject(HttpServletResponse response,
            @RequestBody Map<String, Object> data) throws IOException {

        String code = data.get("code") != null ? data.get("code").toString().trim() : "";
        String name = data.get("name") != null ? data.get("name").toString().trim() : "";
        String description = data.get("description") != null ? data.get("description").toString().trim() : "";

        if (code.isEmpty()) {
            writeJsonResponse(response, false, "Subject code is required.", null);
            return;
        }

        if (name.isEmpty()) {
            writeJsonResponse(response, false, "Subject name is required.", null);
            return;
        }

        if (description.isEmpty()) {
            writeJsonResponse(response, false, "Description is required.", null);
            return;
        }

        if (subjectDao.isDuplicateSubject(code, name, description)) {
            writeJsonResponse(response, false, "Subject already exists with the same code, name, or description.", null);
            return;
        }

        SubjectsBean bean = new SubjectsBean();
        bean.setSubjectCode(code);
        bean.setSubjectName(name);
        bean.setDescription(description);

        subjectDao.addSubject(bean);
        writeJsonResponse(response, true, "Subject added successfully.", null);
    }

    @GetMapping("/subjects/get/{id}")
    public void getSubjectById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        try {
            SubjectsBean subject = subjectDao.getSubjectById(id);
            if (subject != null) {
                writeJsonResponse(response, true, null, subject);
            } else {
                writeJsonResponse(response, false, "Subject not found.", null);
            }
        } catch (Exception e) {
            writeJsonResponse(response, false, "Error retrieving subject.", null);
        }
    }

    @PatchMapping("/subjects/update")
    public void updateSubject(HttpServletResponse response,
            @RequestBody Map<String, Object> data) throws IOException {

        Long id = data.get("id") != null ? Long.valueOf(data.get("id").toString()) : null;
        String code = data.get("code") != null ? data.get("code").toString().trim() : "";
        String name = data.get("name") != null ? data.get("name").toString().trim() : "";
        String description = data.get("description") != null ? data.get("description").toString().trim() : "";

        if (id == null) {
            writeJsonResponse(response, false, "Invalid subject ID.", null);
            return;
        }
        if (code.isEmpty()) {
            writeJsonResponse(response, false, "Subject code is required.", null);
            return;
        }
        if (name.isEmpty()) {
            writeJsonResponse(response, false, "Subject name is required.", null);
            return;
        }
        if (description.isEmpty()) {
            writeJsonResponse(response, false, "Description is required.", null);
            return;
        }

        if (subjectDao.isDuplicateSubjectExcludingId(id, code, name, description)) {
            writeJsonResponse(response, false, "A subject with the same code, name, or description already exists.", null);
            return;
        }

        SubjectsBean bean = new SubjectsBean();
        bean.setSubjectId(id);
        bean.setSubjectCode(code);
        bean.setSubjectName(name);
        bean.setDescription(description);

        subjectDao.updateSubject(bean);
        writeJsonResponse(response, true, "Subject updated successfully.", null);
    }

    @DeleteMapping("/subjects/delete/{id}")
    public void deleteSubject(HttpServletResponse response, @PathVariable Long id) throws IOException {
        subjectDao.deleteSubject(id);
        writeJsonResponse(response, true, "Subject deleted", null);
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
