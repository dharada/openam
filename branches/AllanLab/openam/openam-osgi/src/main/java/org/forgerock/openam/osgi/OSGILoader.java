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

    public OSGILoader() {

    }

    public void startup() {
        try {
            FrameworkFactory frameworkFactory = ServiceLoader.load(
                    FrameworkFactory.class).iterator().next();
            Map<String, String> config = new HashMap<String, String>();
// TODO: add some config properties
            Framework framework = frameworkFactory.newFramework(config);
            framework.start();

            BundleContext context = framework.getBundleContext();
            List<Bundle> installedBundles = new LinkedList<Bundle>();

            installedBundles.add(context.installBundle(
                    "file:org.apache.felix.shell-1.4.2.jar"));
            installedBundles.add(context.installBundle(
                    "file:org.apache.felix.shell.tui-1.4.1.jar"));

            for (Bundle bundle : installedBundles) {
                bundle.start();
            }
        } catch (Exception ex) {

        }
    }
}