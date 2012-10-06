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
package com.iplanet.dpro.session.service;

import org.forgerock.openam.shared.session.ha.amsessionstore.AMSessionRepositoryType;

/**
 * Provide simple Pojo for passing addition property
 * information securely to the persistent Store.
 *
 * @author jeff.schenk@forgerock.com
 */
public class SessionServiceConfigurationReferenceObject implements java.io.Serializable {
    private static final long serialVersionUID = 101L;   //  10.1

    /**
     * AM Session Repository Type, always
     * Default to Embedded until we initialize.
     */
    private AMSessionRepositoryType amSessionRepositoryType;

    /**
     * Session Store User Name/Principal
     */
    private String sessionStoreUserName = null;

    /**
     * Session Store Password/Credentials
     */
    private String sessionStorePassword = null;

    /**
     * Session Repository URL,
     * Can be Null, if using Internal Embedded OpenDJ Instance or OpenAM Configuration Directory.
     * Otherwise will specify the LDAP or eventually an LDAP or HTTP RESTful URL to perform
     * persistence against.
     */
    private String sessionRepositoryURL = null;


    /**
     * Default Constructor with All applicable fields.
     *
     * @param amSessionRepositoryType
     * @param sessionStoreUserName
     * @param sessionStorePassword
     * @param sessionRepositoryURL
     */
    public SessionServiceConfigurationReferenceObject(AMSessionRepositoryType amSessionRepositoryType, String sessionStoreUserName, String sessionStorePassword, String sessionRepositoryURL) {
        this.amSessionRepositoryType = amSessionRepositoryType;
        this.sessionStoreUserName = sessionStoreUserName;
        this.sessionStorePassword = sessionStorePassword;
        this.sessionRepositoryURL = sessionRepositoryURL;
    }

    /**
     * default no parameter constructor.
     */
    public SessionServiceConfigurationReferenceObject() {
    }

    public AMSessionRepositoryType getAmSessionRepositoryType() {
        return amSessionRepositoryType;
    }

    public String getSessionStoreUserName() {
        return sessionStoreUserName;
    }

    public String getSessionStorePassword() {
        return sessionStorePassword;
    }

    public String getSessionRepositoryURL() {
        return sessionRepositoryURL;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("SessionServiceConfigurationReferenceObject");
        sb.append("{amSessionRepositoryType=").append(amSessionRepositoryType);
        sb.append(", sessionStoreUserName='").append(sessionStoreUserName).append('\'');
        sb.append(", sessionRepositoryURL='").append(sessionRepositoryURL).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
