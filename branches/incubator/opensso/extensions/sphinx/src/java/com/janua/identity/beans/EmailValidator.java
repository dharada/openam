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

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * This class is used to check the validity of the mail providen by the final user
 * @author faime
 */
public class EmailValidator {

    private String ldapServer;
    private int ldapPort;
    private String ldapUser;
    private String ldapPassword;
    private String ldapBase;
    private String ldapUserBase;
    private int ldapTimeout;
    private static final Logger LOG = Logger.getLogger(EmailValidator.class.getName());

    /**
     * Create a new instance from properties through AppConfig
     * @see AppConfig
     */
    public EmailValidator() {
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

    /**
     * Validate the form of an email address.
     *
     * <br/><br/>
     * Return true only if
     * <ul>
     * <li> aEmailAddress can successfully construct an
     * <li> when parsed with "@" as delimiter, aEmailAddress contains
     * two tokens which satisfy
     * </ul>
     *
     * <br/><br/>
     * The second condition arises since local email addresses, simply of the
     * form "albert", for example, are valid for
     * undesired.
     */
    public boolean isValidEmailAddress(String aEmailAddress) {
        if (aEmailAddress == null) {
            return false;
        }
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(aEmailAddress);
            if (!hasNameAndDomain(aEmailAddress)) {
                result = false;
            }
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    private static boolean hasNameAndDomain(String aEmailAddress) {
        String[] tokens = aEmailAddress.split("@");
        return (tokens.length == 2 && tokens[0].length() > 0 && tokens[1].length() > 0);
    }

    /**
     * Check if the email already exists in the directory
     * @param aEmailAddress
     * @return true if the email already exists
     */
    public boolean isEmailPresent(String aEmailAddress) {
        LDAPConnection conn = new LDAPConnection();
        try {
            conn.connect(ldapServer, ldapPort);
            LOG.info("Connected to the directory");
            conn.bind(LDAPConnection.LDAP_V3, ldapUser, ldapPassword.getBytes());
            LOG.info("Bound to the directory");
            String attrs[] = {"cn", "mail"};
            LOG.info("Search in progress");
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, "(mail=" + aEmailAddress + ")", attrs, false, cons);

            if (res.hasMore() && res.getCount() > 0) {
                LOG.log(Level.INFO, "{0} aldeady exists in the directory", aEmailAddress);

                return true;
            }

        } catch (LDAPException ex) {
            LOG.severe("Invalid LDAP server port in the configuration file");
            LOG.log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(UserProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return false;
    }
}
