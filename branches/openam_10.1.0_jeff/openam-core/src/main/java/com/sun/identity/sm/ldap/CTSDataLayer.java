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
package com.sun.identity.sm.ldap;


import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPConnection;

/**
 * Protected Static Helper Accessor class to Access the protected SMDataLayer.
 * Allows us to securely hack around the accessing a package protected resource.
 *
 * @author jeff.schenk@forgerock.com
 */
class CTSDataLayer {

    /**
     * Debug Logging
     */
    private static Debug DEBUG = SessionService.sessionDebug;
    /**
     * Global Connection Pool Counters
     */
    private static int connectionsObtained = 0;
    private static int connectionsReleased = 0;
    /**
     * Singleton Instance.
     */
    private static CTSDataLayer instance = new CTSDataLayer();
    /**
     * Reference to Internally Shared SM Data Layer.
     */
    private static SMDataLayer sharedSMDataLayer = SMDataLayer.getInstance();
    /**
     * Do not allow this class to be instantiated.
     */
    private CTSDataLayer() {
    }

    /**
     * Allow restricted Access by specific Class Caller without using
     * Java Security.
     *
     * @return CTSDataLayer - Wrapper Accessor Class for Shared SM Data Layer Instance.
     */
    protected static CTSDataLayer getSharedSMDataLayerAccessor() {
        return instance;
    }

    /**
     *
     * @return LDAPConnection - Obtained from Pool
     */
    protected LDAPConnection getConnection() {
        synchronized (instance) { connectionsObtained++; }
        return sharedSMDataLayer.getConnection();
    }

    /**
     *
     * @param ldapConnection
     */
    protected void releaseConnection( LDAPConnection ldapConnection) {
        synchronized (instance) { connectionsReleased++; }
        sharedSMDataLayer.releaseConnection(ldapConnection);
    }

    /**
     *
     * @param ldapConnection
     * @param ldapErrorCode
     */
    protected void releaseConnection(LDAPConnection ldapConnection,int ldapErrorCode ) {
        synchronized (instance) { connectionsReleased++; }
        sharedSMDataLayer.releaseConnection(ldapConnection, ldapErrorCode);
    }

}
