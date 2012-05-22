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
<%@page import="java.io.IOException"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.util.Properties"%>
<%!    String openAMHome = "";
    String sphinxHome = "";
%>
<%
    Properties configFile = null;
    try {
configFile = new Properties();
        FileInputStream fs = new FileInputStream(System.getProperty("catalina.home") + "/conf/sphinx.properties");
        configFile.load(fs);

        sphinxHome = configFile.getProperty("sphynx_url");
        openAMHome = configFile.getProperty("openam_url");

        fs.close();
    } catch (IOException ex) {
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <script type="text/javascript">

            function redirect()
            {
                window.location = '<%= sphinxHome%>/restricted/index.jsp';
            }
        </script>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>You are being redirected</title>
    </head>
    <body onload="timer=setTimeout('redirect()', 2000)">
        <div id="logoutdiv" style="display:none">
            <iframe id="logout" height="0" width="0" src="<%= openAMHome%>/UI/Logout"></iframe>
        </div>
    </body>
</html>
