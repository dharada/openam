/*
 * (C) Copyright Janua 2011 - Author : Frédéric Aime (faime@janua.fr)
 * This code has been provided only for informational purpose
 * Deliverables may not be generated from this code,
 * License details are available upon express demand to : contact@janua.fr
 *
 * You can studdy this code as far as you want, using and/or modifying it is
 * subject to licence terms available upon demand.
 *
 */
package com.janua.identity.beans;

import com.novell.ldap.*;
import com.novell.ldap.util.LDIFReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to perform provisionning actions on users
 * @author faime
 */
public class ProvisionUser {

    private String ldapServer;
    private int ldapPort;
    private String ldapUser;
    private String ldapPassword;
    private String ldapBase;
    private String ldapUserBase;
    private long ldapTimeout;

    /**
     * Creates a new instance from properties through AppConfig
     * @see AppConfig
     */
    public ProvisionUser() {
        ldapServer = AppConfig.getProperty("ldap_server");
        ldapPort = Integer.parseInt(AppConfig.getProperty("ldap_port"));
        ldapUser = AppConfig.getProperty("ldap_user");
        ldapPassword = AppConfig.getProperty("ldap_password");
        ldapBase = AppConfig.getProperty("ldap_base");
        ldapUserBase = AppConfig.getProperty("user_base") + "," + ldapBase;
        String sTimeout = AppConfig.getProperty("ldap_timeout");
        if (sTimeout != null) {
            ldapTimeout = Integer.parseInt(AppConfig.getProperty("ldap_timeout"));
        } else {
            ldapTimeout = 500;
        }
    }

    public String technicalID;

    private LDAPConnection getConnection() throws LDAPException {
        LDAPConnection conn = new LDAPConnection();

        conn.connect(ldapServer, ldapPort);
        LOG.info("connected to directory");

        conn.bind(LDAPConnection.LDAP_V3, ldapUser, ldapPassword.getBytes());
        LOG.info("bound to directory");

        return conn;
    }

    /**
     * Creates a new user in the directory from the template file described in property user_template
     * @param userID The userID choosen by the final user
     * @param password The password choosen by the final user
     * @param email The email choosen by the final user
     * @param telephone The telephone number choosen by the final user
     * @return true on success false otherwise
     */
    public String CreateUser(String userID, String password, String email, String telephone) {
        LDAPEntry entry;
        LDAPMessage retMsg;
        LDAPConnection conn = new LDAPConnection();
        technicalID = java.util.UUID.randomUUID().toString();
        try {
            conn = getConnection();
        } catch (LDAPException ex) {
            LOG.severe(("Cannot connect to the LDAP server : " + ex.getResultCode() + "\n" + ex.getMessage()));
            LOG.log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (LDAPException unbindException) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, unbindException);
            }
            return null;
        }

        try {
            File templateFile = new File(AppConfig.getProperty("user_template"));
            int length = (int) templateFile.length();

            FileInputStream templateStream;
            LDAPMessage msg;
            templateStream = new FileInputStream(templateFile);
            byte[] buffer;
            buffer = new byte[length];
            int read = templateStream.read(buffer);
            LOG.log(Level.INFO, "{0}bytes read", read);

            String stringBuffer = new String(buffer);
            stringBuffer = stringBuffer.replaceAll("#userID", userID);
            stringBuffer = stringBuffer.replaceAll("#password", password);
            stringBuffer = stringBuffer.replaceAll("#email", email);
            stringBuffer = stringBuffer.replaceAll("#telephoneNumber", telephone);
            stringBuffer = stringBuffer.replaceAll("#technicalID", technicalID);
            stringBuffer = stringBuffer.replaceAll("#userBase", AppConfig.getProperty("user_base"));
            stringBuffer = stringBuffer.replaceAll("#ldapBase", AppConfig.getProperty("ldap_base"));
            stringBuffer = stringBuffer.replaceAll("#defaultGroup", AppConfig.getProperty("default_group"));

            buffer = stringBuffer.getBytes();
            ByteArrayInputStream memoryStream = new ByteArrayInputStream(buffer);

            LOG.log(Level.INFO, "LDIF sent");

            LDIFReader reader = new LDIFReader(memoryStream);

            while ((msg = reader.readMessage()) != null) {
                if (msg instanceof LDAPAddRequest) {
                    entry = ((LDAPAddRequest) msg).getEntry();
                    LOG.log(Level.INFO, "Adding entry : {0}", entry.getDN());
                    conn.add(entry);
                } else if (msg instanceof LDAPModifyRequest) {
                    LDAPModification[] mod = ((LDAPModifyRequest) msg).getModifications();
                    String modifiedDN = ((LDAPModifyRequest) msg).getDN();
                    LOG.log(Level.INFO, "Modifing entry : {0} with mod object : {1}", new Object[]{modifiedDN, mod.toString()});
                    conn.modify(modifiedDN, mod);
                }
            }
            return technicalID;
        } catch (LDAPException ex) {
            Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException disconnectException) {
                LOG.log(Level.SEVERE, null, disconnectException);
            }
        }
    }

    /**
     * Check the validity of the providen userID
     * @param userID the requested userID
     * @return true if the userID is available and false otherwise
     */
    public boolean checkUserID(String userID) {

        LDAPConnection conn = null;
        try {
            conn = getConnection();
        } catch (LDAPException ex) {
            LOG.severe(("Cannot connect to the LDAP server : " + ex.getResultCode() + "\n" + ex.getMessage()));
            LOG.log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (LDAPException unbindException) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, unbindException);
            }
            return false;
        }
        String attrs[] = {"uid", "cn"};
        try {
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, "(uid=" + userID + ")", attrs, false, cons);

            if (res.hasMore() && res.getCount() > 0) {
                LOG.log(Level.INFO, "{0} already exists in the directory", userID);
                return false;
            } else {
                LOG.log(Level.INFO, "{0} is available in the directory", userID);
            }
        } catch (LDAPException ex) {
            Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    /**
     * Activate the user described by the technicalID
     * @param technicalID technicalID describing the user
     * @param token the security token given through the mail link
     * @return true on success false otherwise
     */
    public boolean activateUser(String technicalID, String token) {
        LOG.log(Level.INFO, "Activating user : {0}", technicalID);
        LDAPConnection conn = new LDAPConnection();
        try {
            conn = getConnection();
        } catch (LDAPException ex) {
            LOG.severe(("Cannot connect to the LDAP server : " + ex.getResultCode() + "\n" + ex.getMessage()));
            LOG.log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (LDAPException unbindException) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, unbindException);
            }
            return false;
        }
        String attrs[] = {"uid", "email"};
        try {
            LOG.log(Level.INFO, "Starting search in the directory");
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, "(cn=" + technicalID + ")", attrs, false, cons);

            if (res.hasMore() && res.getCount() > 0) {
                LOG.log(Level.INFO, "{0} found in the directory", technicalID);
                LDAPEntry e = res.next();
                String entryDN = e.getDN();

                LDAPAttribute attribute = new LDAPAttribute("inetUserStatus", "active");
                conn.modify(entryDN, new LDAPModification(LDAPModification.ADD, attribute));
                LOG.log(Level.INFO, "User locked");
            } else {
                LOG.log(Level.INFO, "User {0} cannot be found in the directory", technicalID);
                return false;
            }
        } catch (LDAPException ex) {
            Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    /**
     * Remove the user from the directory
     * @param technicalID technicalID describing the user
     * @return true upon success false otherwise
     */
    public boolean deleteUser(String technicalID) {
        UserProperties userProps = new UserProperties();
        LOG.log(Level.INFO, "Delete user: {0}", technicalID);
        LDAPConnection conn = new LDAPConnection();
        try {
            conn = getConnection();

            String theDN = "cn=" + technicalID + "," + ldapUserBase;
            LOG.log(Level.INFO, "deleting {0}", theDN);

            userProps.clearAllApps(technicalID);

            conn.delete(theDN);
        } catch (LDAPException ex) {
            LOG.severe("Invalid LDAP server port in the configuration file");
            LOG.log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException disconnectException) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, disconnectException);
            }
        }

        return true;
    }

    /**
     * Update user details
     * @param identifiant the user login id (uid)
     * @param technicalID the technicalID of the user to be updated
     * @param email the new email
     * @param telephone the new telephone number
     * @return true on success false otherwise
     */
    public boolean updateUser(String identifiant, String technicalID, String email, String telephone) {

        LOG.log(Level.INFO, "Updating user : {0}", technicalID);
        LDAPConnection conn = new LDAPConnection();
        try {
            conn = getConnection();
        } catch (LDAPException ex) {
            LOG.severe("Invalid LDAP server port in the configuration file");
            LOG.log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (LDAPException disconnectException) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, disconnectException);
            }
            return false;
        }

        String attrs[] = {"uid", "email", "telephonenumber"};
        try {
            LOG.log(Level.INFO, "Starting search in the directory");
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, "(cn=" + technicalID + ")", attrs, false, cons);

            if (res.hasMore() && res.getCount() > 0) {
                LOG.log(Level.INFO, "{0} found in the directory", technicalID);
                LDAPEntry e = res.next();
                String entryDN = e.getDN();

                LDAPAttribute attribute = new LDAPAttribute("uid", identifiant);
                conn.modify(entryDN, new LDAPModification(LDAPModification.REPLACE, attribute));

                attribute = new LDAPAttribute("mail", email);
                conn.modify(entryDN, new LDAPModification(LDAPModification.REPLACE, attribute));

                attribute = new LDAPAttribute("telephonenumber", telephone);
                conn.modify(entryDN, new LDAPModification(LDAPModification.REPLACE, attribute));
                
                LOG.log(Level.INFO, "User modified");
            } else {
                LOG.log(Level.INFO, "User {0} cannot be found in the directory", technicalID);
                return false;
            }
        } catch (LDAPException ex) {
            Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(ProvisionUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }
    private static final Logger LOG = Logger.getLogger(ProvisionUser.class.getName());
}