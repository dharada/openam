/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) Janua 2011
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 * This class is used in the provisionning workflow in order to inform the final user through email
 * @author faime
 */
public class NotifyNewUser {

    private static final Logger LOG = Logger.getLogger(NotifyNewUser.class.getName());

    /**
     * send activation link to the final user
     * @param technicalID technicalID describing the user
     * @param email the email of the final user
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
//                email = URLEncoder.encode(email, "UTF-8");
                byte[] bytesOfMessage = email.getBytes();

                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(bytesOfMessage);

                token = new String(thedigest);

                String stringBuffer = new String(buffer);
                stringBuffer = stringBuffer.replaceAll("#token", token);
                stringBuffer = stringBuffer.replaceAll("#sphynx_url", AppConfig.getProperty("sphynx_url"));
                stringBuffer = stringBuffer.replaceAll("#email", email);
                stringBuffer = stringBuffer.replaceAll("#technicalID", technicalID);

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

    /**
     * Sens the reset password link to the final user
     * @param technicalID technicalID describing the final user
     * @return true on success false otherwise
     */
    public boolean sendResetPasswordLink(String technicalID, String token) {
        PasswordGenerator pwGenerator = new PasswordGenerator();
        String templateFileName = AppConfig.getProperty("new_password_template");
        File templateFile = new File(templateFileName);
        UserProperties ldapUserProperties = new UserProperties();
        String email = ldapUserProperties.getProperty(technicalID, "mail");
        String identifiant = ldapUserProperties.getProperty(technicalID, "uid");

        if (email == null) {
            return false;
        }
        String newPassword = pwGenerator.generatePassword();

        try {
            FileInputStream templateStream = new FileInputStream(templateFile);
            long fileLength = templateFile.length();
            byte[] buffer = new byte[(int) fileLength];

            int read = 0;
            try {
                read = templateStream.read(buffer);
                LOG.log(Level.INFO, "{0}bytes read", read);
                //email = URLEncoder.encode(email, "UTF-8");

                String stringBuffer = new String(buffer);
                stringBuffer = stringBuffer.replaceAll("#token", token);
                stringBuffer = stringBuffer.replaceAll("#sphynx_url", AppConfig.getProperty("sphynx_url"));
                stringBuffer = stringBuffer.replaceAll("#email", email);
                stringBuffer = stringBuffer.replaceAll("#technicalID", technicalID);
                stringBuffer = stringBuffer.replaceAll("#newPassword", newPassword);
                stringBuffer = stringBuffer.replaceAll("#UID", identifiant);

                send(email, AppConfig.getProperty("FromEmailAddress"), "Your new Sphinx password", stringBuffer);

                UserProperties props = new UserProperties();
                props.setProperty(technicalID, "userPassword", newPassword);
            } catch (IOException ex) {
                Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public boolean sendNewApplicationList(String technicalID, String[] applicationList) {
        PasswordGenerator pwGenerator = new PasswordGenerator();
        String templateFileName = AppConfig.getProperty("new_applications_template");
        File templateFile = new File(templateFileName);
        UserProperties ldapUserProperties = new UserProperties();
        String email = ldapUserProperties.getProperty(technicalID, "mail");
        String identifiant = ldapUserProperties.getProperty(technicalID, "uid");

        String applications = "<table border=\"0\">";
        for (String s : applicationList) {
            applications += "<tr><td>" + s + "</td></tr>";
        }
        applications += "</table>";

        if (email == null) {
            return false;
        }

        try {
            FileInputStream templateStream = new FileInputStream(templateFile);
            long fileLength = templateFile.length();
            byte[] buffer = new byte[(int) fileLength];

            int read = 0;
            try {
                read = templateStream.read(buffer);
                LOG.log(Level.INFO, "{0}bytes read", read);
                String token = null;
                //email = URLEncoder.encode(email, "UTF-8");
                byte[] bytesOfMessage = email.getBytes("UTF-8");

                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(bytesOfMessage);

                token = new String(thedigest);

                String stringBuffer = new String(buffer);
                stringBuffer = stringBuffer.replaceAll("#token", token);
                stringBuffer = stringBuffer.replaceAll("#sphynx_url", AppConfig.getProperty("sphynx_url"));
                stringBuffer = stringBuffer.replaceAll("#email", email);
                stringBuffer = stringBuffer.replaceAll("#appList", applications);
                stringBuffer = stringBuffer.replaceAll("#UID", identifiant);

                send(email, AppConfig.getProperty("FromEmailAddress"), "Your new Sphinx applications", stringBuffer);
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

    public boolean sendUnlockAccountLink(String technicalID, String token) {
        String templateFileName = AppConfig.getProperty("unlock_account_template");
        File templateFile = new File(templateFileName);
        UserProperties ldapUserProperties = new UserProperties();
        String email = ldapUserProperties.getProperty(technicalID, "mail");
        String identifiant = ldapUserProperties.getProperty(technicalID, "uid");

        if (email == null) {
            return false;
        }

        try {
            FileInputStream templateStream = new FileInputStream(templateFile);
            long fileLength = templateFile.length();
            byte[] buffer = new byte[(int) fileLength];

            int read = 0;
            try {
                read = templateStream.read(buffer);
                LOG.log(Level.INFO, "{0}bytes read", read);
                //email = URLEncoder.encode(email, "UTF-8");

                String stringBuffer = new String(buffer);
                stringBuffer = stringBuffer.replaceAll("#token", token);
                stringBuffer = stringBuffer.replaceAll("#sphynx_url", AppConfig.getProperty("sphynx_url"));
                stringBuffer = stringBuffer.replaceAll("#email", email);
                stringBuffer = stringBuffer.replaceAll("#technicalID", technicalID);
                stringBuffer = stringBuffer.replaceAll("#UID", identifiant);

                send(email, AppConfig.getProperty("FromEmailAddress"), "Your account has been locked", stringBuffer);

                UserProperties props = new UserProperties();
            } catch (IOException ex) {
                Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

        public boolean sendUserDeletedLink(String email, String uid) {
        String templateFileName = AppConfig.getProperty("user_deleted_template");
        File templateFile = new File(templateFileName);

        if (email == null) {
            return false;
        }

        try {
            FileInputStream templateStream = new FileInputStream(templateFile);
            long fileLength = templateFile.length();
            byte[] buffer = new byte[(int) fileLength];

            int read = 0;
            try {
                read = templateStream.read(buffer);
                LOG.log(Level.INFO, "{0}bytes read", read);
                //email = URLEncoder.encode(email, "UTF-8");

                String stringBuffer = new String(buffer);
                stringBuffer = stringBuffer.replaceAll("#sphynx_url", AppConfig.getProperty("sphynx_url"));
                stringBuffer = stringBuffer.replaceAll("#email", email);
                stringBuffer = stringBuffer.replaceAll("#UID", uid);

                send(email, AppConfig.getProperty("FromEmailAddress"), "Your account has been deleted", stringBuffer);

                UserProperties props = new UserProperties();
            } catch (IOException ex) {
                Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

        public boolean sendUserUpdatedLink(String email, String uid, String telephone) {
        String templateFileName = AppConfig.getProperty("user_modified_template");
        File templateFile = new File(templateFileName);

        if (email == null) {
            return false;
        }

        try {
            FileInputStream templateStream = new FileInputStream(templateFile);
            long fileLength = templateFile.length();
            byte[] buffer = new byte[(int) fileLength];

            int read = 0;
            try {
                read = templateStream.read(buffer);
                LOG.log(Level.INFO, "{0}bytes read", read);
                //email = URLEncoder.encode(email, "UTF-8");

                String stringBuffer = new String(buffer);
                stringBuffer = stringBuffer.replaceAll("#sphynx_url", AppConfig.getProperty("sphynx_url"));
                stringBuffer = stringBuffer.replaceAll("#email", email);
                stringBuffer = stringBuffer.replaceAll("#UID", uid);
                stringBuffer = stringBuffer.replaceAll("#telephone", telephone);

                send(email, AppConfig.getProperty("FromEmailAddress"), "Your account has been modified", stringBuffer);

                UserProperties props = new UserProperties();
            } catch (IOException ex) {
                Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(NotifyNewUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }


    /**
     * Low level send email
     * @param to the email recipient
     * @param from the email sender
     * @param subject the email subject
     * @param body the email body
     */
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
