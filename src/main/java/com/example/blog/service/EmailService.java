package com.example.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public void sendVerificationCode(String to, String code) {
        sendVerificationCode(to, code, null);
    }

    public void sendVerificationCode(String to, String code, String type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Dansela - 验证码");
            
            String htmlContent;
            if ("admin_verify".equals(type)) {
                htmlContent = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { 
                                font-family: 'Helvetica Neue', Arial, sans-serif;
                                line-height: 1.6;
                                color: #333;
                                max-width: 600px;
                                margin: 0 auto;
                                padding: 20px;
                            }
                            .container {
                                background: linear-gradient(135deg, #1a1a1a, #2a2a2a);
                                border-radius: 16px;
                                padding: 30px;
                                box-shadow: 0 8px 32px rgba(0,0,0,0.1);
                                border: 1px solid rgba(255,255,255,0.1);
                            }
                            .header {
                                text-align: center;
                                margin-bottom: 30px;
                            }
                            .logo {
                                font-size: 24px;
                                color: #00b7ff;
                                font-weight: bold;
                                text-shadow: 0 0 10px rgba(0,183,255,0.3);
                            }
                            .code-container {
                                background: rgba(0,183,255,0.1);
                                border: 1px solid rgba(0,183,255,0.3);
                                border-radius: 8px;
                                padding: 20px;
                                text-align: center;
                                margin: 20px 0;
                            }
                            .code {
                                font-size: 32px;
                                color: #00b7ff;
                                letter-spacing: 8px;
                                font-weight: bold;
                            }
                            .note {
                                color: #666;
                                font-size: 14px;
                                text-align: center;
                                margin-top: 20px;
                            }
                            .footer {
                                text-align: center;
                                margin-top: 30px;
                                padding-top: 20px;
                                border-top: 1px solid rgba(255,255,255,0.1);
                                color: #888;
                                font-size: 12px;
                            }
                            .p{
                                color:#ffffff;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <div class="logo">Dansela</div>
                            </div>
                            <p><font color="#ffffff">尊敬的领主大人：</font></p>
                            <p><font color="#ffffff">这是您今天的事务密令，如果不是您本人的操作要尽快检查领地安全呀：</font></p>
                            <div class="code-container">
                                <div class="code">%s</div>
                            </div>
                            <p class="note">验证码有效期为5分钟。如非您本人的操作，请及时检查系统安全。</p>
                            <div class="footer">
                                <p>此邮件由系统自动发送，请勿回复</p>
                                <p>© 2025 Dansela. All rights reserved.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """, code);
            } else {
                htmlContent = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { 
                                font-family: 'Helvetica Neue', Arial, sans-serif;
                                line-height: 1.6;
                                color: #333;
                                max-width: 600px;
                                margin: 0 auto;
                                padding: 20px;
                            }
                            .container {
                                background: linear-gradient(135deg, #1a1a1a, #2a2a2a);
                                border-radius: 16px;
                                padding: 30px;
                                box-shadow: 0 8px 32px rgba(0,0,0,0.1);
                                border: 1px solid rgba(255,255,255,0.1);
                            }
                            .header {
                                text-align: center;
                                margin-bottom: 30px;
                            }
                            .logo {
                                font-size: 24px;
                                color: #00b7ff;
                                font-weight: bold;
                                text-shadow: 0 0 10px rgba(0,183,255,0.3);
                            }
                            .code-container {
                                background: rgba(0,183,255,0.1);
                                border: 1px solid rgba(0,183,255,0.3);
                                border-radius: 8px;
                                padding: 20px;
                                text-align: center;
                                margin: 20px 0;
                            }
                            .code {
                                font-size: 32px;
                                color: #00b7ff;
                                letter-spacing: 8px;
                                font-weight: bold;
                            }
                            .note {
                                color: #666;
                                font-size: 14px;
                                text-align: center;
                                margin-top: 20px;
                            }
                            .footer {
                                text-align: center;
                                margin-top: 30px;
                                padding-top: 20px;
                                border-top: 1px solid rgba(255,255,255,0.1);
                                color: #888;
                                font-size: 12px;
                            }
                            .p{
                                color:#ffffff;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <div class="logo">Dansela</div>
                            </div>
                            <p><font color="#ffffff">我尊敬的大人：</font></p>
                            <p><font color="#ffffff">我来给您派发领主大人颁发的友令（验证码）啦,请您在友令中输入：</font></p>
                            <div class="code-container">
                                <div class="code">%s</div>
                            </div>
                            <p class="note">友令有效期为5分钟。如非大人您本人的操作，请忽略此邮件哦。</p>
                            <div class="footer">
                                <p>此邮件由系统自动发送，请勿回复</p>
                                <p>© 2025 Dansela. All rights reserved.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """, code);
            }
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
        } catch (Exception e) {
            logger.error("发送邮件失败: ", e);
            throw new RuntimeException("发送邮件失败: " + e.getMessage());
        }
    }
    
    public void sendNewPassword(String to, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Dansela - 密码重置");
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { 
                            font-family: 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background: linear-gradient(135deg, #1a1a1a, #2a2a2a);
                            border-radius: 16px;
                            padding: 30px;
                            box-shadow: 0 8px 32px rgba(0,0,0,0.1);
                            border: 1px solid rgba(255,255,255,0.1);
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .logo {
                            font-size: 24px;
                            color: #00b7ff;
                            font-weight: bold;
                            text-shadow: 0 0 10px rgba(0,183,255,0.3);
                        }
                        .password-container {
                            background: rgba(0,183,255,0.1);
                            border: 1px solid rgba(0,183,255,0.3);
                            border-radius: 8px;
                            padding: 20px;
                            text-align: center;
                            margin: 20px 0;
                        }
                        .password {
                            font-size: 24px;
                            color: #00b7ff;
                            letter-spacing: 2px;
                            font-weight: bold;
                        }
                        .warning {
                            color: #ff4d4f;
                            font-size: 14px;
                            text-align: center;
                            margin-top: 20px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid rgba(255,255,255,0.1);
                            color: #888;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Dansela</div>
                        </div>                        
                        <p><font color="#ffffff">我尊敬的大人：</font></p>
                        <p><font color="#ffffff">我偷偷帮您把密码改好啦，您新的密码是：</font></p>
                        <div class="password-container">
                            <div class="password">%s</div>
                        </div>
                        <p class="warning">为了您的账户安全，请登录后立即修改此密码！</p>
                        <div class="footer">
                            <p>此邮件由系统自动发送，请勿回复</p>
                            <p>© 2025 Dansela. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, password);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
        } catch (Exception e) {
            throw new RuntimeException("发送邮件失败: " + e.getMessage());
        }
    }

    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Dansela - " + subject);
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { 
                            font-family: 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background: linear-gradient(135deg, #1a1a1a, #2a2a2a);
                            border-radius: 16px;
                            padding: 30px;
                            box-shadow: 0 8px 32px rgba(0,0,0,0.1);
                            border: 1px solid rgba(255,255,255,0.1);
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .logo {
                            font-size: 24px;
                            color: #00b7ff;
                            font-weight: bold;
                            text-shadow: 0 0 10px rgba(0,183,255,0.3);
                        }
                        .content {
                            background: rgba(0,183,255,0.1);
                            border: 1px solid rgba(0,183,255,0.3);
                            border-radius: 8px;
                            padding: 20px;
                            margin: 20px 0;
                            white-space: pre-wrap;
                            color: #ffffff;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid rgba(255,255,255,0.1);
                            color: #888;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Dansela</div>
                        </div>
                        <div class="content">%s</div>
                        <div class="footer">
                            <p>此邮件由系统自动发送，请勿回复</p>
                            <p>© 2025 Dansela. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, content);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("邮件发送成功：to={}, subject={}", to, subject);
        } catch (Exception e) {
            logger.error("发送邮件失败: ", e);
            throw new RuntimeException("发送邮件失败: " + e.getMessage());
        }
    }
} 