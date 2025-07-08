package com.acadalyze.controllers.auth;

import com.acadalyze.beans.admin.manage_users.RolesBean;
import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.acadalyze.utils.VerificationService;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.gson.Gson;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsersDAO userDao;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private VerificationService verificationService;

    // GET login page
    @GetMapping("/login")
    public ModelAndView showLoginPage(@RequestParam(value = "returnTo", required = false) String returnTo,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (session.getAttribute("isLoggedIn") != null) {
            return new ModelAndView("pages/dashboard");
        }

        // Try auto-login from remember-me cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember-me".equals(cookie.getName())) {
                    String[] parts = cookie.getValue().split("\\|");
                    if (parts.length == 3) {
                        String userName = parts[0];
                        long expiry;
                        try {
                            expiry = Long.parseLong(parts[1]);
                            if (System.currentTimeMillis() > expiry) {
                                break;
                            }

                            String expectedSignature = DigestUtils.md5DigestAsHex((userName + ":" + expiry + ":secretKey").getBytes());
                            if (expectedSignature.equals(parts[2])) {
                                UsersBean user = userDao.findByUserName(userName);
                                if (user != null && user.getIsVerified()) {
                                    session.setAttribute("isLoggedIn", true);
                                    session.setAttribute("user", user);
                                    session.setAttribute("userId", user.getAuthUserId());
                                    return new ModelAndView("pages/dashboard");
                                }
                            }
                        } catch (NumberFormatException e) {
                            break;
                        }
                    }
                }
            }
        }

        ModelAndView mv = new ModelAndView("pages/auth/login");
        if (returnTo != null && !returnTo.isEmpty()) {
            mv.addObject("returnTo", returnTo);
        }
        return mv;
    }

    // POST authenticate
    @PostMapping("/login/authenticate")
    public void handleLogin(
            @RequestParam("userName") String userName,
            @RequestParam("password") String password,
            @RequestParam(value = "rememberMe", required = false) boolean rememberMe,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Map<String, Object> jsonResponse = new HashMap<>();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();

        System.out.println("Login attempt for username: " + userName);

        if (userName == null || userName.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Username and password are required.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        try {
            UsersBean user = userDao.findByUserName(userName);

            if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Invalid username or password!");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            if (!user.getIsVerified()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Please verify your email before logging in.");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // Set role ID to avoid null in session-based security checks
            if (user.getRole() != null) {
                user.setAuthRoleId(user.getRole().getAuthRoleId());
            }

            session.setAttribute("isLoggedIn", true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getAuthUserId());

            System.out.println("Login attempt for userId: " + user.getAuthUserId());

            // Save login history
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }

            // Normalize IPv6 loopback to IPv4
            if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
                ipAddress = "127.0.0.1";
            }

            String userAgent = request.getHeader("User-Agent");

            System.out.println("[LOGIN] Saving login history...");
            System.out.println("[LOGIN] IP Address : " + ipAddress);
            System.out.println("[LOGIN] User Agent : " + userAgent);
            System.out.println("[LOGIN] Auth User ID: " + user.getAuthUserId());

            userDao.saveLoginHistory(user.getAuthUserId(), ipAddress, userAgent);

            // --- Remember Me: Set Cookie ---
            if (rememberMe) {
                long expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);
                String signature = DigestUtils.md5DigestAsHex((userName + ":" + expiry + ":secretKey").getBytes());
                String token = userName + "|" + expiry + "|" + signature;

                Cookie cookie = new Cookie("remember-me", token);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(30 * 24 * 60 * 60);
                response.addCookie(cookie);
            }

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Login successful.");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", "An error occurred during login.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(jsonResponse));
        }
    }

    // GET sign up page
    @GetMapping("/signup")
    public ModelAndView showSignupForm() {
        return new ModelAndView("pages/auth/sign_up");
    }

    // Post sign up authenticate
    @PostMapping("/signup/authenticate")
    public void handleSignup(@RequestBody Map<String, String> payload, HttpServletResponse response) throws IOException {
        String email = payload.get("email");
        String password = payload.get("password");
        String firstName = payload.get("firstName");
        String middleName = payload.get("middleName");
        middleName = middleName == null ? "" : middleName.trim();
        String lastName = payload.get("lastName");
        String userName = payload.get("userName");
        String role = payload.get("role");
        String studentId = payload.get("studentId");

        Map<String, Object> jsonResponse = new HashMap<>();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();

        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$";
        String studentIdRegex = "^\\d{11}$";
        String nameRegex = "^([A-Z][a-z]{1,49})(\\s[A-Z][a-z]{1,49})*$";

        if (email.trim().isEmpty() || password.trim().isEmpty() || role.trim().isEmpty()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Email, password, and role are required.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (userDao.findByEmail(email) != null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Email already exists.");
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (userDao.findByUserName(userName) != null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Username already exists.");
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (!password.matches(passwordRegex)) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (!firstName.matches(nameRegex)
                || (!middleName.trim().isEmpty() && !middleName.matches(nameRegex))
                || !lastName.matches(nameRegex)) {

            jsonResponse.put("success", false);
            jsonResponse.put("message", "Names must start with capital letters and be alphabetic.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        Long roleId;
        if (role.equalsIgnoreCase("STUDENT")) {
            roleId = 3L;
            if (studentId == null || !studentId.matches(studentIdRegex)) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Valid student ID is required.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            if (userDao.findByStudentId(studentId) != null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Student ID already registered.");
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

        } else if (role.equalsIgnoreCase("ADMIN")) {
            roleId = 2L;
            studentId = null;
        } else if (role.equalsIgnoreCase("INSTRUCTOR")) {
            roleId = 4L;
            studentId = null;
        } else {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Invalid role selected.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        RolesBean roleBean = new RolesBean();
        roleBean.setAuthRoleId(roleId);

        // Generate token ONCE
        String token = verificationService.generateToken();

        UsersBean newUser = new UsersBean();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setFirstName(firstName);
        newUser.setMiddleName(middleName);
        newUser.setLastName(lastName);
        newUser.setUserName(userName);
        newUser.setStudentId(studentId);
        newUser.setRole(roleBean);
        newUser.setIsVerified(false);
        newUser.setVerificationToken(token);

        try {
            Long userId = userDao.save(newUser);
            if (userId == null) {
                throw new RuntimeException("User ID was not generated after save.");
            }

            newUser.setAuthUserId(userId);

            verificationService.saveTokenWithGivenToken(userId, token);
            verificationService.sendVerificationEmail(email, firstName, token);

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Account created successfully. Please check your email to verify.");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(jsonResponse));
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to create account: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(jsonResponse));
        }
    }

    @GetMapping("/check-username")
    public void checkUsername(@RequestParam String userName, HttpServletResponse response) throws IOException {
        Map<String, Object> json = new HashMap<>();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();

        boolean exists = userDao.findByUserName(userName) != null;
        json.put("success", true);
        json.put("available", !exists);
        json.put("message", exists ? "Username is already taken." : "Username is available.");
        response.getWriter().write(gson.toJson(json));
    }

    @GetMapping("/check-email")
    public void checkEmailAvailability(@RequestParam String email, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        boolean taken = userDao.findByEmail(email) != null;
        Map<String, Object> json = new HashMap<>();
        json.put("success", true);
        json.put("available", !taken);
        json.put("message", taken ? "Email already exists." : "Email is available.");

        response.getWriter().write(new Gson().toJson(json));
    }

    // GET log out page
    @GetMapping("/logout")
    public ModelAndView logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        session.invalidate();

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember-me".equals(cookie.getName())) {
                    cookie.setValue(null);
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }

        return new ModelAndView("redirect:/auth/login");
    }

}
