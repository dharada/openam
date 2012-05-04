<%--
    Document   : index
    Created on : 24 oct. 2011, 21:45:37
    Author     : faime
--%>
<%@page import="java.util.logging.Logger"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.util.Properties"%>
<%!    String technicalID = null;
    String openAMHome = "";
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

        openAMHome = configFile.getProperty("openam_url");
        fs.close();
    } catch (IOException ex) {
    }
%>
<jsp:useBean id="userProperties" scope="page" class="com.janua.identity.beans.UserProperties" />
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Home screen</title>
        <link rel="stylesheet" href="css/styles.css" type="text/css">

        <link href="css/dropdown/dropdown.css" media="screen" rel="stylesheet" type="text/css" />

        <link href="css/dropdown/default.css" media="screen" rel="stylesheet" type="text/css" />
        <link href="css/dropdown/default.advanced.css" media="screen" rel="stylesheet" type="text/css" />
        <link rel="stylesheet" type="text/css" href="css/dropdown/style.css" />

        <script language="JavaScript" src="js/browserVersion.js"></script>
        <script language="JavaScript" src="js/auth.js"></script>
    </head>
    <body>
        <table border="0">
            <thead>
                <tr>
                    <th colspan="2">Your applications</th>
                </tr>
            </thead>
            <tbody>
                <%
                    int cpt = 0;
                    for (String s : userProperties.getApplicationsCN(technicalID)) {
                %>
                <tr>
                    <td><a href="<% out.print(userProperties.getApplicationURL(s));%>"><% out.print(userProperties.getApplicationName(s));%></a></td>
                </tr>
                <%    cpt++;
                    }
                %>
            </tbody>
        </table>

        <table border="0">
            <thead>
                <tr>
                    <td>
                        Sphinx functions
                    </td>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><a href="modifyuser.jsp">Edit my informations</a></td>
                </tr>
                <tr>
                    <td><a href="changepassword.jsp" target="_">Change my password</a></td>
                </tr>
                <tr>
                    <td><a href="deleteuser.jsp">Manage my subscriptions</a></td>
                </tr>
                <tr>
                    <td>
                        <a href="<%= openAMHome%>/UI/Logout">Logout from Sphinx</a>
                    </td>
                </tr>
            </tbody>
        </table>
    </body>
</html>
