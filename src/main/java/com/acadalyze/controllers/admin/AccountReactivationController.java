package com.acadalyze.controllers.admin;

import com.acadalyze.beans.tokens.ReactivationTokenBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.acadalyze.utils.ReactivationService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
public class AccountReactivationController {

    @Autowired
    private ReactivationService reactivationService;

    @Autowired
    private UsersDAO usersDAO;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @GetMapping("/reactivate-account")
    public ModelAndView reactivateAccount(@RequestParam("token") String token) {
        System.out.println("Received token: " + token);
        ModelAndView mav = new ModelAndView("/pages/admin/account_reactivation/account_reactivation");

        ReactivationTokenBean tokenBean = reactivationService.verifyReactivationToken(token);

        if (tokenBean == null) {
            mav.addObject("success", false);
            mav.addObject("message", "Invalid or expired reactivation link.");
        } else {
            Long userId = tokenBean.getAuthUserId();
            usersDAO.reactivateUserById(userId);
            reactivationService.deleteToken(token);
            mav.addObject("success", true);
            mav.addObject("message", "Your account has been successfully reactivated.");
        }

        return mav;
    }

    @PostMapping("/resend-reactivation")
    public void resendReactivationToken(@RequestParam("email") String email, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> json = new HashMap<>();
        Gson gson = new Gson();

        if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            json.put("success", false);
            json.put("message", "Invalid email format.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = response.getWriter()) {
                out.print(gson.toJson(json));
            }
            return;
        }

        UsersBean user = usersDAO.findEmailForReactivation(email.trim());

        if (user == null) {
            json.put("success", false);
            json.put("message", "No user found with that email.");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else if (user.getIsActive()) {
            json.put("success", false);
            json.put("message", "Account is already active.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            try {
                String token = reactivationService.generateReactivationToken();
                reactivationService.saveReactivationToken(user.getAuthUserId(), token);
                reactivationService.sendReactivationEmail(user.getEmail(), user.getFirstName(), token);

                json.put("success", true);
                json.put("message", "Reactivation email sent.");
                response.setStatus(HttpServletResponse.SC_OK);
            } catch (Exception e) {
                e.printStackTrace();
                json.put("success", false);
                json.put("message", "Failed to send reactivation email.");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(json));
        }
    }
}
