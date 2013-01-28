/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.services;

import static org.testng.Assert.*;

import org.junit.runner.RunWith;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

import org.powermock.modules.junit4.PowerMockRunner;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;


/**
 * XACML Content Handler Test Suite
 *
 * @author Jeff.Schenk@ForgeRock.com
 */
@RunWith(PowerMockRunner.class)
public class TestXacmlContentHandlerService {

    private static ServletTester servletTester;

    @BeforeClass
    public void before() throws Exception {

        servletTester = new ServletTester();
        servletTester.addServlet(XacmlContentHandlerService.class, "/xacml");
        servletTester.start();
    }

    @AfterClass
    public void after() throws Exception {
        servletTester.stop();
    }

    @Test
    public void testUseCase_NoContentLengthSpecified() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.setHeader("Host", "tester");
        request.setURI("/xacml");
        request.setVersion("HTTP/1.0");

        try {
            // Check for a 411 No Content Length Provided.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),411);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_ZeroContentLengthSpecified() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.setHeader("Host", "tester");
        request.setURI("/xacml");
        request.setContent("");
        request.setVersion("HTTP/1.0");

        try {
            // Check for a 415 Unsupported Media Type.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),415);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }


}
