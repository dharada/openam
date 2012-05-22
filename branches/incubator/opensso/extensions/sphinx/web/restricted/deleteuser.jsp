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
<jsp:useBean id="notifier" scope="page" class="com.janua.identity.beans.NotifyNewUser" />
<%!    String technicalID = null;
    boolean userDeleted = false;
%>
<%
    technicalID = request.getHeader("SPHYNX_TECH_ID");
    if (technicalID == null) {
        out.print("Internal error, OpenAM Agent configuration problem on the reverse-proxy");
    }

    if (request.getParameter("post_full_delete") != null) {
        // Suppression totale de l'utilisateur
        String removedEmail = userProperties.getProperty(technicalID, "mail");
        String identifiant = userProperties.getProperty(technicalID, "uid");
        if (userProvisionner.deleteUser(technicalID)) {
            notifier.sendUserDeletedLink(removedEmail, identifiant);
            userDeleted = true;
        }
    } else {
        if (request.getParameter("post_remove_role") != null) {
            // Suppression d'un ou plusieurs role dans l'annuaire

            String applications[] = request.getParameterValues("app");
            userProperties.clearAllApps(technicalID);

            if (applications != null) {
                for (int i = 0; i < applications.length; i++) {
                    userProperties.addApplication(technicalID, applications[i]);
                }
            } else {
                applications = new String[]{"No application found"};
            }
            notifier.sendNewApplicationList(technicalID, applications);
            response.sendRedirect("index.jsp");
        } else {
            userDeleted = false;
        }
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Suppression des acc&egrave;s &agrave; vos applications</title>
        <title></title>

        <link rel="stylesheet" href="css/styles.css" type="text/css">

        <link href="css/dropdown/dropdown.css" media="screen" rel="stylesheet" type="text/css" />

        <link href="css/dropdown/default.css" media="screen" rel="stylesheet" type="text/css" />
        <link href="css/dropdown/default.advanced.css" media="screen" rel="stylesheet" type="text/css" />
        <link rel="stylesheet" type="text/css" href="css/dropdown/style.css" />

        <script language="JavaScript" src="js/browserVersion.js"></script>
        <script language="JavaScript" src="js/auth.js"></script>
        <script language="JavaScript">
            function validate_and_delete() {
                var answer = confirm("Are you sure you want to delete your account ?");
                if (answer==true) {
                    document.forms["deprovisionform"].submit();
                }
            }
        </script>
    </head>
    <body>
        <% if (!userDeleted) {%>
        <form name="deprovisionform" method="GET">
            <input type="hidden" name="post_full_delete" value="1"/>
            <table border="0">
                <thead>
                    <tr>
                        <th colspan="2">Complete removal of the account <%= userProperties.getProperty(technicalID, "uid")%></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td colspan="2">Are you sure you want to delete your account ? ?</td>
                    </tr>
                    <tr>
                        <td colspan="2"><input type="button" onclick="validate_and_delete();" value="Complete removal of the account" name="okbutton" /></td>
                    </tr>
                </tbody>
            </table>
        </form>
        <form name="suppressroleform" method="GET">
            <input type="hidden" name="post_remove_role" value="1"/>
            <table border="0">
                <thead>
                    <tr>
                        <th colspan="2">My applications</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        int cpt = 0;
                        for (String s : userProperties.getAllApplicationsCN()) {
                            String active = null;
                            if (userProperties.hasApplication(technicalID, s)) {
                                active = "checked=\"checked\"";
                            } else {
                                continue;
                            }
                    %>
                    <tr>
                        <td><a href="Application : <% out.print(userProperties.getApplicationName(s));%>">
                                <% out.print(userProperties.getApplicationURL(s));%>
                            </a>
                        </td>
                        <td><input type="checkbox" name="app" value="<%= s%>" <%= active%>/></td>
                    </tr>
                    <%    cpt++;
                        }
                    %>
                    <tr>
                        <td colspan="2">
                            <input type="submit" value="Commit the changes" name="modify_role_okbutton" />
                        </td>
                    </tr>
                </tbody>
            </table>
        </form>
        <table border="0">
            <thead>
                <tr>
                    <th>Applications not subscribed</th>
                </tr>
            </thead>
            <tbody>
                <%
                    cpt = 0;
                    for (String s : userProperties.getAllApplicationsCN()) {
                        if (userProperties.hasApplication(technicalID, s)) {
                            continue;
                        }
                %>
                <tr>
                    <td><a href="Application : <% out.print(userProperties.getApplicationName(s));%>">
                            <% out.print(userProperties.getApplicationURL(s));%>
                        </a></td>
                </tr>
                <%    cpt++;
                    }
                %>
            </tbody>
        </table>

        <form method="POST" name="cancel_form" action="index.jsp">
            <tr>
                <td colspan="2"><input type="submit" value="Cancel" name="cancel"/></td>
            </tr>
        </form>



        <% } else {%>
        <div id="logoutdiv" style="display:none">
            <iframe id="logout" height="0" width="0" src="/opensso/UI/Logout"></iframe>
        </div>
        <table border="0">
            <thead>
                <tr>
                    <th colspan="2">The user has been deleted.</th>
                </tr>
            <form method="POST" name="cancel_form" action="index.jsp">
                <tr>
                    <td colspan="2"><input type="submit" value="Home screen" name="cancel"/></td>
                </tr>
            </form>
        </thead>
        <tbody>
        </tbody>
    </table>
    <% }%>
</body>
</html>
