package com.acadalyze.controllers.auth;

import com.acadalyze.beans.admin.manage_users.ForgotPasswordBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.auth.ForgotPasswordDAO;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.acadalyze.utils.ForgotPasswordEmailService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordDAO tokenDao;

    @Autowired
    private UsersDAO userDao;

    @Autowired
    private ForgotPasswordEmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$";

    // Redirect to forgot password form if no token is provided
    @GetMapping(value = "/forgot-password", params = "!token")
    public ModelAndView redirectToForm() {
        return new ModelAndView("pages/auth/forgot_password/forgot_password");
    }

    // Display reset password page using a valid token
    @GetMapping(value = "/forgot-password", params = "token")
    public ModelAndView showResetPasswordPage(@RequestParam String token) {
        ForgotPasswordBean bean = tokenDao.findByToken(token);
        if (bean == null || bean.getExpiry().isBefore(LocalDateTime.now())) {
            return new ModelAndView("pages/auth/forgot_password/reset_expired");
        }

        ModelAndView mv = new ModelAndView("pages/auth/forgot_password/reset_password");
        mv.addObject("token", token);
        return mv;
    }

    // Handle forgot password email submission (AJAX)
    @PostMapping("/api/forgot-password")
    public void handleForgotPassword(
            @RequestBody Map<String, String> payload,
            HttpServletResponse response) throws IOException {

        Gson gson = new Gson();
        Map<String, Object> json = new HashMap<>();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String email = payload.get("email");

        if (email == null || email.trim().isEmpty()) {
            json.put("success", false);
            json.put("message", "Email is required.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(json));
            return;
        }

        if (!email.matches(EMAIL_REGEX)) {
            json.put("success", false);
            json.put("message", "Please enter a valid email address.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(json));
            return;
        }

        UsersBean user = userDao.findByEmail(email);
        if (user == null) {
            json.put("success", false);
            json.put("message", "No account associated with this email.");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(gson.toJson(json));
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        ForgotPasswordBean forgotToken = new ForgotPasswordBean();
        forgotToken.setToken(token);
        forgotToken.setEmail(email);
        forgotToken.setExpiry(expiry);
        tokenDao.save(forgotToken);

        try {
            emailService.sendResetEmail(user.getFirstName(), email, token);
            json.put("success", true);
            json.put("message", "A password reset link has been sent to your email.");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            e.printStackTrace();
            json.put("success", false);
            json.put("message", "Failed to send email. Please try again.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(json));
    }

    // Handle reset password logic via AJAX
    @PostMapping("/reset-password")
    public void handleResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            HttpServletResponse response) throws IOException {

        Gson gson = new Gson();
        Map<String, Object> json = new HashMap<>();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ForgotPasswordBean bean = tokenDao.findByToken(token);
        if (bean == null || bean.getExpiry().isBefore(LocalDateTime.now())) {
            json.put("success", false);
            json.put("message", "Token is invalid or has expired.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(json));
            return;
        }

        UsersBean user = userDao.findByEmail(bean.getEmail());
        if (user == null) {
            json.put("success", false);
            json.put("message", "User not found.");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(gson.toJson(json));
            return;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            json.put("success", false);
            json.put("message", "Password is required.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(json));
            return;
        }

        if (!newPassword.matches(PASSWORD_REGEX)) {
            json.put("success", false);
            json.put("message", "Password must be at least 8 characters and include uppercase, lowercase, number, and symbol.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(json));
            return;
        }

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        userDao.update(user);
        tokenDao.delete(token);

        json.put("success", true);
        json.put("message", "Password reset successful. You may now log in.");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(json));
    }

    // Success and expired views
    @GetMapping("/reset-success")
    public ModelAndView resetSuccess() {
        return new ModelAndView("pages/auth/forgot_password/reset_success");
    }

    @GetMapping("/reset-expired")
    public ModelAndView resetExpired() {
        return new ModelAndView("pages/auth/forgot_password/forgot_password/reset_expired");
    }
}
