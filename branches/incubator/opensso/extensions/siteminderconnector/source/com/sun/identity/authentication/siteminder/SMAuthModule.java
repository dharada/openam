/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMAuthModule.java,v 1.3 2012/02/17 13:40:34 jah Exp $
 *
 */

package com.sun.identity.authentication.siteminder;

import java.util.Map;
import java.util.Set;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Date;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import netegrity.siteminder.javaagent.AgentAPI;
import netegrity.siteminder.javaagent.InitDef;
import netegrity.siteminder.javaagent.Attribute;
import netegrity.siteminder.javaagent.AttributeList;
import netegrity.siteminder.javaagent.ServerDef;
import netegrity.siteminder.javaagent.TokenDescriptor;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.debug.Debug;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Custom authentication module for validating siteminder user session
 * to enable SSO integration between OpenSSO and
 * Siteminder access server.
 * Siteminder is the trade mark of Computer Associates, the usage of the
 * Siteminder API is subject to Siteminder License terms.
 */
public class SMAuthModule extends AMLoginModule {

    private static final String COOKIE_NAME = "SMCookieName"; 
    private static final String SHARED_SECRET = "SharedSecret";
    private static final String SMHOSTFILE = "SmHostFile";
    private static final String SERVER_IP = "PolicyServerIPAddress";
    private static final String CHECK_REMOTE_USER_ONLY = "CheckRemoteUserOnly";
    private static final String TRUST_HOSTNAME = "TrustedHostName";
    private static final String ACCOUNT_PORT = "AccountingPort"; 
    private static final String AUTHN_PORT = "AuthenticationPort";
    private static final String AUTHZ_PORT = "AuthorizationPort";
    private static final String MIN_CONNECTION = "MinimumConnection";
    private static final String MAX_CONNECTION = "MaximumConnection"; 
    private static final String STEP_CONNECTION = "StepConnection";
    private static final String REQUEST_TIMEOUT = "RequestTimeout";
    private static final String REMOTE_USER_HEADER_NAME = 
                                "RemoteUserHeaderName";

    private static Debug debugLog = Debug.getInstance("SiteMinder");
    private boolean messageDebug = false;

    private String smCookieName = null;
    private String sharedSecret = null;    
    private String smHostFile = null;
    private String policyServerIP = null;
    private boolean checkRemoteUserOnly = false; 
    private String hostName = null;
    private int accountingPort = 44441;
    private int authenticationPort = 44442;
    private int authorizationPort = 44443;
    private int connectionMin = 2;
    private int connectionMax = 20;
    private int connectionStep = 2;
    private int timeout = 60;
    private String userId = null;
    private Principal userPrincipal = null;
    private String remoteUserHeader = "REMOTE_USER";
    private Set configuredHTTPHeaders = null;

    public SMAuthModule() throws LoginException{
        messageDebug = debugLog.messageEnabled();
        if (messageDebug) {
          debugLog.message("SMAuthModule() instantiation.");
        }
    }

    /**
     * Initialize the authentication module with it's configuration
     */
    public void init(Subject subject, Map sharedState, Map options) {
        if (messageDebug) {
            debugLog.message("SMAuthModule.init() begin options=" + options);
        }

        smCookieName = CollectionHelper.getMapAttr(options, 
                       COOKIE_NAME, "SMSESSION");

        sharedSecret = CollectionHelper.getMapAttr(options, SHARED_SECRET);
        smHostFile = CollectionHelper.getMapAttr(options, SMHOSTFILE);
        policyServerIP = CollectionHelper.getMapAttr(options, SERVER_IP);
        checkRemoteUserOnly = Boolean.valueOf(CollectionHelper.getMapAttr(
                   options, CHECK_REMOTE_USER_ONLY, "false")).booleanValue(); 
        hostName = CollectionHelper.getMapAttr(options, TRUST_HOSTNAME);
        configuredHTTPHeaders = (Set)options.get("HTTPHeaders");
        try {
            String tmp = CollectionHelper.getMapAttr(options, 
                     ACCOUNT_PORT, "44441");
            accountingPort = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options,
                  AUTHN_PORT, "44442"); 
            authenticationPort = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options,
                  AUTHZ_PORT, "44443");
            authorizationPort = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options, MIN_CONNECTION);
            connectionMin = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options, MAX_CONNECTION);
            connectionMax = Integer.parseInt(tmp);

            tmp =  CollectionHelper.getMapAttr(options, STEP_CONNECTION); 
            connectionStep = Integer.parseInt(tmp);

            tmp =  CollectionHelper.getMapAttr(options,  REQUEST_TIMEOUT);
            timeout = Integer.parseInt(tmp);
                  
        } catch (Exception e) {
            debugLog.error("SMAuthModule Caught exception parsing configuration settings.", e);
        }

        remoteUserHeader = CollectionHelper.getMapAttr(options,
                           REMOTE_USER_HEADER_NAME, "REMOTE_USER");
        
        // Set shared secret and trusted host name. Note that values read
        // from SmHostFile will override the ones set in module configuration.
        parseSmHostFile();

        if (messageDebug) {
            debugLog.message("SMAuthModule.init() end");
        }
    } 

    private boolean parseSmHostFile() {
      if ((smHostFile == null) || (smHostFile.length() == 0)) {
        debugLog.warning("SmAuthmodule: SmHostFile is not set.");
        return false;
      }

      Properties prop = new Properties();
      try {
        FileInputStream fis = new FileInputStream(smHostFile);
        prop.load(fis);
        fis.close();
      }
      catch (IOException ex) {
        debugLog.error("SMAuthModule Caught Exception reading SmHostFile.", ex);
        return false;
      }

      // Read shared secret and trusted host name from SmHost.conf
      if ((sharedSecret = prop.getProperty("sharedsecret")) == null) {
        debugLog.error("SMAuthModule: Unable to read shared secret from SmHostFile.");
        return false;
      }
      if ((hostName = prop.getProperty("hostname")) == null) {
        debugLog.error("SMAuthModule: Unable to read trusted host name from SmHostFile.");
        return false;
      }

      // Remove quotes from the strings
      sharedSecret = sharedSecret.substring(1,(sharedSecret.length()-1));
      hostName = hostName.substring(1,(hostName.length()-1));

      if (messageDebug) {
        debugLog.message("SMAuthModule: Hostname=" + hostName + ", Shared Secret=" + sharedSecret);
      }

      return true;
    }

    /**
     * This method process the login procedure for this authentication
     * module. In this auth module, if the user chooses to just validate
     * the HTTP headers set by the siteminder agent, this will not further
     * validate the SMSESSION by the siteminder SDK since the same thing
     * might have already been validated by the agent.
     */
    public int process(Callback[] callbacks, int state) 
                 throws AuthLoginException {

        if (messageDebug) {
            debugLog.message("SMAuthModule.process() start");
        }
        // Extra logging to stdout
        System.out.println("SMAuthModule.process() start at " + (new Date()).toString());

        HttpServletRequest request = getHttpServletRequest();

        if(configuredHTTPHeaders != null) {
           request.setAttribute("SM-HTTPHeaders", configuredHTTPHeaders);
        }
        if(checkRemoteUserOnly) {
           Enumeration headers = request.getHeaderNames();
           while(headers.hasMoreElements()) {
               String headerName = (String)headers.nextElement();
               if(headerName.equals(remoteUserHeader)) {
                  userId = request.getHeader(headerName);
               }
           }
           if(userId == null) {
              throw new AuthLoginException("No remote user header found");
           }
           return ISAuthConstants.LOGIN_SUCCEED;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
          throw new AuthLoginException("HttpServletRequest.getCookies() returned null");
        }

        String SMCookie =  null;
        boolean cookieFound = false;
        for (int i=0; i < cookies.length; i++) {
           Cookie cookie = cookies[i];
           if (cookie.getName().equals(smCookieName)) {
              cookieFound = true;
              String value = cookie.getValue();
              if (messageDebug) {
                  debugLog.message("SiteMinder cookie " + smCookieName + "=" + value);
              }
              value = value.replaceAll(" ", "+");
              value = value.replaceAll("%3D", "=");
              // debugLog.message("SMSESSION cookie value afer replacing=" + value);
              SMCookie = value;
          } // if cookie == SMSESSION
        } // for cookies

        if (!cookieFound) {
            debugLog.warning("SMAuthModule: No SiteMinder cookie in request");
            throw new AuthLoginException("SMAuthModule: No SiteMinder cookie in request");
        }

        if (SMCookie.equals("LOGGEDOFF")) {
            debugLog.warning("SMAuthModule: Siteminder session is logged out");
            throw new AuthLoginException("SMAuthModule: Siteminder session is logged out");
        }

        // SmAgent begin
        InitDef id = new InitDef(hostName, sharedSecret, true, new ServerDef());
        String[] policyServerIPs = policyServerIP.split(",");
        for (int i = 0; i < policyServerIPs.length; i++) {
            id.addServerDef(policyServerIPs[i].trim(),
                            connectionMin,
                            connectionMax,
                            connectionStep,
                            timeout,
                            authorizationPort,
                            authenticationPort,
                            accountingPort);
        }
        AgentAPI agentAPI = new AgentAPI();
        int initStat = agentAPI.init(id);
        if (initStat == AgentAPI.SUCCESS) {
           if (messageDebug) {
               debugLog.message("SiteMinder AgentAPI init succesful");
           }
        }
        else {
          debugLog.error("SiteMinder AgentAPI init failed, status=" + initStat);
          throw new AuthLoginException("SiteMinder AgentAPI init failed, status=" + initStat);
        }

        // Decode the SMSESSION cookie with SiteMinder API
        int version = 0;
        boolean thirdParty = false;
        TokenDescriptor td = new TokenDescriptor(version, thirdParty);
        AttributeList al  = new AttributeList();
        StringBuffer token = new StringBuffer();
        int status = agentAPI.decodeSSOToken(SMCookie, td, al, true, token);

        // We don't need the Siteminder connection anymore, clean up
        agentAPI.unInit();

        if (status == AgentAPI.SUCCESS) {
          if (messageDebug) {
            debugLog.message("SiteMinder session decoded succesfully");
          }
        }
        else {
           debugLog.error("SMAuthModule: SMSession decode failed, status=" + status + ", SMSESSION=" + SMCookie);
           throw new AuthLoginException("SMAuthModule: SMSession decode failed, status=" + status);
        }

        // Get the userid from attributes returned from SMAgentAPI
        Enumeration attributes = al.attributes();
        while(attributes.hasMoreElements()) {
            Attribute attr =  (Attribute)attributes.nextElement();
            int attrId = attr.id;
            String attrValue = XMLUtils.removeNullCharAtEnd(new String(attr.value));
            // Extra debugging
            // debugLog.message("Attribute Id=" + attrId + ", value=" + attrValue);
/*
 *          // Use SiteMinder userDN as the userid
 *          if(attrId == AgentAPI.ATTR_USERDN) {
 *              userId = attrValue;
 *              if (messageDebug) {
 *                  debugLog.message("Setting userId=" + userId);
 *              }
 *          }
 */
            // Use SiteMinder username as the userid
            if(attrId == AgentAPI.ATTR_USERNAME) {
                userId = attrValue;
                if (messageDebug) {
                    debugLog.message("Setting userId=" + userId);
                }
            }
        } // while

        // Check that we actually got the userid
        if(userId == null) {
           debugLog.error("SMAuthModule: Failed to extract userid from SMSESSION");
           throw new AuthLoginException("SMAuthModule: Failed to extract userid from SMSESSION");
        }

        if (messageDebug) {
          debugLog.message("SiteMinder authentication succesful, user=" + userId);
          debugLog.message("SMAuthModule.process() end");
        }

        // Extra logging to stdout
        System.out.println("SiteMinder authentication succesful, user=" + userId);
        System.out.println("SMAuthModule.process() end at " + (new Date()).toString());

        return ISAuthConstants.LOGIN_SUCCEED;

    } // process

    /**
     * Returns the authenticated principal.
     * This is consumed by the authentication framework to set the 
     * principal
     */
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userId != null) {
            userPrincipal = new SMPrincipal(userId);
            return userPrincipal;
        } else {
            return null;
        }
    }
}
