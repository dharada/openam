/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2011-2012 Progress Software Corporation
 *
 * $Id: SMCreateSessionPlugin.java,v 1.3 2012/02/17 13:40:34 jah Exp $
 *
 */


package com.sun.identity.authentication.siteminder;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Date;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import org.apache.commons.codec.binary.Base64;

/**
 * This class <code>SMCreateSessionPlugin</code> implements 
 * <code>AMPostAuthProcessInterface</code> and this post auth plug-in
 * will be used for creating a SiteMinder session.
 */
public class SMCreateSessionPlugin implements AMPostAuthProcessInterface {

    private static Debug debugLog = Debug.getInstance("SiteMinder");
    private boolean messageDebug = false;
    private String smLoginURL = null;

    public SMCreateSessionPlugin() {
      try {
        ResourceBundle res = ResourceBundle.getBundle("SMCreateSessionPlugin");
        this.smLoginURL = res.getString("SMLoginURL");
        this.messageDebug = debugLog.messageEnabled();
      }
      catch (Exception ex) {
        debugLog.error("SMCreateSessionPlugin() caught Exception.", ex);
      }
      if (messageDebug) {
        debugLog.message("SMCreateSessionPlugin instantiation, smLoginURL=" + this.smLoginURL);
      }
    }

    /**
     * Post processing on successful authentication.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest</code>
     *        parameters
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign token.
     * @exception AuthenticationException if there is an error.
     */
    public void onLoginSuccess(
        Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken
    ) throws AuthenticationException {
        
        if (messageDebug) {
            debugLog.message("SMCreateSessionPlugin.onLoginSuccess() begin.");
        }

        if (smLoginURL == null) {
           throw new AuthenticationException("smLoginURL is not set");
        }

        int retCode = 500;
        String famSession = ssoToken.getTokenID().toString();
        if(famSession == null) {
           throw new AuthenticationException("No OpenSSO Session found");
        }
        AMIdentity amid = null;
        try {
            amid = new AMIdentity(ssoToken);
        } catch (Exception e) {
            throw new AuthenticationException("Unable to create AMIdentity, Exception=" + e.getMessage());
        }

        // If we already have SMSESSION then don't bother creating a new one
        Cookie[] cookies = request.getCookies();
        for (int i=0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            // if (messageDebug) {
            //     debugLog.message("Cookie name=" + cookie.getName() + ", value=" + cookie.getValue());
            // }
            if ((cookie.getName().equals("SMSESSION")) &&
                !(cookie.getValue().equals("LOGGEDOFF"))) {
                    if (messageDebug) {
                        debugLog.message("SMCreateSessionPlugin found existing SMSESSION, skipping session creation.");
                    }
                return;
            }
        } // for cookies

        if (messageDebug) {
            debugLog.message("Attempting SiteMinder login, user=" + amid.getName() + ", credentials=" + famSession);
        }

        try {
            SMSessionUtils smSessionUtils = new SMSessionUtils(smLoginURL, null);
            if (messageDebug) {
                smSessionUtils.setDebug(messageDebug);
            }
            retCode = smSessionUtils.createSmSession(request, response, amid.getName(), famSession);
        } catch (Exception e) {
            debugLog.error("createSmSession() returned Exception.", e);
        }

        if (retCode == 200) {
           // Extra logging to stdout
           System.out.println("createSmSession() succesful, user=" + amid.getName());
           if (messageDebug) {
             debugLog.message("createSmSession() succesful, user=" + amid.getName());
           }
        }
        else {
            debugLog.error("createSmSession() returned code " + retCode + ", user=" + amid.getName());
            throw new AuthenticationException("createSmSession() returned code " + retCode + ", user=" + amid.getName());
        }

        if (messageDebug) {
            debugLog.message("SMCreateSessionPlugin.onLoginSuccess() end.");
        }

        return;
    }

    /**
     * Post processing on failed authentication.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest<code>
     *        parameters.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @throws AuthenticationException when there is an error.
     */
    public void onLoginFailure(
        Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthenticationException {
        
    }
 
    /**
     * Post processing on Logout.
     *
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign on token.
     * @throws AuthenticationException
     */
    public void onLogout(
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken
    ) throws AuthenticationException {
        
    }

}
