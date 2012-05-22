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
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
<jsp:useBean id="syntaxChecker" scope="request" class="com.janua.identity.beans.SyntaxChecker" />
<%!    String technicalID = null;
    String oldPassword = "";
    String newPassword = "";
    String confirmNewPassword = "";
    boolean passwordok = true;
    boolean wrongPassword = false;
    boolean passwordSyntaxOk = true;
    boolean passwordsyntaxok = true;
    boolean passwordChanged = false;
    String InputFailed = "class =\".inputfailed\"";
%>
<%
    technicalID = request.getHeader("SPHYNX_TECH_ID");
    if (technicalID == null) {
        out.print("Internal error, OpenAM Agent configuration problem on the reverse-proxy");
    }

    if (request.getParameter("post") != null) {
        oldPassword = request.getParameter("oldpassword");
        newPassword = request.getParameter("newpassword");
        confirmNewPassword = request.getParameter("confirmnewpassword");

        if (newPassword.equals(confirmNewPassword)) {
            if (userProperties.checkPassword(technicalID, oldPassword)) {
                if (userProperties.changePassword(technicalID, oldPassword, newPassword)) {
                    passwordChanged = true;
                } else {
                    passwordSyntaxOk = false;
                }
            } else {
                wrongPassword = true;
            }
        } else {
            passwordok = false;
        }

        if (syntaxChecker.checkPassword(newPassword)) {
            passwordsyntaxok = true;
        } else {
            passwordsyntaxok = false;
        }
    } else {
        technicalID = "";
        oldPassword = "";
        newPassword = "";
        confirmNewPassword = "";
        passwordok = true;
        passwordsyntaxok = true;
        passwordChanged = false;
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Change password</title>

        <link rel="stylesheet" href="css/styles.css" type="text/css">

        <link href="css/dropdown/dropdown.css" media="screen" rel="stylesheet" type="text/css" />

        <link href="css/dropdown/default.css" media="screen" rel="stylesheet" type="text/css" />
        <link href="css/dropdown/default.advanced.css" media="screen" rel="stylesheet" type="text/css" />
        <link rel="stylesheet" type="text/css" href="css/dropdown/style.css" />

        <script language="JavaScript" src="js/browserVersion.js"></script>
        <script language="JavaScript" src="js/auth.js"></script>
        <title>Change password</title>
    </head>
    <% if (passwordChanged) {%>
    <body onload="window.close()">
    <% } else {%>
    <body>
    <% }%>
        <form name="changepasswordform" method="GET">
            <input type="hidden" name="post" value="1"/>
            <table border="0">
                <tbody>
                    <tr>
                        <td>Current password</td>
                        <td <% if (wrongPassword) {
                                out.print(InputFailed);
                            }%>><input type="password" name="oldpassword" value="" autocomplete="off" /></td>
                    </tr>
                    <tr>
                        <td>New password</td>
                        <td><input type="password" name="newpassword" value="" autocomplete="off"/></td>
                    </tr>
                    <tr>
                        <td>New password confirmation</td>
                        <td <% if (!passwordok) {
                                out.print(InputFailed);
                            }%>><input type="password" name="confirmnewpassword" value="" autocomplete="off"/></td>
                    </tr>
                    <tr>
                        <td colspan="2"><input type="submit" value="Submit" name="submit" /></td>
                    </tr>
                    <%
                        if (!passwordok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Passwords don't match</td></tr>");
                        }
                        if (!passwordsyntaxok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Invalid password</td></tr>");
                            out.print("<tr><td colspan=\"2\" align=\"center\">Password must contain 8 to 20 characters with at least a letter and a digit</td></tr>");
                        }
                    %>
                </tbody>
            </table>
            <input type="hidden" name="SPHYNX_TECH_ID" value="<%= technicalID%>" />
        </form>
        <form method="POST" name="cancel_form" action="index.jsp">
            <tr>
                <td colspan="2" align="center"><input type="button" value="Cancel" name="cancel" onclick="javascript:window.close()"/></td>
            </tr>
        </form>
    </body>
</html>
