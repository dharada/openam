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
 * $Id: SMSessionUtils.java,v 1.6 2012/05/15 09:55:28 jah Exp $
 *
 */


package com.sun.identity.authentication.siteminder;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

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
 * This class <code>SmSessionUtils</code> has methods for SiteMinder sessions.
 */
public class SMSessionUtils {

    private static Debug debugLog = Debug.getInstance("SiteMinder");
    private String smLoginURL = null;
    private String smLogoutURL = null;
    private String smCookieName = null;

    // Constructor with default values;
    public SMSessionUtils(String smLoginURL, String smLogoutURL, String smCookieName) {
      this.smLoginURL = smLoginURL;
      this.smLogoutURL = smLogoutURL;
      this.smCookieName = smCookieName;
    }

    // Create a SiteMinder session by accessing the login URL
    public int createSmSession(HttpServletRequest request, HttpServletResponse response, String userName, String credentials)
      throws Exception {

      int respCode = 500;
      URL myurl = null;

      if (debugLog.messageEnabled()) {
        debugLog.message("SMSessionUtils.createSmSession() begin.");
      }

      if (smCookieName == null) {
        debugLog.error("SMSessionUtils.createSmSession() smCookieName is not set.");
        throw new Exception("smCookieName is not set.");
      }

      if (smLoginURL == null) {
        debugLog.error("SMSessionUtils.createSmSession() smLoginURL is not set.");
        throw new Exception("smLoginURL is not set.");
      }

      try {
        myurl = new URL(smLoginURL);
      } catch (MalformedURLException e) {
        debugLog.error("SMSessionUtils.createSmSession() Malformed smLoginURL " + smLoginURL);
        throw new Exception("Malformed smLoginURL " + smLoginURL);
      }

      try {
        String authorization = userName + ":FMTOKEN" + credentials;
        if (debugLog.messageEnabled()) {
          debugLog.message("Unencoded Authorization : " + authorization);
        }
        Base64 codec = new Base64();
        authorization = new String(codec.encode(authorization.getBytes()));
        authorization = "Basic " + authorization;
        if (debugLog.messageEnabled()) {
          debugLog.message("Encoded Authorization : " + authorization);
        }

        HttpURLConnection con = (HttpURLConnection)myurl.openConnection();

        // Set connection timeout and SiteMinder request properties
        con.setConnectTimeout(5000);
        con.setRequestProperty("Cookie", "SMCHALLENGE=YES");
        con.setRequestProperty("Authorization", authorization);

        try {
          con.connect();
        } catch (IOException e) {
          debugLog.error("SMSessionUtils.createSmSession() Connection to smLoginURL failed.", e);
          throw new Exception("Connection to SMLoginURL Failed, reason=" + e.getMessage());
        }

        // Get response code and cookie
        respCode = con.getResponseCode();
        String setCookie = con.getHeaderField("Set-Cookie");
        if (debugLog.messageEnabled()) {
          debugLog.message("ResponseCode=" + respCode);
          debugLog.message("Set-Cookie=" + setCookie);
        }

        // Consume any content from the connection
        try {
          InputStream ins = con.getInputStream();
          InputStreamReader isr = new InputStreamReader(ins);
          BufferedReader in = new BufferedReader(isr);
          String inputLine;
          while ((inputLine = in.readLine()) != null)
          {
            // if (debugLog.messageEnabled()) {
            //   debugLog.message(inputLine);
            // }
          }
          in.close();
        } catch (IOException e) {
        }

        // Close the connection
        con.disconnect();

        // If Siteminder authenticated succesfully, add SM session cookie
        // to response headers.
        // Note that a redirect status could mean either success or failure
        // so we need to check for SM session cookie in the response
        if ((respCode == 200) || (respCode == 302)) {
          // Check if we got SM session cookie
          if (setCookie.startsWith(smCookieName + "=")) {
            if (debugLog.messageEnabled()) {
              debugLog.message("Siteminder authentication succesful, user=" + userName);
            }
            response.addHeader("Set-Cookie", setCookie);
            respCode = 200;
          }
          else {
            debugLog.error("SMSessionUtils.createSmSession() Siteminder authentication unsuccesful, user=" + userName + ", Set-Cookie=" + setCookie);
            respCode = 403; // Forbidden
          }
        }
        else {
            debugLog.error("SMSessionUtils.createSmSession() Siteminder authentication unsuccesful, user=" + userName + ", response=" + respCode);
        }
      } catch (Throwable t) {
        debugLog.error("SMSessionUtils.createSmSession() Caught Throwable.", t);
        throw new Exception("Siteminder session creation failed");
      }

      if (debugLog.messageEnabled()) {
        debugLog.message("SMSessionUtils.createSmSession() end.");
      }

      return respCode;
    } // end of createSmSession()

    // Destroy a SiteMinder session by accessing the logout URL
    public int destroySmSession(HttpServletRequest request, HttpServletResponse response, String userName, String credentials)
      throws Exception {

      if (smLogoutURL == null) {
        throw new Exception("SMLogoutURL is not set");
      }

      // This method is not implemented yet, just return 500
      return 500;

    } // end of destroySmSession()
}
