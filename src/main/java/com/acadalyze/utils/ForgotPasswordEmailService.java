package com.acadalyze.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 *
 * @author Ralph Gervacio
 */
@Service
public class ForgotPasswordEmailService {

    @Autowired
    JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendResetEmail(String firstName, String email, String token) {
        String subject = "Reset Your Acadalyze Password";
        String resetLink = baseUrl + "/forgot-password?token=" + token;

        String content = "<p>Hi " + firstName + ",</p>"
                + "<p>You requested to reset your password for Acadalyze.</p>"
                + "<p>Click the link below to reset your password:</p>"
                + "<p><a href=\"" + resetLink + "\">Reset Password</a></p>"
                + "<p>This link will expire in 15 minutes.</p>"
                + "<br><p>If you didn't request this, you can safely ignore this email.</p>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("no-reply@acadalyze.com");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send reset password email", e);
        }
    }
}
