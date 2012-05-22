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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class check for internet connectivity from the application server
 * @author faime
 */
public class CheckConnectivity {

    private int httpUrlConnectTimeout = 0;

    /**
     * Create a new instance and set it up from the configuration file
     */
    public CheckConnectivity() {
        String sHttpUrlConnectTimeout = AppConfig.getProperty("http_url_connect_timeout");
        if (sHttpUrlConnectTimeout != null) {
            httpUrlConnectTimeout = Integer.parseInt(sHttpUrlConnectTimeout);
        }
    }

    /**
     * check if the "test_url" is accessible (test_url is found through AppConfig.getProperty
     * @see AppConfig
     * @return true if the "test_url" is accessible, false otherwise
     */
    public boolean checkURL() {
        String theURL = AppConfig.getProperty("test_url");
        HttpURLConnection conn = null;
        URL url = null;
        try {
            url = new URL(theURL);

            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(httpUrlConnectTimeout);
            conn.setReadTimeout(httpUrlConnectTimeout);
        } catch (IOException ex) {
            Logger.getLogger(CheckConnectivity.class.getName()).log(Level.INFO, "Cannot reach the URL {0}", theURL);
            return false;
        }
        try {
            int responseCode = conn.getResponseCode();
        } catch (IOException ex) {
            Logger.getLogger(CheckConnectivity.class.getName()).log(Level.INFO, "Internet connexion not available, using a local captcha");
            return false;
        }
        return true;
    }
}
