<%@page import="java.io.IOException"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.util.Properties"%>
<%!    String openAMHome = "";
    String sphinxHome = "";
%>
<%
    Properties configFile = null;
    try {
configFile = new Properties();
        FileInputStream fs = new FileInputStream(System.getProperty("catalina.home") + "/conf/sphinx.properties");
        configFile.load(fs);

        sphinxHome = configFile.getProperty("sphynx_url");
        openAMHome = configFile.getProperty("openam_url");

        fs.close();
    } catch (IOException ex) {
    }
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <script type="text/javascript">

            function redirect()
            {
                window.location = '<%= sphinxHome%>/restricted/index.jsp';
            }
        </script>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>You are being redirected</title>
    </head>
    <body onload="timer=setTimeout('redirect()', 2000)">
        <div id="logoutdiv" style="display:none">
            <iframe id="logout" height="0" width="0" src="<%= openAMHome%>/UI/Logout"></iframe>
        </div>
    </body>
</html>
