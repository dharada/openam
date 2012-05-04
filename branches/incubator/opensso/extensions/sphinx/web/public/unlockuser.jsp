<%--
    Document   : unlockuser.jsp
    Created on : 24 nov. 2011, 13:07:37
    Author     : faime
--%>
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
<%
    String technicalID = request.getParameter("technicalID");
    String givenToken = request.getParameter("token");
    String token = userProperties.getProperty(technicalID, "description");

    if (token != null && givenToken != null) {
        if (token.equals(givenToken)) {
            userProperties.setProperty(technicalID, "userPassword", userProperties.getProperty(technicalID, "userPassword"));
//          userProperties.unlockAccount(technicalID);
//          userProperties.clearProperty(technicalID, "pwdaccountlockedtime");
            userProperties.clearProperty(technicalID, "description");

            response.sendRedirect("../restricted/index.jsp");
        }
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Error, link not found</title>
    </head>
    <body>
        <h1>The link you are looking for is no more available.</h1>
    </body>
</html>
