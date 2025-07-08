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
public class AccountDeletionEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendAccountDeletionNotice(String firstName, String email, String token) {
        String subject = "Account Deletion - Acadalyze";

        String reactivationLink = baseUrl + "/reactivate-account?token=" + token;

        String content = "<p>Hi " + firstName + ",</p>"
                + "<p>This is to inform you that your Acadalyze account has been deactivated by an administrator.</p>"
                + "<p>If you believe this was a mistake, you can request to reactivate your account by clicking the link below:</p>"
                + "<p><a href=\"" + reactivationLink + "\">Reactivate My Account</a></p>"
                + "<p>This link will expire in 24 hours.</p>"
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
            throw new RuntimeException("Failed to send account deletion email", e);
        }
    }
}
