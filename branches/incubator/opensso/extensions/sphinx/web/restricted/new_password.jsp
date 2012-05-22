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
<jsp:useBean id="userNotifier" scope="page" class="com.janua.identity.beans.NotifyNewUser" />
<%!    String email = "";
    String email2 = "";
    String mycaptcha = "";
    String captcha = "";
    String captcha_response = "";
    boolean emailsyntaxok = true;
    boolean emailok = true;
    boolean captchaok = true;
    String captchaAttrib = "";
    String passwordAttrib = "";
    boolean netIsUp = false;
    String InputFailed = "bgcolor=\"#ff0000\"";
    String telephone = "";
    String technicalID = null;
%>
<%
    technicalID = request.getHeader("SPHYNX_TECH_ID");
    if (technicalID == null) {
        out.print("Internal error, OpenAM Agent configuration problem on the reverse-proxy");
    }

    userNotifier.sendResetPasswordLink(techincalID);
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Sphinx: Change password</title>
    </head>
    <body>
        <p>Your <a href="#sphynx_url">Sphinx</a>password has been reset</p>
        <p>An email has been sent to you.</p>
    </body>
</html>