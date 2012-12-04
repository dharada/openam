/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 */

package org.forgerock.openam.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.util.*;

public class OSGILoader {
    private static OSGILoader instance;

    protected FrameworkFactory frameworkFactory;
    protected Map<String, String> config;
    protected Framework framework;

    private OSGILoader() {
        try {
        frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        config = new HashMap<String, String>();

            config.put("org.osgi.framework.storage.clean","onFirstInit");
            config.put("gosh.args","--nointeractive");
        framework = frameworkFactory.newFramework(config);
        framework.start();
    } catch (Exception ex) {
        System.out.println("Exception :" + ex);
    }
}
    public static OSGILoader getInstance() {
        if ( instance == null) {
            instance = new OSGILoader();
        }
        return instance;

    }

    public void startup() {
        try {

            BundleContext context = framework.getBundleContext();
            List<Bundle> installedBundles = new LinkedList<Bundle>();

            installedBundles.add(context.installBundle(
                    "http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.command/0.12.0/org.apache.felix.gogo.command-0.12.0.jar"));
            installedBundles.add(context.installBundle(
                    "http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.runtime/0.10.0/org.apache.felix.gogo.runtime-0.10.0.jar"));
            installedBundles.add(context.installBundle(
                    "http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.shell/0.10.0/org.apache.felix.gogo.shell-0.10.0.jar"));
            installedBundles.add(context.installBundle(
                    "http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.shell/1.4.3/org.apache.felix.shell-1.4.3.jar"));
            installedBundles.add(context.installBundle(
                   "http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.shell.remote/1.1.2/org.apache.felix.shell.remote-1.1.2.jar"));

            for (Bundle bundle : installedBundles) {
                System.out.println("Starting: "+bundle);
                bundle.start();
            }
        } catch (Exception ex) {
            System.out.println("Exception :" + ex);
        }
    }
}