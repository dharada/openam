/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.janua.identity.beans;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This singleton represents the configuration file
 * @author faime
 */
public class AppConfig {
    private static Properties configFile = null;

    /**
     * get a parameter from the configuration file
     * @param propertyName the name of the property
     * @return the value of the property
     */
    public static String getProperty(String propertyName) {
        if (configFile == null) {
            init();
        }
        return configFile.getProperty(propertyName);
    }

    private static void init() {
        try {
            configFile = new Properties();
            configFile.load(new FileInputStream(System.getProperty("catalina.home") + "/conf/sphinx.properties"));
        } catch (IOException ex) {
            Logger.getLogger(AppConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private AppConfig() {
        init();
    }
}
