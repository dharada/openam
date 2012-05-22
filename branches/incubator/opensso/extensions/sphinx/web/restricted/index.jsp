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
<%@page import="java.util.logging.Logger"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.util.Properties"%>
<%!    String technicalID = null;
    String openAMHome = "";
%>
<%
    technicalID = request.getHeader("SPHYNX_TECH_ID");
    if (technicalID == null) {
        out.print("Internal error, OpenAM Agent configuration problem on the reverse-proxy");
    }
    Properties configFile = null;
    try {
        configFile = new Properties();
        FileInputStream fs = new FileInputStream(System.getProperty("catalina.home") + "/conf/sphinx.properties");
        configFile.load(fs);

        openAMHome = configFile.getProperty("openam_url");
        fs.close();
    } catch (IOException ex) {
    }
%>
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Home screen</title>
        <link rel="stylesheet" href="css/styles.css" type="text/css">

        <link href="css/dropdown/dropdown.css" media="screen" rel="stylesheet" type="text/css" />

        <link href="css/dropdown/default.css" media="screen" rel="stylesheet" type="text/css" />
        <link href="css/dropdown/default.advanced.css" media="screen" rel="stylesheet" type="text/css" />
        <link rel="stylesheet" type="text/css" href="css/dropdown/style.css" />

        <script language="JavaScript" src="js/browserVersion.js"></script>
        <script language="JavaScript" src="js/auth.js"></script>
    </head>
    <body>
        <table border="0">
            <thead>
                <tr>
                    <th colspan="2">Your applications</th>
                </tr>
            </thead>
            <tbody>
                <%
                    int cpt = 0;
                    for (String s : userProperties.getApplicationsCN(technicalID)) {
                %>
                <tr>
                    <td><a href="<% out.print(userProperties.getApplicationURL(s));%>"><% out.print(userProperties.getApplicationName(s));%></a></td>
                </tr>
                <%    cpt++;
                    }
                %>
            </tbody>
        </table>

        <table border="0">
            <thead>
                <tr>
                    <td>
                        Sphinx functions
                    </td>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><a href="modifyuser.jsp">Edit my informations</a></td>
                </tr>
                <tr>
                    <td><a href="changepassword.jsp" target="_">Change my password</a></td>
                </tr>
                <tr>
                    <td><a href="deleteuser.jsp">Manage my subscriptions</a></td>
                </tr>
                <tr>
                    <td>
                        <a href="<%= openAMHome%>/UI/Logout">Logout from Sphinx</a>
                    </td>
                </tr>
            </tbody>
        </table>
    </body>
</html>
