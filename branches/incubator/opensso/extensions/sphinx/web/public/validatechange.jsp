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
<%
    String technicalID = request.getParameter("technicalID");
    String givenToken = request.getParameter("token");
    String token = userProperties.getProperty(technicalID, "description");

    if (token != null && givenToken != null) {
        if (token.equals(givenToken)) {
            userProperties.clearProperty(technicalID, "description");
            userProperties.setProperty(technicalID, "inetUserStatus", "active");
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
