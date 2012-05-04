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

import com.novell.ldap.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author faime
 */
public class UserProperties {

    private static final Logger LOG = Logger.getLogger(UserProperties.class.getName());
    private String ldapServer;
    private int ldapPort;
    private String ldapUser;
    private String ldapPassword;
    private String ldapBase;
    private String ldapUserBase;
    private String applicationUrlAttribute;
    private int ldapTimeout;
    private String activeTechnicalID;
    private LDAPAttributeSet activeAttributes;
    private String activeAppListTechnicalID;
    private LDAPAttributeSet [] activeAppListAttributes;
    private String activeApplicationCN;
    private String [] activeApplicationAttrs;

    public UserProperties() {
        ldapServer = AppConfig.getProperty("ldap_server");
        ldapPort = Integer.parseInt(AppConfig.getProperty("ldap_port"));
        ldapUser = AppConfig.getProperty("ldap_user");
        ldapPassword = AppConfig.getProperty("ldap_password");
        ldapBase = AppConfig.getProperty("ldap_base");
        ldapUserBase = AppConfig.getProperty("user_base") + "," + ldapBase;
        applicationUrlAttribute = AppConfig.getProperty("application_url_attribute");
        String sTimeout = AppConfig.getProperty("ldap_timeout");
        if (sTimeout != null) {
            ldapTimeout = Integer.parseInt(AppConfig.getProperty("ldap_timeout"));
        } else {
            ldapTimeout = 500;
        }
        activeTechnicalID = null;
        activeAttributes = null;
        activeAppListTechnicalID = null;
        activeAppListAttributes = null;
        activeApplicationCN = null;
        activeApplicationAttrs = null;
    }

    private LDAPConnection getConnection() throws LDAPException {
        LDAPConnection conn = new LDAPConnection();

        conn.connect(ldapServer, ldapPort);
        LOG.info("connected to directory");

        conn.bind(LDAPConnection.LDAP_V3, ldapUser, ldapPassword.getBytes());
        LOG.info("bound to directory");

        return conn;
    }

    private boolean refresh(String technicalID) {
        activeTechnicalID = technicalID;
        activeAttributes = null;
        LDAPConnection conn = null;

        try {
            conn = getConnection();
            String attrs[] = {"*", "+"};

            LOG.info("Search in progress");
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, "(cn=" + technicalID + ")", attrs, false, cons);

            if (res.hasMore() && res.getCount() > 0) {
                LOG.log(Level.INFO, "Technical ID : {0} found in the directory", technicalID);

                LDAPEntry e = res.next();
                activeAttributes = e.getAttributeSet();
                if (activeAttributes == null) {
                    LOG.log(Level.SEVERE, "Cannot retrieve the technical IDs.");
                    return false;
                }
                LOG.log(Level.INFO, "Attributes retrieved.");
            }
        } catch (LDAPException ex) {
            LOG.severe(("Cannot connect to the LDAP server : " + ex.getResultCode() + "\n" + ex.getMessage()));
            LOG.log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (LDAPException unbindException) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, unbindException);
            }
            return false;
        }
        
        return true;
    }

    /**
     *
     * @param technicalID the technical ID of the user to fetch attribute from
     * @param attribute the name of the attribute to be fetched
     * @return the attribute value as a String
     */
    public String getProperty(String technicalID, String attribute) {
        
        if (technicalID == null) {
            return null;
        }
        
        if (!technicalID.equals(activeTechnicalID) || activeAttributes == null) {
            if (!refresh(technicalID)) {
                return null;
            }
        }

        LDAPAttribute theAttribute = activeAttributes.getAttribute(attribute);
        if (theAttribute == null) {
            LOG.log(Level.SEVERE, "{0} attribute cannot be found in the direcotry", attribute);
            return null;
        } else {
            LOG.log(Level.INFO, "L''attribut : {0} found in the directory", attribute);
            return theAttribute.getStringValue();
        }
    }

    /**
     *
     * @param technicalID the technical ID of the user to set an attribute
     * @param attribute the name of the attribute to modify
     * @param value the value of the attribute
     * @return true on success, false otherwise
     */
    public boolean setProperty(String technicalID, String attribute, String value) {
        LDAPConnection conn = null;
        if (technicalID == null) {
            return false;
        }
        String theDN = "cn=" + technicalID + "," + ldapUserBase;

        try {
            conn = getConnection();
            LDAPAttribute theAttribute = new LDAPAttribute(attribute, value);
            LDAPModification mod = new LDAPModification(LDAPModification.REPLACE, theAttribute);
            conn.modify(theDN, mod);
        } catch (LDAPException ex) {
            try {
                conn.disconnect();
            } catch (LDAPException ex1) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        }

        activeTechnicalID = null;
        return true;
    }
    
    public boolean addProperty(String technicalID, String attribute, String value) {
        LDAPConnection conn = null;
        if (technicalID == null) {
            return false;
        }
        String theDN = "cn=" + technicalID + "," + ldapUserBase;

        try {
            conn = getConnection();
            LDAPAttribute theAttribute = new LDAPAttribute(attribute, value);
            LDAPModification mod = new LDAPModification(LDAPModification.ADD, theAttribute);
            conn.modify(theDN, mod);
        } catch (LDAPException ex) {
            try {
                conn.disconnect();
            } catch (LDAPException ex1) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        }

        activeTechnicalID = null;
        return true;
    }

    public boolean clearProperty(String technicalID, String attribute) {
        LDAPConnection conn = null;
        if (technicalID == null) {
            return false;
        }
        String theDN = "cn=" + technicalID + "," + ldapUserBase;

        try {
            conn = getConnection();
            LDAPAttribute theAttribute = new LDAPAttribute(attribute);
            LDAPModification mod = new LDAPModification(LDAPModification.DELETE, theAttribute);
            conn.modify(theDN, mod);

        } catch (LDAPException ex) {
            try {
                conn.disconnect();
            } catch (LDAPException ex1) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        activeTechnicalID = null;
        return true;
    }

    public boolean clearProperty(String technicalID, String attribute, String value) {
        LDAPConnection conn = null;
        if (technicalID == null) {
            return false;
        }
        String theDN = "cn=" + technicalID + "," + ldapUserBase;

        try {
            conn = getConnection();
            LDAPAttribute theAttribute = new LDAPAttribute(attribute,value);
            LDAPModification mod = new LDAPModification(LDAPModification.DELETE, theAttribute);
            conn.modify(theDN, mod);

        } catch (LDAPException ex) {
            try {
                conn.disconnect();
            } catch (LDAPException ex1) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        activeTechnicalID = null;
        return true;
    }

    public boolean unlockAccount(String technicalID) {
        LDAPConnection conn = null;
        String theDN = "cn=" + technicalID + "," + ldapUserBase;

        try {
            conn = getConnection();
            LDAPModification[] mod = new LDAPModification[2];
            mod[0] = new LDAPModification(LDAPModification.DELETE, new LDAPAttribute("description"));
            mod[1] = new LDAPModification(LDAPModification.DELETE, new LDAPAttribute("pwdAccountLockedTime"));
            conn.modify(theDN, mod);
        } catch (LDAPException ex) {
            try {
                conn.disconnect();
            } catch (LDAPException ex1) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        activeTechnicalID = null;
        return true;
    }
    
    private boolean refreshApplicationsList(String technicalID) {
        activeAppListTechnicalID = technicalID;
        activeAppListAttributes = null;
        LDAPConnection conn = null;
        String ldapFilter = "(&(" + applicationUrlAttribute + "=*)(uniqueMember=cn=" + technicalID + "," + ldapUserBase + "))";

        try {
            conn = getConnection();
            String attrs[] = {"cn",applicationUrlAttribute};
            LOG.log(Level.INFO, "Searching for : {0} in  base : {1}", new Object[]{ldapFilter, ldapBase});
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search("ou=Groups," + ldapBase, LDAPConnection.SCOPE_SUB, ldapFilter, attrs, false, cons);
            
            int cpt = 0;
            if (res.hasMore()) {
            }

            int count = res.getCount();
            LOG.log(Level.INFO, "fetched {0} entries", count);
            
            if (count != 0) {
                activeAppListAttributes = new LDAPAttributeSet[count];

                LDAPEntry theEntry = null;
                while (res.hasMore()) {
                    theEntry = res.next();
                    activeAppListAttributes[cpt++] = theEntry.getAttributeSet();
                }
            }
            else {
                activeAppListAttributes = null;
            }
            
        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return true;
    }

    public String[] getApplicationsName(String technicalID) {
            if (technicalID==null) {
                return new String[0];
            }
       
            if (!technicalID.equals(activeAppListTechnicalID) || activeAppListAttributes == null) {
                if (!refreshApplicationsList(technicalID)) {
                    return new String[0];
                }
            }
        
            if (activeAppListAttributes==null) {
                return new String[0];
            }
            
            String [] result = new String[activeAppListAttributes.length];
            for (int i=0; i<activeAppListAttributes.length; i++) {
                String theApplicationUrlAttribute = activeAppListAttributes[i].getAttribute(applicationUrlAttribute).getStringValue();
                String[] tokens = theApplicationUrlAttribute.split("\\|");
                result[i] = tokens[0];
            }
            
            return result;
    }

    public String[] getApplicationsURL(String technicalID) {
            
            if (technicalID==null) {
                return new String[0];
            }
        
            if (!technicalID.equals(activeAppListTechnicalID) || activeAppListAttributes == null) {
                if (!refreshApplicationsList(technicalID)) {
                    return new String[0];
                }
            }
        
            if (activeAppListAttributes==null) {
                return new String[0];
            }
            
            String [] result = new String[activeAppListAttributes.length];
            for (int i=0; i<activeAppListAttributes.length; i++) {
                String theApplicationUrlAttribute = activeAppListAttributes[i].getAttribute(applicationUrlAttribute).getStringValue();
                String[] tokens = theApplicationUrlAttribute.split("\\|");
                result[i] = tokens[1];
            }
            
            return result;
    }

    public String[] getAllApplicationsCN() {
        LDAPConnection conn = null;
        String[] result = null;

        String ldapFilter = "(&(objectClass=groupOfUniqueNames)(!(cn=GNC)))";
        try {
            conn = getConnection();

            String attrs[] = {"cn"};

            LOG.log(Level.INFO, "Searching for : {0} in  base : {1}", new Object[]{ldapFilter, ldapBase});
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search("ou=Groups," + ldapBase, LDAPConnection.SCOPE_SUB, ldapFilter, attrs, false, cons);

            int cpt = 0;
            if (res.hasMore()) {
            }

            cpt = 0;

            int count = res.getCount();

            LOG.log(Level.INFO, "fetched {0} entries", count);


            result = new String[count];
            LDAPEntry theEntry = null;
            while (res.hasMore()) {
                theEntry = res.next();
                String theApplicationUrlAttribute = theEntry.getAttribute("cn").getStringValue();
                result[cpt++] = theApplicationUrlAttribute;
            }


            return result;
        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }

    public boolean hasApplication(String technicalID, String applicationCN) {
        LDAPConnection conn = null;

        String ldapFilter = "(&(cn=" + applicationCN + ")(uniqueMember=cn=" + technicalID + "," + ldapUserBase + "))";
        try {
            conn = getConnection();

            String attrs[] = {"cn"};

            LOG.log(Level.INFO, "Searching for : {0} in  base : {1}", new Object[]{ldapFilter, ldapBase});
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search("ou=Groups," + ldapBase, LDAPConnection.SCOPE_SUB, ldapFilter, attrs, false, cons);

            if (res.hasMore()) {
                return true;
            } else {
                return false;
            }
        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public String[] getApplicationsCN(String technicalID) {
            
        if (technicalID==null) {
            return new String[0];
        }
        
        if (!technicalID.equals(activeAppListTechnicalID) || activeAppListAttributes == null) {
                if (!refreshApplicationsList(technicalID)) {
                    return new String[0];
                }
            }
        
        if (activeAppListAttributes==null) {
            return new String[0];
        }
        
        String [] result = new String[activeAppListAttributes.length];
            for (int i=0; i<activeAppListAttributes.length; i++) {
                result[i] = activeAppListAttributes[i].getAttribute("cn").getStringValue();
            }
            
        return result;
        
    }

    /**
     *
     * @param applicationCN the cn of the group describing the application
     * @return a String describing the URL of the application
     */
    public String getApplicationURL(String applicationCN) {
            
        if (applicationCN==null) {
            return "";
        }
        
        if (!applicationCN.equals(activeApplicationCN) || activeApplicationAttrs == null) {
            if (!refreshApplication(applicationCN)) {
                return "";
            }
        }
        
        if (activeApplicationAttrs == null) {
            return "";
        }
        
        return activeApplicationAttrs[1];
        
    }

    /**
     *
     * @param applicationCN the cn of the group describing the application
     * @return a String describing the name of the application
     */
    public String getApplicationName(String applicationCN) {
            
        if (applicationCN==null) {
            return "";
        }
        
        if (!applicationCN.equals(activeApplicationCN) || activeApplicationAttrs == null) {
            if (!refreshApplication(applicationCN)) {
                return "";
            }
        }
        
        if (activeApplicationAttrs == null) {
            return "";
        }
        
        return activeApplicationAttrs[0];
    }
    
    private boolean refreshApplication(String applicationCN) {
        activeApplicationCN = applicationCN;
        activeApplicationAttrs = null;
        
        LDAPConnection conn = null;

        String ldapFilter = "(&(" + applicationUrlAttribute + "=*)(cn=" + applicationCN + "))";
        try {
            conn = getConnection();

            String attrs[] = {"cn", applicationUrlAttribute};

            LOG.log(Level.INFO, "Searching for : {0} in  base : {1}", new Object[]{ldapFilter, ldapBase});
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search("ou=Groups," + ldapBase, LDAPConnection.SCOPE_SUB, ldapFilter, attrs, false, cons);

            int count = res.getCount();

            LOG.log(Level.INFO, "fetched {0} entries", count);

            LDAPEntry theEntry = null;
            while (res.hasMore()) {
                theEntry = res.next();
                LDAPAttribute theAttribute = theEntry.getAttribute(applicationUrlAttribute);
                String theApplicationUrlAttribute = null;
                if (theAttribute != null) {
                    theApplicationUrlAttribute = theAttribute.getStringValue();
                    String[] tokens = theApplicationUrlAttribute.split("\\|");

                    if (tokens.length >= 2) {
                        activeApplicationAttrs = new String[]{tokens[0],tokens[1]};
                    } else {
                        LOG.log(Level.SEVERE, "Malformed description attribute");
                    }
                } else {
                    LOG.log(Level.INFO, "No application information for entry : {0}", applicationCN);
                }
            }
            if (activeApplicationAttrs == null) {
                LOG.log(Level.SEVERE, "Cannot retrieve the application data.");
                return false;
            }
        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return true;
    }

    /**
     * Deny access to all applications for the specified technicalID
     * @param technicalID
     */
    public void clearAllApps(String technicalID) {
        for (String s : getApplicationsCN(technicalID)) {
            clearApp(technicalID, s);
        }
    }

    /**
     * Deny access to a single app for user described by technicalID
     * @param technicalID ID of the user to clear application from
     * @param applicationCN the CN of the application to be denied access
     */
    public void clearApp(String technicalID, String applicationCN) {
        LDAPConnection conn = null;
        try {
            conn = getConnection();

            LDAPAttribute myAttribute = new LDAPAttribute("uniqueMember", "cn=" +technicalID + "," + ldapUserBase);

            String theDN = "cn=" + applicationCN + ",ou=Groups," + ldapBase;

            LDAPModification mod = new LDAPModification(LDAPModification.DELETE, myAttribute);
            conn.modify(theDN, mod);
            
            clearProperty(technicalID, "iplanet-am-static-group-dn", theDN);
            
            activeTechnicalID = null;
            activeAppListTechnicalID = null;

        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Grant access to a single application for user technicalID
     * @param technicalID technicalID describing the user
     * @param applicationCN the CN of the application to be granted access to
     * @return true on success false otherwise
     */
    public boolean addApplication(String technicalID, String applicationCN) {
        String theDN = "cn=" + applicationCN + ",ou=Groups," + ldapBase;
        String memberDN = "cn=" + technicalID + "," + ldapUserBase;
        LDAPAttribute theAttribute = new LDAPAttribute("uniqueMember", memberDN);

        LDAPConnection conn = null;

        try {
            conn = getConnection();

            LDAPModification mod = new LDAPModification(LDAPModification.ADD, theAttribute);
            conn.modify(theDN, mod);
            
            addProperty(technicalID, "iplanet-am-static-group-dn", theDN);

            activeTechnicalID = null;
            activeAppListTechnicalID = null;

            return true;
        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    /**
     * Check if given password is the actual password of the user
     * @param technicalID technicalID of the user
     * @param password password to be checked
     * @return true on success false otherwise
     */
    public boolean checkPassword(String technicalID, String password) {
        LDAPConnection conn = new LDAPConnection();
        LOG.log(Level.INFO, "Checking password for user : {0}", technicalID);

        try {
            String theUser = "cn=" + technicalID + "," + ldapUserBase;
            conn.connect(ldapServer, ldapPort);
            LOG.log(Level.INFO, "Checking password for user : {0}", theUser);
            conn.bind(LDAPConnection.LDAP_V3, theUser, password.getBytes());
            LOG.log(Level.INFO, "Password ok");
            return true;
        } catch (LDAPException ex) {
            LOG.log(Level.INFO, "Wrong Password : {0} Exception is : {1}", new Object[]{password, ex});
            return false;
        }
    }

    /**
     * Change password for the specified user
     * @param technicalID technicalID of the user
     * @param oldPassword the actual password of the user
     * @param newPassword the new password of the user
     * @return true on success false otherwise
     */
    public boolean changePassword(String technicalID, String oldPassword, String newPassword) {
        LDAPConnection conn = null;
        String userDN = "cn=" + technicalID + "," + ldapUserBase;
        LDAPModification[] modifications = new LDAPModification[2];
        try {
            conn = getConnection();
            LDAPAttribute deletePassword = new LDAPAttribute("userPassword", oldPassword);
            modifications[0] = new LDAPModification(LDAPModification.DELETE, deletePassword);

            LDAPAttribute addPassword = new LDAPAttribute("userPassword", newPassword);
            modifications[1] = new LDAPModification(LDAPModification.ADD, addPassword);

            conn.modify(userDN, modifications);
            conn.disconnect();
            activeTechnicalID = null;
            return true;
        } catch (LDAPException ex) {
            //Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (LDAPException ex1) {
                //Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex1);
            }
            return false;
        }
    }

    public String getTechnicalIdByUid(String identifiant) {
        LDAPConnection conn = null;

        try {
            conn = getConnection();
        } catch (LDAPException ex) {
            LOG.severe(("Cannot connect to the LDAP server : " + ex.getResultCode() + "\n" + ex.getMessage()));
            LOG.log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (Exception unbindException) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, unbindException);
            }
            return null;
        }
        String attrs[] = {"cn", "uid"};
        try {
            LOG.info("Search in progress");
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, "(uid=" + identifiant + ")", attrs, false, cons);
            int cpt = 0;

            if (res.hasMore() && res.getCount() > 0) {
                LOG.log(Level.INFO, "UID : {0} found in the directory", identifiant);

                LDAPEntry e = res.next();
                LDAPAttribute theAttribute = e.getAttribute("cn");
                if (theAttribute == null) {
                    LOG.log(Level.SEVERE, "The technical ID of UID = {0} cannot be found in the directory", identifiant);
                    return null;
                } else {
                    LOG.log(Level.INFO, "Technical ID of UID = {0} found in the directory", identifiant);
                    return theAttribute.getStringValue();
                }
            }
        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public String getTechnicalIdByMail(String email) {
        LDAPConnection conn = null;

        try {
            conn = getConnection();
        } catch (LDAPException ex) {
            LOG.severe(("Cannot connect to the LDAP server: " + ex.getResultCode() + "\n" + ex.getMessage()));
            LOG.log(Level.SEVERE, null, ex);
            try {
                conn.disconnect();
            } catch (LDAPException unbindException) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, unbindException);
            }
            return null;
        }
        String attrs[] = {"cn", "mail"};
        try {
            LOG.info("Search in progress");
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, "(mail=" + email + ")", attrs, false, cons);
            int cpt = 0;

            if (res.hasMore() && res.getCount() > 0) {
                LOG.log(Level.INFO, "L''UID : {0} found in the directory", email);

                LDAPEntry e = res.next();
                LDAPAttribute theAttribute = e.getAttribute("cn");
                if (theAttribute == null) {
                    LOG.log(Level.SEVERE, "The technical ID of {0} cannot be found in the directory", email);
                    return null;
                } else {
                    LOG.log(Level.INFO, "Technical ID of {0} found in the directory", email);
                    return theAttribute.getStringValue();
                }
            }
        } catch (LDAPException ex) {
            Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}