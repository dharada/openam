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
<jsp:useBean id="userProvisionner" scope="page" class="com.janua.identity.beans.ProvisionUser" />
<jsp:useBean id="userNotifier" scope="page" class="com.janua.identity.beans.NotifyNewUser" />
<jsp:useBean id="urlTester" scope="page" class="com.janua.identity.beans.CheckConnectivity" />
<jsp:useBean id="emailTester" scope="page" class="com.janua.identity.beans.EmailValidator" />
<jsp:useBean id="syntaxChecker" scope="page" class="com.janua.identity.beans.SyntaxChecker" />
<%!    String identifiant = "";
    String password = "";
    String password2 = "";
    String email = "";
    String email2 = "";
    String mycaptcha = "";
    String captcha = "";
    String captcha_response = "";
    boolean passwordsyntaxok = true;
    boolean emailsyntaxok = true;
    boolean emailok = true;
    boolean captchaok = true;
    boolean passwordok = true;
    boolean identifiantok = true;
    boolean identifiansyntaxok = true;
    boolean conditionsok = true;
    boolean telephoneNumberok = true;
    boolean telephoneNumbersyntaxok = true;
    String captchaAttrib = "";
    String passwordAttrib = "";
    boolean netIsUp = false;
    String captchaHTML = "<td><img src=\"captcha.jsp\"></td><td><input type=\"text\" name=\"mycaptcha\"/></td>";
    String InputFailed = "class =\".inputfailed\"";
    String telephoneNumber = "";
    String sphinxHome = "/restricted/index.jsp";
    String condString = "";
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
    Properties configFile = null;
    try {
        configFile = new Properties();
        FileInputStream fs = new FileInputStream(System.getProperty("catalina.home") + "/conf/sphinx.properties");
        configFile.load(fs);

        sphinxHome = configFile.getProperty("sphynx_url") + "/restricted/index.jsp";
        fs.close();
    } catch (IOException ex) {
    }

    if (request.getParameter("post") != null) {
        telephoneNumber = request.getParameter("telephone");
        telephoneNumbersyntaxok = syntaxChecker.checkTelephone(telephoneNumber);
        identifiant = request.getParameter("identifiant");
        if (identifiant != null) {
            identifiansyntaxok = syntaxChecker.checkIdentifier(identifiant);
        } else {
            identifiansyntaxok = false;
        }
        password = request.getParameter("password");
        password2 = request.getParameter("password2");
        email = request.getParameter("email");
        email2 = request.getParameter("email2");
        mycaptcha = request.getParameter("mycaptcha");
        if (request.getParameter("accept_conditions") != null) {
            conditionsok = true;
            condString = "checked=\"yes\"";
        } else {
            conditionsok = false;
            condString = "";
        }

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

        if (syntaxChecker.checkPassword(password)) {
            passwordsyntaxok = true;
        } else {
            passwordsyntaxok = false;
        }

        if (password.equals(password2)) {
            passwordok = true;
        } else {
            passwordok = false;
            passwordAttrib = "bgcolor=\"#ff0000\"";
        }

        if (userProvisionner.checkUserID(identifiant)) {
            //out.println("IDENTIFIANT OK");
            identifiantok = true;
        } else {
            identifiantok = false;
            //out.println("IDENTIFIANT KO");
        }

        if (email != null && email2 != null) {
            emailok = email.equals(email2);
            emailsyntaxok = emailTester.isValidEmailAddress(email);
            if (emailTester.isEmailPresent(email)) {
                emailsyntaxok = false;
            }
        }

        if (passwordsyntaxok && passwordok && captchaok && identifiantok && identifiansyntaxok && emailok && emailsyntaxok && conditionsok && telephoneNumbersyntaxok) {
            //out.println("PROVISIONNING USER");
            String technicalID = userProvisionner.CreateUser(identifiant, password, email, telephoneNumber);
            if (technicalID != null) {
                userNotifier.sendActivationLink(userProvisionner.technicalID, email);
                response.sendRedirect("register2.jsp?technicalID=" + technicalID + "&uid=" + identifiant + "&email=" + email);
            }
        }
    } else {
        identifiant = "";
        password = "";
        password2 = "";
        email = "";
        email2 = "";
        mycaptcha = "";
        captcha = "";
        captcha_response = "";
        passwordsyntaxok = true;
        emailsyntaxok = true;
        emailok = true;
        captchaok = true;
        passwordok = true;
        identifiantok = true;
        identifiansyntaxok = true;
        conditionsok = true;
        telephoneNumberok = true;
        telephoneNumbersyntaxok = true;
        captchaAttrib = "";
        passwordAttrib = "";
        telephoneNumber = "";
        condString = "";
    }
%><%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Registration</title>

        <link rel="stylesheet" href="css/styles.css" type="text/css">

        <link href="css/dropdown/dropdown.css" media="screen" rel="stylesheet" type="text/css" />

        <link href="css/dropdown/default.css" media="screen" rel="stylesheet" type="text/css" />
        <link href="css/dropdown/default.advanced.css" media="screen" rel="stylesheet" type="text/css" />
        <link rel="stylesheet" type="text/css" href="css/dropdown/style.css" />

        <script language="JavaScript" src="js/browserVersion.js"></script>
        <script language="JavaScript" src="js/auth.js"></script>
    </head>
    <body>
    <center>
        <h1>Registration</h1>
        <form name="register" action="?post" method="POST">
            <table border="0">
                <tbody>
                    <tr>
                        <td>Enter your username</td>
                        <td <% if (!identifiantok || !identifiansyntaxok) {
                                out.print(InputFailed);
                            }%>><input type="text" name="identifiant" value="<%= identifiant%>" /></td>
                    </tr>
                    <tr>
                        <td>
                            Enter your phone number
                        </td>
                        <td
                            <% if (!telephoneNumberok || !telephoneNumbersyntaxok) {
                                    out.print(InputFailed);
                                }
                            %>><input type="text" name="telephone" value="<%= telephoneNumber%>"/></td>
                    </tr>
                    <tr>
                        <td>Enter your password</td>
                        <td <% if (!passwordok || !passwordsyntaxok) {
                                out.print(InputFailed);
                            }%>><input  type="password" name="password" value="" autocomplete="off"/></td>
                    </tr>
                    <tr>
                        <td>Confirm your password</td>
                        <td <% if (!passwordok || !passwordsyntaxok) {
                                out.print(InputFailed);
                            }
                            %>>
                            <input type="password" name="password2" value="" autocomplete="off">
                        </td>
                    </tr>
                    <tr>
                        <td>Enter your email</td>
                        <td <% if (!emailok || !emailsyntaxok) {
                                out.print(InputFailed);
                            }%>><input type="text" name="email" value="<%= email%>" /></td>
                    </tr>
                    <tr>
                        <td>Confirm your email</td>
                        <td <% if (!emailok || !emailsyntaxok) {
                                out.print(InputFailed);
                            }
                            %>><input type="text" name="email2" value="<%= email2%>" />
                        </td>
                    </tr>
                    <tr>
                        <% out.print(captchaHTML);%>
                    </tr>
                    <tr>
                        <td>
                            <a href="conditions.html" target="_">Terms of Use Agreements</a>
                        </td>
                        <td align="center" <% if (!conditionsok) {
                                out.print(InputFailed);
                            }%>>
                            <input type="checkbox" name="accept_conditions" value="I agree to Terms of Use" <%= condString%>>
                        </td>
                    </tr>
                    <%
                        if (!identifiantok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Username already exists</td></tr>");
                        }
                        if (!identifiansyntaxok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Invalid username : 4 to 30 lowercase characters (no special ones except '.')</td></tr>");
                        }
                        if (!passwordok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Password don't match</td></tr>");
                        }
                        if (!passwordsyntaxok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Invalid password</td></tr>");
                            out.print("<tr><td colspan=\"2\" align=\"center\">Password must contain 8 to 20 characters with at least a letter and a digit</td></tr>");
                        }
                        if (!emailok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Emails don't match</td></tr>");
                        }
                        if (!emailsyntaxok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Email does not match <i>user@example.com</i> or is already used</td></tr>");
                        }
                        if (!captchaok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">String does not match the captcha</td></tr>");
                            //out.print("<tr><td colspan=\"2\" align=\"center\">Attendu: " + captcha_response + " obtenu: " + mycaptcha + "</td></tr>");
                        }
                        if (!conditionsok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">You have to agree to the terms of use</td></tr>");
                        }
                        if (!telephoneNumbersyntaxok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Invalid phone number</td></tr>");
                        }

                    %>
                    <tr>
                        <td align="center">
                            <input type="submit" value="Submit" name="submit" />
                        </td>
                        <td align="center">
                            <input type="button" value="Cancel" name="cancel" onclick="javascript:window.location='<%= sphinxHome%>'"/>
                        </td>
                    </tr>
                </tbody>
            </table>
        </form>
    </center>
</body>
</html>