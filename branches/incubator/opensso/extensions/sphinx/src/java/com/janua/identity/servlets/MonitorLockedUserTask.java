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
package com.janua.identity.servlets;

import com.janua.identity.beans.AppConfig;
import com.janua.identity.beans.NotifyNewUser;
import com.janua.identity.beans.UserProperties;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author faime
 */
public class MonitorLockedUserTask extends TimerTask {
    private final String ldapServer;
    private final int ldapPort;
    private final String ldapUser;
    private final String ldapPassword;
    private final String ldapBase;
    private final String ldapUserBase;
    private final String applicationUrlAttribute;

    public MonitorLockedUserTask() {
        ldapServer = AppConfig.getProperty("ldap_server");
        ldapPort = Integer.parseInt(AppConfig.getProperty("ldap_port"));
        ldapUser = AppConfig.getProperty("ldap_user");
        ldapPassword = AppConfig.getProperty("ldap_password");
        ldapBase = AppConfig.getProperty("ldap_base");
        ldapUserBase = AppConfig.getProperty("user_base") + "," + ldapBase;
        applicationUrlAttribute = AppConfig.getProperty("application_url_attribute");
        String sTimeout = AppConfig.getProperty("ldap_timeout");
    }

    private LDAPConnection getConnection() throws LDAPException {
        LDAPConnection conn = new LDAPConnection();

        conn.connect(ldapServer, ldapPort);
        LOG.info("connected to directory");

        conn.bind(LDAPConnection.LDAP_V3, ldapUser, ldapPassword.getBytes());
        LOG.info("bound to directory");

        return conn;
    }

    @Override
    public void run() {
        LDAPConnection conn = null;
        UserProperties userProperties = new UserProperties();
        try {
            conn = getConnection();
            String attrs[] = {"cn", "mail", "uid" };

            String ldapFilter = "(&(pwdAccountLockedTime=*)(!(description=*)))";

            LOG.log(Level.INFO, "Searching for : {0} in  base : {1}", new Object[]{ldapFilter, ldapUserBase});
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search(ldapUserBase, LDAPConnection.SCOPE_SUB, ldapFilter, attrs, false, cons);

            res.hasMore();

            int count = res.getCount();
            LOG.log(Level.INFO, "Trouvé " + count + " résultats");

            NotifyNewUser notifier = new NotifyNewUser();

            while(res.hasMore()) {
                LDAPEntry e = res.next();

                LOG.log(Level.SEVERE, "Processing user : {0}", e.getAttribute("uid").getStringValue());

                String technicalID = e.getAttribute("cn").getStringValue();
                String mail = e.getAttribute("mail").getStringValue();

                String unlockToken = java.util.UUID.randomUUID().toString();
                LOG.log(Level.SEVERE, "Storing generated UUID : {0}", unlockToken);
                userProperties.setProperty(technicalID, "description", unlockToken);

                notifier.sendUnlockAccountLink(technicalID, unlockToken);
            }
        } catch (LDAPException ex) {
            Logger.getLogger(MonitorLockedUserTask.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(MonitorLockedUserTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private static final Logger LOG = Logger.getLogger(MonitorLockedUserTask.class.getName());
}
