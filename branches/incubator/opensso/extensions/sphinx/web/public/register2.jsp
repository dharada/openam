<%--
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
--%>
<%@page import="com.janua.identity.beans.AppConfig"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.util.Properties"%>
<%! String sphinxHome = "/openam"; // default value%>
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
<%
    Properties configFile = null;
    try {
        configFile = new Properties();
        FileInputStream fs = new FileInputStream(System.getProperty("catalina.home") + "/conf/sphinx.properties");
        configFile.load(fs);

        sphinxHome = configFile.getProperty("sphynx_url") + "/restricted/index.jsp";
        fs.close();
    } catch (IOException ex) {

    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Thank you for your registration</title>
    </head>
    <body>
        <h1>Registration successful</h1>
        <p>An activation link has been sent to your email</p>
            <table border="0">
                <thead>
                    <tr>
                        <th>Summary of your information :</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Username</td>
                        <td><% out.print(userProperties.getProperty(request.getParameter("technicalID"), "uid"));%></td>
                    </tr>
                    <tr>
                        <td>Email address</td>
                        <td><% out.print(userProperties.getProperty(request.getParameter("technicalID"), "mail"));%></td>
                    </tr>
                    <tr>
                        <td>Phone number</td>
                        <td><% out.print(userProperties.getProperty(request.getParameter("technicalID"), "telephonenumber"));%></td>
                    </tr>
                    <tr>
                        <td colspan="2" align="center"><input type="button" value="Login page" onclick="javascript:window.location='<%= sphinxHome %>'"/></td>
                    </tr>
                </tbody>
            </table>
    </body>
</html>
