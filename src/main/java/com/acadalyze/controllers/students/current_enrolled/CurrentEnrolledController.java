package com.acadalyze.controllers.students.current_enrolled;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.beans.students.current_enrolled.CurrentEnrolledBean;
import com.acadalyze.dao.students.current_enrolled.CurrentEnrolledDAO;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
@RequestMapping(value = "/student")
public class CurrentEnrolledController {

    @Autowired
    CurrentEnrolledDAO currentEnrolledDAO;

    @GetMapping("/current-enrolled")
    public ModelAndView showCurrentEnrolledPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 3L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/students/current_enrolled/current_enrolled");
    }

    @GetMapping("/current-enrolled/list")
    public void getStudentEnrolled(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");
        Map<String, Object> json = new HashMap<>();

        if (user == null || user.getRole().getAuthRoleId() != 3) {
            json.put("success", false);
            json.put("message", "Unauthorized access");
        } else {
            List<CurrentEnrolledBean> enrolled = currentEnrolledDAO.getEnrolledByStudentId(user.getAuthUserId());
            json.put("success", true);
            json.put("data", enrolled);
        }

        response.setContentType("application/json");
        response.getWriter().print(new Gson().toJson(json));
    }

}
