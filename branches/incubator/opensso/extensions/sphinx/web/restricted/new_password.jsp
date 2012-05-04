<%--
    Document   : new_password
    Created on : 25 oct. 2011, 01:23:51
    Author     : faime
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