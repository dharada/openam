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


import java.util.ArrayList;
import java.util.List;

/**
 * Static Helper Accessor class to Access the protected SMDataLayer.
 * Allows us to securely hack around the accessing a package protected resource.
 *
 * @author jeff.schenk@forgerock.com
 */
public class OpenDJSMDataLayerAccessor {

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
    private static OpenDJSMDataLayerAccessor instance = new OpenDJSMDataLayerAccessor();
    /**
     * Reference to Internally Shared SM Data Layer.
     */
    private static SMDataLayer sharedSMDataLayer = SMDataLayer.getInstance();
    /**
     * Security Stack Pattern to verify who
     * is trying to Access.
     */
    private static List<String> accessStackPattern;

    /**
     * Do not allow this class to be instantiated.
     */
    private OpenDJSMDataLayerAccessor() {
    }

    /**
     * This is the Stack Trace we are Expecting to Allow access to
     * our getInstance Accessor.
     */
    static {
        accessStackPattern = new ArrayList<String>(5);
        accessStackPattern.add("java.lang.Thread:getStackTrace:");
        accessStackPattern.add("com.sun.identity.sm.ldap.OpenDJSMDataLayerAccessor:getSharedSMDataLayerAccessor:");
        accessStackPattern.add("org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore:prepareBackEndPersistenceStore:");
        accessStackPattern.add("org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore:initialize:");
        accessStackPattern.add("org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore:getInstance:");
    }

    /**
     * Allow restricted Access by specific Class Caller without using
     * Java Security.
     *
     * @return OpenDJSMDataLayerAccessor - Wrapper Accessor Class for Shared SM Data Layer Instance.
     */
    public static synchronized OpenDJSMDataLayerAccessor getSharedSMDataLayerAccessor() {
        if (accessStackPattern == null) {
            return null;
        }
        // We now will Interrogate the Stack Trace for a precise Stack Access Pattern.
        // If this Stack Pattern is not found, then we will not allow Access to the
        // the Singleton Instance Wrapper.
        int index = 0;
        String saveEntry = null;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            String validateEntry = stackTraceElement.getClassName() + ":" + stackTraceElement.getMethodName() + ":";
            if (index >= accessStackPattern.size()) {
                break;
            }
            if (index == 1)
            { saveEntry = validateEntry; }
            if (accessStackPattern.get(index).equals(validateEntry)) {
                index++;
            }
        }
        // If we have determined we can allow this
        // method invocation, then return the instance.
        if (index == accessStackPattern.size()) {
            return instance;
        }
        String errorMessage = OpenDJSMDataLayerAccessor.class.getSimpleName() +
                " Internal Security Class Accessor Violation detected By "+saveEntry;
        throw new IllegalAccessError(errorMessage);
    }

    /**
     *
     * @param secureToken
     * @return LDAPConnection - Obtained from Pool
     */
    public LDAPConnection getConnection(String secureToken) {
        synchronized (instance) { connectionsObtained++; }
        return sharedSMDataLayer.getConnection();
    }

    /**
     *
     * @param secureToken
     * @param ldapConnection
     */
    public void releaseConnection(String secureToken, LDAPConnection ldapConnection) {
        synchronized (instance) { connectionsReleased++; }
        sharedSMDataLayer.releaseConnection(ldapConnection);
    }

    /**
     * @param secureToken
     * @param ldapConnection
     * @param ldapErrorCode
     */
    public void releaseConnection(String secureToken, LDAPConnection ldapConnection,int ldapErrorCode ) {
        synchronized (instance) { connectionsReleased++; }
        sharedSMDataLayer.releaseConnection(ldapConnection, ldapErrorCode);
    }

    /**
     * Private Helper Method to perform Validation of an Internally
     * calculated Token between this Class and the End Consumer Class
     * of these Methods.
     *
     * @param secureToken - Incoming Secure Token Calculated by calling Component.
     * @return boolean - indicates if request to Method is Authorized or not.
     */
    private static boolean validateSecureMethodToken(String secureToken) {
        String generatedSecureToken = generateSecureMethodToken();
        if ( (generatedSecureToken == null) || (generatedSecureToken.isEmpty()) || (secureToken == null) || (secureToken.isEmpty()))
            { return false; }
        return (generatedSecureToken.equalsIgnoreCase(secureToken));
    }

    /**
     * Private Helper Method to Generate the Secure Token for Validation.
     * @return
     */
    private static String generateSecureMethodToken() {
        // TODO
        return "1";
    }
}
