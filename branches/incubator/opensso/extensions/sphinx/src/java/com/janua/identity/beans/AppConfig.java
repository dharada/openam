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
