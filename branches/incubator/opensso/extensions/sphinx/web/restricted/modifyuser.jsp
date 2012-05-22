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
<jsp:useBean id="urlTester" scope="page" class="com.janua.identity.beans.CheckConnectivity" />
<jsp:useBean id="emailTester" scope="page" class="com.janua.identity.beans.EmailValidator" />
<jsp:useBean id="syntaxChecker" scope="page" class="com.janua.identity.beans.SyntaxChecker" />
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
<jsp:useBean id="userNotifier" scope="page" class="com.janua.identity.beans.NotifyNewUser" />
<%!    String email = "";
    String email2 = "";
    boolean emailsyntaxok = true;
    boolean emailok = true;
    boolean emailAlreadyExists = false;
    String passwordAttrib = "";
    boolean netIsUp = false;
    String InputFailed = "class=\".inputfailed\"";
    String telephone = "";
    String technicalID = null;
    String identifiant = "";
    boolean identifiansyntaxok = true;
    boolean identifiantok = false;
    boolean telephoneSyntaxOk = true;
    String sphinxUrl = "";
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

        sphinxUrl = configFile.getProperty("sphynx_url");
        fs.close();
    } catch (IOException ex) {
    }

    identifiant = userProperties.getProperty(technicalID, "uid");
    String oldIdentifiant = identifiant;

    email = userProperties.getProperty(technicalID, "mail");
    String oldEmail = email;

    telephone = userProperties.getProperty(technicalID, "telephonenumber");
    String oldTelephone = telephone;

    boolean emailChanged = false;
    boolean identifiantChanged = false;
    boolean telephoneChanged = false;
    netIsUp = urlTester.checkURL();

    if (request.getParameter("post") != null) {
        identifiant = request.getParameter("identifiant");

        if (!identifiant.equals(oldIdentifiant)) {
            identifiantChanged = true;
        }

        if (identifiant != null) {
            identifiansyntaxok = syntaxChecker.checkIdentifier(identifiant);
            identifiantok=userProvisionner.checkUserID(identifiant);
        } else {
            identifiansyntaxok = false;
            identifiantok=true;
        }

        telephone = request.getParameter("telephone");
        if (!telephone.equals(oldTelephone)) {
            telephoneChanged = true;
        }

        telephoneSyntaxOk = syntaxChecker.checkTelephone(telephone);
        email = request.getParameter("email");

        if (!email.equals(oldEmail)) {
            emailChanged = true;
        }

        email2 = request.getParameter("email2");

        oldEmail = userProperties.getProperty(technicalID, "mail");


        if (email != null && email2 != null) {
            emailok = email.equals(email2);
            if (!email.equals(oldEmail)) {
                emailAlreadyExists = emailTester.isEmailPresent(email);
                emailChanged = true;
            }
            emailsyntaxok = emailTester.isValidEmailAddress(email);
        }

        if (emailok && emailsyntaxok && identifiansyntaxok && !emailAlreadyExists && telephoneSyntaxOk && identifiantok) {
            out.println("PROVISIONNING USER");
            if (emailChanged || telephoneChanged || identifiantChanged) {
                if (userProvisionner.updateUser(identifiant, technicalID, email, telephone)) {

                    userNotifier.sendUserUpdatedLink(email, identifiant, telephone);
                    if (identifiantChanged) {
                        response.sendRedirect(sphinxUrl + "/public/returntologin.jsp");
                    } else {
                        response.sendRedirect(sphinxUrl + "/restricted/index.jsp");
                    }
                } else {
                    out.print("PROVISIONNING FAILED");
                }
            } else {
                response.sendRedirect(sphinxUrl + "/restricted/index.jsp");
            }
        }
    } else {
        //email = "";
        email2 = "";
        emailsyntaxok = true;
        emailok = true;
        emailAlreadyExists = false;
        passwordAttrib = "";
        netIsUp = false;
        InputFailed = "class=\".inputfailed\"";
        //telephone = "";
        technicalID = null;
        //identifiant = "";
        identifiansyntaxok = true;
        sphinxUrl = "";
        telephoneSyntaxOk = true;
    }
%><%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Edit my informations</title>

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
        <h1>Edit my informations</h1>

        <table border="0">
            <form name="register" method="GET">
                <input type="hidden" name="post" value="1"/>
                <tbody>
                    <tr>
                        <td>Change my username</td>
                        <td <% if (!identifiansyntaxok || !identifiantok) {
                                out.print(InputFailed);
                            }%>><input type="text" name="identifiant" value="<%= identifiant%>" /></td>
                    </tr>
                    <tr>
                        <td>Change my email</td>
                        <td <% if (!emailok || !emailsyntaxok) {
                                out.print(InputFailed);
                            }%>><input type="text" name="email" value="<%= email%>" /></td>
                    </tr>
                    <tr>
                        <td>Confirm my new email</td>
                        <td <% if (!emailok || !emailsyntaxok) {
                                out.print(InputFailed);
                            }%>><input type="text" name="email2" value="<%= email%>" />
                        </td>
                    </tr>
                    <tr>
                        <td>Change my phone number</td>
                        <td <%if (!telephoneSyntaxOk) {
                                out.print(InputFailed);
                            }%>><input type="text" name="telephone" value="<%= telephone%>"></td>
                    </tr>
                    <%
                        if (!identifiantok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Username already exists</td></tr>");
                        }
    
                        if (!identifiansyntaxok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Invalid username : 4 to 30 lowercase characters (no special ones except '.')</td></tr>");
                        }
    
                        if (!emailok && !emailAlreadyExists) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Emails don't match</td></tr>");
                        }

                        if (emailAlreadyExists) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Email already used</td></tr>");
                        }


                        if (!emailsyntaxok) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Email does not match <i>user@example.com</i></td></tr>");
                        }

                        if (!telephoneSyntaxOk) {
                            out.print("<tr><td colspan=\"2\" align=\"center\">Invalid phone number</td></tr>");
                        }
                    %>
                    <tr>
                        <td colspan="2" align="center">
                            <input type="submit" value="Change my informations" name="submit" />
                        </td>
                    </tr>
                </tbody>
            </form>
            <form method="POST" name="cancel_form" action="index.jsp">
                <tr>
                    <td colspan="2" align="center"><input type="submit" value="Cancel" name="cancel"/></td>
                </tr>
            </form>
        </table>
    </center>
</body>
</html>