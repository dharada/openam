<%--
    Document   : register2
    Created on : 14 oct. 2011, 02:08:27
    Author     : faime
--%>
<%@page import="com.janua.identity.beans.AppConfig"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.util.Properties"%>
<%! String sphinxHome = "/openam"; // default value%>
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
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
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Thank you for your registration</title>
    </head>
    <body>
        <h1>Registration successful</h1>
        <p>An activation link has been sent to your email</p>
            <table border="0">
                <thead>
                    <tr>
                        <th>Summary of your information :</th>
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
                    <tr>
                        <td>Phone number</td>
                        <td><% out.print(userProperties.getProperty(request.getParameter("technicalID"), "telephonenumber"));%></td>
                    </tr>
                    <tr>
                        <td colspan="2" align="center"><input type="button" value="Login page" onclick="javascript:window.location='<%= sphinxHome %>'"/></td>
                    </tr>
                </tbody>
            </table>
    </body>
</html>
