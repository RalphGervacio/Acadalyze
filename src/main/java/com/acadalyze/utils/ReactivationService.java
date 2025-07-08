package com.acadalyze.utils;

import com.acadalyze.beans.tokens.ReactivationTokenBean;
import com.acadalyze.dao.tokens.ReactivationTokenDAO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 *
 * Author: Ralph Gervacio
 */
@Service
public class ReactivationService {

    @Autowired
    private ReactivationTokenDAO tokenDao;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public String generateReactivationToken() {
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public void saveReactivationToken(Long userId, String token) {
        Timestamp expiry = Timestamp.from(Instant.now().plusSeconds(86400));
        ReactivationTokenBean tokenBean = new ReactivationTokenBean();
        tokenBean.setToken(token);
        tokenBean.setAuthUserId(userId);
        tokenBean.setExpiry(expiry);

        tokenDao.deleteExpiredTokensForUser(userId);
        tokenDao.save(tokenBean);
    }

    public void sendReactivationEmail(String email, String firstName, String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = baseUrl + "/reactivate-account?token=" + encodedToken;
        String subject = "Account Reactivation - Acadalyze";

        String content = "<p>Hi " + firstName + ",</p>"
                + "<p>Your Acadalyze account has been deactivated. If this was a mistake, please click the link below to reactivate it:</p>"
                + "<p><a href=\"" + link + "\">Reactivate My Account</a></p>"
                + "<p><em>This link will expire in 24 hours.</em></p>"
                + "<br><p>Best regards,<br>Acadalyze Team</p>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send reactivation email: " + e.getMessage(), e);
        }
    }

    public ReactivationTokenBean verifyReactivationToken(String token) {
        System.out.println("Verifying reactivation token: " + token);
        ReactivationTokenBean tokenBean = tokenDao.findByToken(token);

        if (tokenBean == null) {
            System.out.println("Token not found.");
            return null;
        }

        if (tokenBean.getExpiry().before(Timestamp.from(Instant.now()))) {
            return null;
        }

        return tokenBean;
    }

    public void deleteToken(String token) {
        tokenDao.delete(token);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void deleteExpiredTokens() {
        System.out.println("Cleaning up expired reactivation tokens...");
        tokenDao.deleteExpiredTokens();
    }
}
