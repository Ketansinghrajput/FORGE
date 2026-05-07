package com.forge.platform.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async // 🔥 Backend par email jayega, user wait nahi karega
    public void sendAuctionWonEmail(String toEmail, String auctionTitle, BigDecimal finalAmount) {
        try {
            Context context = new Context();
            context.setVariable("title", auctionTitle);
            context.setVariable("amount", finalAmount);

            // Thymeleaf template render karo
            String htmlContent = templateEngine.process("emails/auction-won", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Congratulations! You won the auction: " + auctionTitle);
            helper.setText(htmlContent, true);
            helper.setFrom("no-reply@forge.com");

            mailSender.send(message);
            log.info("🏆 Auction won email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send win email", e);
        }
    }
    @Async // 🔥 Backend thread handle karega, user ko lag nahi milega
    public void sendOutbidEmail(String toEmail, String auctionTitle, BigDecimal newHighestPrice) {
        try {
            Context context = new Context();
            context.setVariable("title", auctionTitle);
            context.setVariable("newPrice", newHighestPrice);

            // Thymeleaf template render karo (outbid.html file honi chahiye)
            String htmlContent = templateEngine.process("emails/outbid", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("⚠️ Outbid! High bid on: " + auctionTitle);
            helper.setText(htmlContent, true);
            helper.setFrom("no-reply@forge.com");

            mailSender.send(message);
            log.info("📧 Outbid notification sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send outbid email", e);
        }
    }
}