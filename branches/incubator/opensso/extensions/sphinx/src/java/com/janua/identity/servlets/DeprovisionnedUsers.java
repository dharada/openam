/**
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
package com.janua.identity.servlets;

import com.janua.identity.beans.AppConfig;
import com.janua.identity.beans.UserProperties;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author faime
 */
public class DeprovisionnedUsers extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private String ldapServer;
    private int ldapPort;
    private String ldapUser;
    private String ldapPassword;
    private String ldapBase;
    private String ldapUserBase;
    private String applicationUrlAttribute;
    private int ldapTimeout;

    private LDAPConnection getConnection() throws LDAPException {

        ldapServer = AppConfig.getProperty("ldap_server");
        ldapPort = Integer.parseInt(AppConfig.getProperty("ldap_port"));
        ldapUser = AppConfig.getProperty("ldap_user");
        ldapPassword = AppConfig.getProperty("ldap_password");
        ldapBase = AppConfig.getProperty("ldap_base");
        ldapUserBase = AppConfig.getProperty("user_base") + "," + ldapBase;
        applicationUrlAttribute = AppConfig.getProperty("application_url_attribute");
        String sTimeout = AppConfig.getProperty("ldap_timeout");
        if (sTimeout != null) {
            ldapTimeout = Integer.parseInt(AppConfig.getProperty("ldap_timeout"));
        } else {
            ldapTimeout = 500;
        }

        LDAPConnection conn = new LDAPConnection();

        conn.connect(ldapServer, ldapPort);
        LOG.info("connected to directory");

        conn.bind(LDAPConnection.LDAP_V3, ldapUser, ldapPassword.getBytes());
        LOG.info("bound to directory");

        return conn;
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/xml");
        UserProperties props = new UserProperties();
        String role = request.getParameter("role");
        String timeStamp = request.getParameter("timeStamp");
        String DN = "cn=" + role + ",ou=groups,dc=gouv,dc=nc";

        List<String> removedCNs = new ArrayList<String>();
        LDAPConnection conn = null;
        try {
            conn = getConnection();
            String attrs[] = {"targetDN", "changes", "changeTime"};
            LOG.log(Level.INFO,"Recherche en cours de : " + "(targetDN=cn={0},ou=groups,dc=gouv,dc=nc)", role);
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setBatchSize(0);
            LDAPSearchResults res = conn.search("cn=Changelog", LDAPConnection.SCOPE_SUB, "(targetDN=cn=" + role + ",ou=groups,dc=gouv,dc=nc)", attrs, false, cons);

            while (res.hasMore() && res.getCount() > 0) {
                LDAPEntry e = res.next();
                LOG.log(Level.INFO, "Inspecting entry : {0}", e.getAttribute("targetDN").getStringValue());
                if (e.getAttribute("changeTime").getStringValue().compareTo(timeStamp) < 0) { // skip record if older than provided timestamp
                    LOG.info("Skipping entry for it's too old");
                    continue;
                }

                String changes = e.getAttribute("changes").getStringValue();
                LOG.log(Level.INFO, "Found changes : \n{0}", changes);

                if (changes.contains("delete: uniqueMember")) { // a deletion of uniqueMember has been detected
                    LOG.info("Detected delete");
                    String removedCN = changes.substring(changes.indexOf("uniqueMember: ") + 14, changes.indexOf(",ou="));
                    LOG.log(Level.INFO, "removedCN = {0}", removedCN);
                    removedCN = removedCN.substring(4);
                    LOG.log(Level.INFO, "removedCN = {0}", removedCN);
                    String techID = props.getTechnicalIdByUid(removedCN);
                    if (techID != null) {
                        removedCNs.add(techID);
                    }
                    LOG.log(Level.INFO, "WEBSERVICE found DN : {0}", removedCN);
                } else {
                    continue;
                }
            }
        } catch (LDAPException ex) {
            Logger.getLogger(DeprovisionnedUsers.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.disconnect();
            } catch (LDAPException ex) {
                Logger.getLogger(DeprovisionnedUsers.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        PrintWriter out = response.getWriter();
        try {
            out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            out.println("<removedUsers>");
            Iterator itr = removedCNs.iterator();
            while(itr.hasNext()) {
                out.println("  <DN>" + itr.next() + "</DN>");
            }
            out.println("</removedUsers>");
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
    private static final Logger LOG = Logger.getLogger(DeprovisionnedUsers.class.getName());
}
