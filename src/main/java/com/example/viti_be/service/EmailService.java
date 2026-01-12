package com.example.viti_be.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Viti Shop - Verify your account");
        message.setText("Your verification code is: " + otp);
        mailSender.send(message);
    }

    public void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    /**
     * Gửi email thông báo tài khoản mới cho employee
     */
    public void sendEmployeeCredentials(String toEmail, String username, String temporaryPassword, String fullName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Viti Shop - Your Employee Account");
        message.setText(String.format(
                "Dear %s,\n\n" +
                        "An admin has created an account for you in Viti Shop system.\n\n" +
                        "Your login credentials:\n" +
                        "Email: %s\n" +
                        "Username: %s\n" +
                        "Temporary Password: %s\n\n" +
                        "⚠️ IMPORTANT: You must change your password after first login for security.\n\n" +
                        "Login here: [YOUR_FRONTEND_URL]/login\n\n" +
                        "Best regards,\n" +
                        "Viti Shop Team",
                fullName,
                toEmail,
                username,
                temporaryPassword
        ));
        mailSender.send(message);
    }
}