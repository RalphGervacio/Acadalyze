package com.acadalyze.controllers.search;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_grades.GradesDAO;
import com.acadalyze.dao.admin.manage_subjects.SubjectsDAO;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
public class SearchController {

    @Autowired
    UsersDAO userDao;

    @Autowired
    SubjectsDAO subjectDao;

    @Autowired
    GradesDAO gradeDao;

    @GetMapping("/search")
    public void searchResults(@RequestParam("Search") String query,
            HttpSession session,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();

        UsersBean currentUser = (UsersBean) session.getAttribute("user");

        if (currentUser == null) {
            Map<String, Object> redirectResponse = new HashMap<>();
            redirectResponse.put("redirect", "/auth/login?returnTo=/search");
            response.getWriter().write(gson.toJson(redirectResponse));
            return;
        }

        Map<String, Object> results = new HashMap<>();

        try {
            results.put("students", userDao.searchStudents(query));
            results.put("subjects", subjectDao.searchSubjects(query));

            if (currentUser.getRole().getAuthRoleId() == 3) {
                results.put("grades", gradeDao.searchGrades(query, currentUser.getAuthUserId()));
            } else {
                results.put("grades", gradeDao.searchGradesAsAdmin(query));
            }

            response.getWriter().write(gson.toJson(results));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
