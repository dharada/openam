/*
 * (C) Copyright Janua 2010 - Author : Frédéric Aime (faime@janua.fr)
 * This code has been provided only for informational purpose
 * Deliverables may not be generated from this code,
 * License details are available upon express demand to : contact@janua.fr
 *
 * You can studdy this code as far as you want, using and/or modifying it is
 * subject to licence terms available upon demand.
 *
 */
package com.janua.identity.beans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author faime
 */
public class NotifyPasswordChange {

    private static final Logger LOG = Logger.getLogger(NotifyNewUser.class.getName());

    /**
     *
     * @param technicalID
     * @param email
     * @return true on success false otherwise
     */
    public boolean sendActivationLink(String technicalID, String email) {
        String templateFileName = AppConfig.getProperty("register_mail_template");

        File templateFile = new File(templateFileName);
        try {
            FileInputStream templateStream = new FileInputStream(templateFile);
            long fileLength = templateFile.length();
            byte[] buffer = new byte[(int) fileLength];

            int read = 0;
            try {
                read = templateStream.read(buffer);
                LOG.log(Level.INFO, "{0}bytes read", read);
                String token = null;

                byte[] bytesOfMessage = email.getBytes("UTF-8");

                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(bytesOfMessage);

                token = new String(thedigest);

                String stringBuffer = new String(buffer);
                stringBuffer.replaceAll("#token", token);
                stringBuffer.replaceAll("#sphynx_url", AppConfig.getProperty("sphynx_url"));
                stringBuffer.replaceAll("#email", email);
                stringBuffer.replaceAll("#technicalID", technicalID);

                send(email, AppConfig.getProperty("FromEmailAddress"), AppConfig.getProperty("RegistrationMailSubject"), stringBuffer);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public static void send(String to, String from, String subject, String body) {
        boolean auth = false;

        String smtpServer = AppConfig.getProperty("smtp_host");

        if (AppConfig.getProperty("smtp_use_auth").equals("Yes")) {
            auth = true;
        }

        if (AppConfig.getProperty("MailOnlyToTestAddress").equals("Yes")) {
            to = AppConfig.getProperty("MailTestAddress");
        }


        Logger.getLogger(NotifyNewUser.class.getName() + " send ").log(Level.INFO, "Sending {0} to : {1}", new Object[]{subject, to});

        try {
            Properties props = System.getProperties();
            // -- Attaching to default Session, or we could start a new one --
            props.put("mail.smtp.host", smtpServer);
            Session session = Session.getDefaultInstance(props, null);
            // -- Create a new message --
            Message msg = new MimeMessage(session);
            // -- Set the FROM and TO fields --
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            // -- We could include CC recipients too --
            // if (cc != null)
            // msg.setRecipients(Message.RecipientType.CC
            // ,InternetAddress.parse(cc, false));
            // -- Set the subject and body text --

            msg.setSubject(subject);
            msg.setText(body);
            // -- Set some other header information --
            msg.setHeader("X-Mailer", "DTSI_SPHYNX_SERVICE");
            msg.setHeader("Content-Type", "text/html");
            msg.setSentDate(new Date());
            // -- Send the message --
            Transport.send(msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
