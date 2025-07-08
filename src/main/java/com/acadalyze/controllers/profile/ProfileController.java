package com.acadalyze.controllers.profile;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.acadalyze.utils.NotificationService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    UsersDAO userDao;

    @Autowired
    NotificationService notificationService;

    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", user);
        return "pages/profile/Profile";
    }

    @GetMapping("/view/{id}")
    public String viewOtherUserProfile(@PathVariable("id") Long id,
            HttpSession session,
            Model model) {
        UsersBean sessionUser = (UsersBean) session.getAttribute("user");

        if (sessionUser == null) {
            return "redirect:/auth/login";
        }

        if (sessionUser.getAuthUserId().equals(id)) {
            return "redirect:/profile";
        }

        UsersBean otherUser = userDao.findById(id);

        if (otherUser == null) {
            model.addAttribute("error", "User not found.");
            return "pages/profile/ViewProfile"; 
        }

        model.addAttribute("user", otherUser);
        return "pages/profile/ViewProfile";
    }

    @GetMapping("/profile-image/{id}")
    public void getProfileImage(@PathVariable Long id, HttpServletResponse response) throws IOException {
        UsersBean user = userDao.findById(id);

        if (user == null || user.getProfileImage() == null) {
            response.sendRedirect("/img/no-profile-picture.png");
            return;
        }

        byte[] image = user.getProfileImage();
        response.setContentType("image/jpeg");
        response.setContentLength(image.length);

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        response.getOutputStream().write(image);
    }

    @GetMapping("/cover-image/{id}")
    public void getCoverImage(@PathVariable Long id, HttpServletResponse response) throws IOException {
        UsersBean user = userDao.findById(id);

        if (user == null || user.getCoverImage() == null) {
            response.sendRedirect("/img/no-cover-photo.png");
            return;
        }

        System.out.println("Serving cover image for ID " + id + ", size: " + (user.getCoverImage() != null ? user.getCoverImage().length : 0));

        byte[] image = user.getCoverImage();

        response.setContentType("image/jpeg");
        response.setContentLength(image.length);

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        OutputStream out = response.getOutputStream();
        out.write(image);
        out.flush();
        out.close();
    }

    @PatchMapping("/upload-image")
    public void uploadProfileImage(@RequestParam("image") MultipartFile file,
            HttpSession session,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "User not logged in.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (file.isEmpty()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Please select a file.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "File size must be less than 2MB.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Only image files are allowed.");
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        try {
            byte[] imageBytes = file.getBytes();
            user.setProfileImage(imageBytes);
            userDao.updateProfileImage(user.getAuthUserId(), imageBytes);
            session.setAttribute("user", user);
            notificationService.notifyProfilePhotoUpdate(user.getAuthUserId().intValue());

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Profile image uploaded successfully.");
            jsonResponse.put("userId", user.getAuthUserId());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(jsonResponse));
        } catch (IOException e) {
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to upload image.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String json = gson.toJson(jsonResponse);
            response.getWriter().write(json);
            System.out.println("Error Response: " + json);
        }
    }

    @PatchMapping("/upload-cover")
    public void uploadCoverImage(@RequestParam("coverImage") MultipartFile file,
            HttpSession session,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "User not logged in.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (file.isEmpty()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Please select a file.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "File size must be less than 2MB.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Only image files are allowed.");
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        try {
            byte[] coverBytes = file.getBytes();
            user.setCoverImage(coverBytes);
            userDao.updateCoverImage(user.getAuthUserId(), coverBytes);
            session.setAttribute("user", user);
            notificationService.notifyProfilePhotoUpdate(user.getAuthUserId().intValue());

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Cover photo uploaded successfully.");
            jsonResponse.put("userId", user.getAuthUserId());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(jsonResponse));
        } catch (IOException e) {
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to upload cover photo.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(jsonResponse));
        }
    }

    @PatchMapping("/update-bio")
    public void updateUserBio(@RequestParam("bio") String bio,
            HttpSession session,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> json = new HashMap<>();
        Gson gson = new Gson();

        UsersBean user = (UsersBean) session.getAttribute("user");

        if (user == null) {
            json.put("success", false);
            json.put("message", "User not logged in.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            userDao.updateBio(user.getAuthUserId(), bio.trim());

            notificationService.notifyBioUpdate(user.getAuthUserId().intValue(), bio.trim());

            UsersBean updatedUser = userDao.findById(user.getAuthUserId());

            session.setAttribute("user", updatedUser);

            json.put("success", true);
            json.put("message", "Bio updated successfully.");
            response.setStatus(HttpServletResponse.SC_OK);
        }

        response.getWriter().write(gson.toJson(json));
    }

    @GetMapping("/get-bio")
    public void getUserBio(HttpSession session, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        UsersBean sessionUser = (UsersBean) session.getAttribute("user");
        Map<String, Object> json = new HashMap<>();
        Gson gson = new Gson();

        if (sessionUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json.put("success", false);
            json.put("message", "User not logged in.");
        } else {
            UsersBean freshUser = userDao.findById(sessionUser.getAuthUserId());
            json.put("success", true);
            json.put("bio", freshUser.getBio() != null ? freshUser.getBio() : "");
        }

        response.getWriter().write(gson.toJson(json));
    }

    @PostMapping("/verify-password")
    @ResponseBody
    public Map<String, Object> verifyPassword(@RequestBody Map<String, String> body, HttpSession session) {
        UsersBean user = (UsersBean) session.getAttribute("user");
        String enteredPassword = body.get("password");

        boolean isValid = userDao.verifyUserPassword(user.getAuthUserId(), enteredPassword);

        Map<String, Object> json = new HashMap<>();
        if (isValid) {
            json.put("success", true);
        } else {
            json.put("success", false);
            json.put("message", "Incorrect password.");
        }
        return json;
    }

    @GetMapping("/login-history")
    @ResponseBody
    public List<Map<String, Object>> getLoginHistory(HttpSession session) {
        UsersBean user = (UsersBean) session.getAttribute("user");
        return userDao.getLoginHistoryByUserId(user.getAuthUserId());
    }

}
