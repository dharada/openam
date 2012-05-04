/*
 * (C) Copyright Janua 2010 - Author : Frédéric Aime (faime@janua.fr)
 * This code has been provided only for informational purpose
 * Deliverables may not be generated from this code,
 * License details are available upon express demand to : contact@janua.fr
 *
 * You can studdy this code as far as you want, using and/or modifying it is
 * subject to licence terms available upon demand.
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
