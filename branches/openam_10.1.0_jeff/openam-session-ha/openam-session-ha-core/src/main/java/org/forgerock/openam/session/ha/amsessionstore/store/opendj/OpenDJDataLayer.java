/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */
package org.forgerock.openam.session.ha.amsessionstore.store.opendj;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.exceptions.StoreException;
import com.iplanet.dpro.session.service.SessionServiceConfigurationReferenceObject;
import com.iplanet.services.ldap.*;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPBind;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * OpenDJDataLayer
 * Access An External LDAP Directory Server or VIP Cluster to provide
 * Session Failover Persistence.
 * <p/>
 * This was original cloned from the SMDataLayer Object.
 *
 * @author jeff.schenk@forgerock.com
 */
class OpenDJDataLayer {

    /**
     * Static section to retrieve the debug object.
     */
    private static Debug debug;

    /**
     * Session Service Configuration Reference Object
     * Provides additional information to use an external Directory outside
     * of the configured Configuration data store which is the default.
     */
    private static volatile SessionServiceConfigurationReferenceObject
            sessionServiceConfigurationReferenceObject;

    /**
     * Used when we have an explicit External Persistence Directory Store
     * besides using the normal Configuration Store in embedded or
     * external mode.
     */
    private static volatile URL externalLDAPConnectionURL;

    /**
     * Default maximum backlog queue size
     */
    static final int MAX_BACKLOG = 100;
    static final String LDAP_MAXBACKLOG = "maxbacklog";
    static final String LDAP_REFERRAL = "referral";

    private static OpenDJDataLayer m_instance = null;

    private LDAPConnectionPool _ldapPool = null;
    private LDAPSearchConstraints _defaultSearchConstraints = null;

    /**
     * OpenDJDataLayer constructor
     */
    private OpenDJDataLayer(SessionServiceConfigurationReferenceObject
                                    sessionServiceConfigurationReferenceObject) {
        OpenDJDataLayer.sessionServiceConfigurationReferenceObject =
                sessionServiceConfigurationReferenceObject;
        initLdapPool();
    }

    /**
     * Create the singleton OpenDJDataLayer object if it doesn't exist already.
     */
    protected synchronized static OpenDJDataLayer getInstance(
            SessionServiceConfigurationReferenceObject
                    sessionServiceConfigurationReferenceObject) {
        // Obtain the Debug instance.
        debug = Debug.getInstance("amSessionLdap");

        // Make sure only one instance of this class is created.
        if (m_instance == null) {

            m_instance = new OpenDJDataLayer(sessionServiceConfigurationReferenceObject);
        }
        return m_instance;
    }

    /**
     * Get connection from pool, not through LDAPProxy. Reauthenticate if
     * necessary
     *
     * @return connection that is available to use
     */
    protected LDAPConnection getConnection() {
        if (_ldapPool == null)
            return null;

        if (debug.messageEnabled()) {
            debug.message("OpenDJDataLayer:getConnection()-"
                    + "Invoking _ldapPool.getConnection()");
        }
        LDAPConnection conn = _ldapPool.getConnection();
        if (debug.messageEnabled()) {
            debug.message("OpenDJDataLayer:getConnection()-Got Connection : "
                    + conn);
        }

        return conn;
    }

    /**
     * Just call the pool method to release the connection so that the
     * given connection is free for others to use
     *
     * @param conn        connection in the pool to be released for others to use.
     * @param ldapErrCode ldap exception error code used to determine
     *                    failover.
     *                    iPlanet-PUBLIC-METHOD
     */
    protected void releaseConnection(LDAPConnection conn, int ldapErrCode) {
        if (_ldapPool == null || conn == null) return;

        // reset the original constraints
        // TODO: check with ldapjdk and see if this is appropriate
        //       to restore the default constraints.
        //
        conn.setSearchConstraints(_defaultSearchConstraints);

        // A soft close on the connection.
        // Returns the connection to the pool and
        // make it available.
        if (debug.messageEnabled()) {
            debug.message("OpenDJDataLayer:releaseConnection()-" +
                    "Invoking _ldapPool.close(conn,ldapErrCode) : " +
                    conn + ":" + ldapErrCode);
        }
        _ldapPool.close(conn, ldapErrCode);
        if (debug.messageEnabled()) {
            debug.message("OpenDJDataLayer:releaseConnection()-" +
                    "Released Connection:close(conn,ldapErrCode) : " + conn);
        }
    }

    /**
     * Just call the pool method to release the connection so that the given
     * connection is free for others to use
     *
     * @param conn connection in the pool to be released for others to use
     */
    protected void releaseConnection(LDAPConnection conn) {
        if (_ldapPool == null || conn == null)
            return;

        // reset the original constraints
        // TODO: check with ldapjdk and see if this is appropriate
        // to restore the default constraints.
        //
        conn.setSearchConstraints(_defaultSearchConstraints);

        // A soft close on the connection.
        // Returns the connection to the pool and make it available.
        if (debug.messageEnabled()) {
            debug.message("SMDataLayer:releaseConnection()-"
                    + "Invoking _ldapPool.close(conn) : " + conn);
        }
        _ldapPool.close(conn);
        if (debug.messageEnabled()) {
            debug.message("SMDataLayer:releaseConnection()-"
                    + "Released Connection : " + conn);
        }
    }

    /**
     * Closes all the open ldap connections
     */
    protected synchronized void shutdown() {
        if (_ldapPool != null) {
            _ldapPool.destroy();
        }
        _ldapPool = null;
        m_instance = null;
    }

    /**
     * Initialize the pool shared by all OpenDJDataLayer object(s).
     */
    private synchronized void initLdapPool() {
        // Dont' do anything if pool is already initialized
        if (_ldapPool != null) {
            return;
        }
        // Only Initialize if our Reference Object is Available.
        if (sessionServiceConfigurationReferenceObject == null) {
            return;
        }
        try {
            externalLDAPConnectionURL = new URL(sessionServiceConfigurationReferenceObject.getSessionRepositoryURL());
            if (!externalLDAPConnectionURL.getProtocol().startsWith("ldap")) {
                // TODO Support other Protocols, such as http[s] for restful calls...
                throw new IllegalArgumentException(
                        "Currently External Session Persistence only supports LDAP as a storage protocol, Protocol" +
                                externalLDAPConnectionURL.getProtocol() + ", is Invalid!");
            }
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("Invalid URL supplied for External Session Persistence Store, " +
                    "Session Failover will not be Available! " + mue.getMessage(), mue);
        }
        // Initialize the pool with minimum and maximum connections settings
        // retrieved from configuration
        HashMap connOptions = new HashMap();

        try {
            // Construct the pool by cloning the successful connection
            // Set the default options too for failover and fallback features.

            connOptions.put("maxbacklog", Integer.valueOf(1));
            connOptions.put("referrals", Boolean.valueOf(true));
            connOptions.put("searchconstraints", _defaultSearchConstraints);

            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    // Establish an independent LDAP Connection Pool to this Resource.
                    _ldapPool = new LDAPConnectionPool("OpenAM_SF_EXT_STORE_POOL",
                            sessionServiceConfigurationReferenceObject.getMinPoolSize(),
                            sessionServiceConfigurationReferenceObject.getMaxPoolSize(),
                            externalLDAPConnectionURL.getHost(),
                            externalLDAPConnectionURL.getPort(),
                            sessionServiceConfigurationReferenceObject.getSessionStoreUserName(),
                            sessionServiceConfigurationReferenceObject.getSessionStorePassword());
                    // Establish Shutdown Listener.
                    shutdownMan.addShutdownListener(
                            new ShutdownListener() {
                                public void shutdown() {
                                    if (_ldapPool != null) {
                                        _ldapPool.destroy();
                                    }
                                }
                            }
                    );
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            } // End of Check for Shutdown Lock Acquired.
        } catch (LDAPException e) {
            debug.error("OpenDJDataLayer:initLdapPool()-"
                    + "Exception in OpenDJDataLayer.initLdapPool:", e);
        }
    }
}
