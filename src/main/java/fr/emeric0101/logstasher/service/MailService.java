package fr.emeric0101.logstasher.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

 //   @Autowired
 //   public JavaMailSender emailSender;

    public void sendSimpleMessage(
            String to, String subject, String text) {
      /*  try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply.child2.VALIDATION@airbus.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }
}
