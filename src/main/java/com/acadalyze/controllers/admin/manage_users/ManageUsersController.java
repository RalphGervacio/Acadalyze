package com.acadalyze.controllers.admin.manage_users;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.acadalyze.utils.AccountDeletionEmailService;
import com.acadalyze.utils.VerificationService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
@RequestMapping(value = "/admin")
public class ManageUsersController {

    @Autowired
    UsersDAO usersDAO;

    @Autowired
    AccountDeletionEmailService accountDeletionEmailService;

    @Autowired
    VerificationService verificationService;

    @GetMapping("/users")
    public ModelAndView showManageUsersPage(HttpSession session, HttpServletResponse response) throws IOException {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null || user.getRole() == null) {
            return new ModelAndView("pages/access-denied");
        }

        Long roleId = user.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            return new ModelAndView("pages/access-denied");
        }

        return new ModelAndView("pages/admin/manage_users/manage_users");
    }

    @GetMapping("/users/list")
    public void getUserList(HttpServletResponse response, HttpSession session) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();
        Map<String, Object> json = new HashMap<>();

        UsersBean currentUser = (UsersBean) session.getAttribute("user");

        if (currentUser == null || currentUser.getRole() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.put("success", false);
            json.put("message", "Unauthorized - no session or role");
            response.getWriter().write(gson.toJson(json));
            return;
        }

        Long roleId = currentUser.getRole().getAuthRoleId();
        if (!(roleId == 1L || roleId == 2L)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.put("success", false);
            json.put("message", "Unauthorized - access restricted");
            response.getWriter().write(gson.toJson(json));
            return;
        }

        List<UsersBean> users = usersDAO.findAllUsers();
        json.put("success", true);
        json.put("users", users);
        response.getWriter().write(gson.toJson(json));
    }

    @DeleteMapping("/users/deactivate")
    public void softDeleteUser(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();
        Map<String, Object> json = new HashMap<>();

        Long authUserId = ((Number) payload.get("authUserId")).longValue();
        String email = (String) payload.get("email");

        UsersBean user = usersDAO.findById(authUserId);
        if (user != null) {
            // Soft delete
            usersDAO.softDeleteUserById(authUserId);

            // Generate reactivation token and store it (reuse verification token DAO)
            String token = verificationService.generateToken();
            verificationService.saveTokenWithGivenToken(authUserId, token);

            // Send account deletion email with reactivation link
            accountDeletionEmailService.sendAccountDeletionNotice(user.getFirstName(), user.getEmail(), token);

            json.put("success", true);
            json.put("message", "User deleted and notified.");
        } else {
            json.put("success", false);
            json.put("message", "User not found.");
        }

        response.getWriter().write(gson.toJson(json));
    }

    @GetMapping("/manage-users/fetch-user")
    public void fetchUserById(@RequestParam("userId") Long userId, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> json = new HashMap<>();
        Gson gson = new Gson();

        try {
            UsersBean user = usersDAO.findById(userId);

            if (user != null) {
                json.put("success", true);
                json.put("user", user);
            } else {
                json.put("success", false);
                json.put("message", "User not found.");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            json.put("success", false);
            json.put("message", "Something went wrong.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(json));
        }
    }

    @GetMapping("/manage-users/check-email")
    public void checkEmailExists(
            @RequestParam("email") String email,
            @RequestParam("userId") Long userId,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> json = new HashMap<>();
        Gson gson = new Gson();

        boolean exists = usersDAO.emailExistsForOtherUser(email, userId);

        if (exists) {
            json.put("success", false);
            json.put("message", "Email is already in use by another user.");
        } else {
            json.put("success", true);
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(json));
        }
    }

    @PatchMapping("/manage-users/update-information")
    public void updateUserProfile(@RequestBody UsersBean updatedUser, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> json = new HashMap<>();
        Gson gson = new Gson();

        try {
            // Check if email is used by another active user
            boolean emailExists = usersDAO.emailExistsForOtherUser(updatedUser.getEmail(), updatedUser.getAuthUserId());
            if (emailExists) {
                json.put("success", false);
                json.put("message", "Email is already in use by another user.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                boolean success = usersDAO.updateUserProfile(updatedUser);
                if (success) {
                    json.put("success", true);
                    json.put("message", "User updated successfully.");
                } else {
                    json.put("success", false);
                    json.put("message", "Failed to update user.");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            json.put("success", false);
            json.put("message", "An unexpected error occurred.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(json));
        }
    }

}
