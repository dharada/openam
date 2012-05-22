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
<jsp:useBean id="userProvisionner" scope="page" class="com.janua.identity.beans.ProvisionUser" />
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
<%!    boolean userAlreadyActive = false;
%>
<%
    String technicalID = request.getParameter("technicalID");
    String token = ""; //request.getParameter("token");

    String userStatus = userProperties.getProperty(technicalID, "inetUserStatus");
    if (!userStatus.equals("inactive")) {
        userAlreadyActive = true;
    }

    userAlreadyActive = false;

    if (!userAlreadyActive) {
        userProvisionner.activateUser(technicalID, token);
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Welcome to Sphinx</title>
    </head>
    <%
        if (!userAlreadyActive) {
    %>
    <body>
        <h1>Your account is now enabled...</h1>
        <table border="0">
            <thead>
                <tr>
                    <th>Summary of your information</th>
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
            </tbody>
        </table>
        <form name="dummyform" action="../restricted/index.jsp?SPHYNX_TECH_ID=<%= technicalID%>" align="center">
            <table>
                <tr>
                    <td>
                        <input type="submit" value="Continue" name="dummybutton" />
                    </td>
                </tr>
            </table>
        </form>
    </body>
    <% } else {%>
    <body>
        <h1>The link you are looking for is no more available.</h1>
    </body>
    <%
            userAlreadyActive = false;
        }
    %>
</html>

