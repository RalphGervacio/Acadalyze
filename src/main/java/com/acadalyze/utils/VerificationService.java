package com.acadalyze.utils;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import com.acadalyze.beans.tokens.VerificationTokenBean;
import com.acadalyze.dao.admin.manage_users.UsersDAO;
import com.acadalyze.dao.auth.VerificationTokenDAO;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author Ralph Gervacio
 */
@Service
public class VerificationService {

    @Autowired
    private VerificationTokenDAO tokenDao;

    @Autowired
    private UsersDAO userDao;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public String generateToken() {
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public void saveTokenWithGivenToken(Long userId, String token) {
        Timestamp expiry = Timestamp.from(Instant.now().plusSeconds(900));
        VerificationTokenBean bean = new VerificationTokenBean();
        bean.setToken(token);
        bean.setAuthUserId(userId);
        bean.setExpiry(expiry);

        System.out.println("Saving token to DB: " + token);
        tokenDao.deleteExpiredTokensForUser(userId);
        tokenDao.save(bean);
    }

    public UsersBean verifyToken(String token) {
        System.out.println("Verifying token: " + token);
        VerificationTokenBean bean = tokenDao.findByToken(token);
        if (bean == null) {
            System.out.println("Token not found.");
            return null;
        }

        UsersBean user = userDao.findById(bean.getAuthUserId());
        if (user == null) {
            System.out.println("User not found.");
            return null;
        }

        if (bean.getExpiry().before(Timestamp.from(Instant.now()))) {
            System.out.println("Token expired at: " + bean.getExpiry());
            return null;
        }

        if (!user.getIsVerified()) {
            user.setIsVerified(true);
            userDao.update(user);
            System.out.println("User successfully verified.");
        } else {
            System.out.println("User already verified.");
        }

        tokenDao.delete(token);
        return user;
    }

    public void sendVerificationEmail(String email, String firstName, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Account Verification - Acadalyze");

            String verificationUrl = baseUrl + "/verify?token=" + token;
            String emailBody = String.format(
                    "Hello %s,\n\n"
                    + "Please click the link below to verify your account:\n\n%s\n\n"
                    + "This link will expire in 15 minutes.\n\n"
                    + "Best regards,\nAcadalyze Team",
                    firstName, verificationUrl);

            message.setText(emailBody);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.print("Failed to send verification email: " + e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public Long getUserIdFromToken(String token) {
        VerificationTokenBean bean = tokenDao.findByToken(token);
        if (bean != null) {
            return bean.getAuthUserId();
        }
        return null;
    }

    public UsersBean reactivationUserToken(String token) {
        System.out.println("Checking reactivation token: " + token);
        VerificationTokenBean bean = tokenDao.findByToken(token);
        if (bean == null) {
            System.out.println("Token not found.");
            return null;
        }

        if (bean.getExpiry().before(Timestamp.from(Instant.now()))) {
            System.out.println("Token expired at: " + bean.getExpiry());
            return null;
        }

        tokenDao.delete(token);
        return userDao.findById(bean.getAuthUserId());
    }

    public void deleteToken(String token) {
        tokenDao.delete(token);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanExpiredTokens() {
        tokenDao.deleteExpiredTokens();
    }
}
