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
<jsp:useBean id="notifyNewUser" scope="page" class="com.janua.identity.beans.NotifyNewUser" />
<jsp:useBean id="urlTester" scope="request" class="com.janua.identity.beans.CheckConnectivity" />
<%! String email = "";
    String identifiant = "";
    boolean captchaok = true;
    boolean changeok = true;
    boolean mailsent = false;
    boolean identifiantOk = true;
    boolean emailOk = true;
    boolean netIsUp = false;
    String mycaptcha = "";
    String captcha_response = "";
    String captchaHTML = "<td><img src=\"captcha.jsp\"></td><td><input type=\"text\" name=\"mycaptcha\"/></td>";
    String InputFailed = "class =\".inputfailed\"";
%>
<%
    netIsUp = urlTester.checkURL();
//    netIsUp = false; // for test purposes
    if (netIsUp) {
        net.tanesha.recaptcha.ReCaptcha c = net.tanesha.recaptcha.ReCaptchaFactory.newReCaptcha("6Ld4KMkSAAAAAI7SCr3uQgQdxmM7s2eEiuWHhlVE", "6Ld4KMkSAAAAAHgBKQt07beT0ZMJruwl5y8oL2MW", false);

        captchaHTML = "<td colspan=\"2\" align=\"center\">" + c.createRecaptchaHtml(null, null) + "</td>";
%>
<%@ page import="net.tanesha.recaptcha.*" %>
<%    }%>
<%

    if (request.getParameter("post") != null) {
        mycaptcha = request.getParameter("mycaptcha");
        identifiant = request.getParameter("identifiant");
        email = request.getParameter("email");
        String technicalID = "";

        String unlockToken = java.util.UUID.randomUUID().toString();

        /*
         *
         * Captcha
         *
         */

        if (netIsUp) { /* Actions in case of full featured remote captcha */
            String remoteAddr = request.getRemoteAddr();
            ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
            reCaptcha.setPrivateKey("6Ld4KMkSAAAAAHgBKQt07beT0ZMJruwl5y8oL2MW");

            String challenge = request.getParameter("recaptcha_challenge_field");
            String uresponse = request.getParameter("recaptcha_response_field");
            ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, challenge, uresponse);

            if (reCaptchaResponse.isValid()) {
                captchaok = true;
            } else {
                captchaok = false;
            }

        } else {
            captcha_response = (String) request.getSession().getAttribute("captcha");
            if (captcha_response != null) {
                captchaok = captcha_response.equals(mycaptcha);
            } else {
                captchaok = false;
            }
        }

        if (identifiant != null && captchaok) { // On envoie via l'identifiant
            if (!identifiant.equals("")) {
                technicalID = userProperties.getTechnicalIdByUid(identifiant);
                if (technicalID != null) {
                    userProperties.setProperty(technicalID, "description", unlockToken);
                    userProperties.clearProperty(technicalID, "inetUserStatus");
                    mailsent = notifyNewUser.sendResetPasswordLink(technicalID, unlockToken);
                } else {
                    identifiantOk = false;
                }
            } else {
                if (email != null && captchaok) { // On envoie via le mail
                    if (!email.equals("")) {
                        technicalID = userProperties.getTechnicalIdByMail(email);
                        if (technicalID == null) {
                            emailOk = false;
                        } else {
                            userProperties.setProperty(technicalID, "description", unlockToken);
                            userProperties.clearProperty(technicalID, "inetUserStatus");
                            mailsent = notifyNewUser.sendResetPasswordLink(technicalID, unlockToken);
                        }
                    } else {
                        changeok = false;
                    }
                }
            }
        }


    } else {
        email = "";
        identifiant = "";
        changeok = false;
        identifiantOk = true;
        emailOk = true;
        mailsent = false;
        captcha_response = "";
        mycaptcha = "";
        captchaok = true;
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Password lost</title>

        <link rel="stylesheet" href="css/styles.css" type="text/css">

        <link href="css/dropdown/dropdown.css" media="screen" rel="stylesheet" type="text/css" />

        <link href="css/dropdown/default.css" media="screen" rel="stylesheet" type="text/css" />
        <link href="css/dropdown/default.advanced.css" media="screen" rel="stylesheet" type="text/css" />
        <link rel="stylesheet" type="text/css" href="css/dropdown/style.css" />

        <script language="JavaScript" src="js/browserVersion.js"></script>
        <script language="JavaScript" src="js/auth.js"></script>
    </head>
    <%
        if (!mailsent) {
    %>
    <body>
    <center>
        <h1>Password lost</h1>

        <form name="resetpassword" action="?post" method="POST">
            <table border="0">
                <tbody>
                    <tr>
                        <% out.print(captchaHTML);%>
                    </tr>
                    <tr>
                        <td>Enter your username</td>
                        <td><input type="text" name="identifiant" value="<%= identifiant%>" /></td>
                    </tr>
                    <tr>
                        <td>
                            Or your email address
                        </td>
                        <td>
                            <input type="text" name="email" value="<%= email%>" />
                        </td>
                    </tr>

                <td colspan="2" align="center">
                    <input type="submit" value="Submit" />
                </td>
                </tr>
                </tbody>
                <tr><td colspan="2">
                        <%
                            if (!identifiantOk) {
                                out.print("<tr><td colspan=\"2\" align=\"center\">Username does not exist</td></tr>");
                            }
                        %>
                        <%
                            if (!identifiantOk) {
                                out.print("<tr><td colspan=\"2\" align=\"center\">Email is not used</td></tr>");
                            }
                        %>
                        <%
                            if (!captchaok) {
                                out.print("<tr><td colspan=\"2\" align=\"center\">String does not match the captcha</td></tr>");
                            }
                        %>
                    </td></tr>
                <tr>
                    <td colspan="2" align="center">
                        <input type="button" value="Cancel" name="cancel" onclick="window.location='../restricted/index.jsp'"/>
                    </td>
                </tr>

            </table>
        </form>
    </center>
</body>
<%
} else {
%>
<body>
    <table border="0">
        <thead>
            <tr>
                <th>An email about your password modification has been sent to you</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td colspan="2" align="center">
                    <form method="POST" name="cancel_form" action="../restricted/index.jsp">
                    <input type="submit" value="Cancel" name="cancel"/></td>
                </form>
            </tr>
        </tbody>
    </table>
</body>
<%    }
%>
</html>
