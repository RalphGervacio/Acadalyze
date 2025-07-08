package com.acadalyze.controllers.auth;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.acadalyze.utils.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
public class VerificationResultController {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private UsersDAO userDao;

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        UsersBean user = verificationService.verifyToken(token);
        String email = null;

        if (user != null) {
            email = user.getEmail();
            model.addAttribute("message", "Your account has been successfully verified.");
        } else {
            // Try resolving email manually from token
            Long userId = verificationService.getUserIdFromToken(token);
            if (userId != null) {
                UsersBean fetched = userDao.findById(userId);
                if (fetched != null) {
                    email = fetched.getEmail();
                }
            }
            model.addAttribute("message", "Verification failed or link expired.");
        }

        if (email != null) {
            model.addAttribute("email", email);
        }

        return "pages/auth/verification_result";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam("email") String email, Model model) {
        UsersBean user = userDao.findByEmail(email);
        if (user != null && !user.getIsVerified()) {
            String token = verificationService.generateToken();
            verificationService.saveTokenWithGivenToken(user.getAuthUserId(), token);
            verificationService.sendVerificationEmail(email, user.getFirstName(), token);
            model.addAttribute("message", "A new verification link has been sent to your email.");
        } else {
            model.addAttribute("message", "Email not found or already verified.");
        }
        return "pages/auth/verification_result";
    }
}
