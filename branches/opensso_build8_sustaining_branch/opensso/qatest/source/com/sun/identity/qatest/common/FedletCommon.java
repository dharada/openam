/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FedletCommon.java,v 1.1 2009/03/20 17:29:28 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class contains helper methods for samlv2 fedlet tests
 */
public class FedletCommon extends TestCommon {
    
    /** Creates a new instance of FedletCommon */
    public FedletCommon() {
        super("FedletCommon");
    } 
    
    /**
     * This method creates the xml file    
     * 1. It goes to the idp side whether it is fedlet initated or 
     * idp initiated. 
     * 2. Enter the idpuser and password
     * 3. After successful idp login, "Single Sign-On successful" msg is
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param urlStr is the URL link of the fedlet initated or idp initated 
     * HTTP-POST or HTTP-Artifact     
     */
    public static void getxmlFedletSSO(String xmlFileName,            
            Map m, String urlStr)
            throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);           
        String fedletidp_user = (String)m.get(TestConstants.KEY_FEDLETIDP_USER);
        String fedletidp_userpw = (String)m.get(
                TestConstants.KEY_FEDLETIDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        out.write("<url href=\"" + urlStr);
   
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" +
                fedletidp_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + fedletidp_userpw + "\" />");
        out.write(newline);        
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }

    /**
     * This method loads the resource bundle & puts all the values in map
     * @param String rbName Resource bundle name
     * @param Map m will be populated with resource bundle data
     */
    public static void getEntriesFromResourceBundle(String rbName, Map map) {
        ResourceBundle rb = ResourceBundle.getBundle(rbName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            map.put(key, rb.getString(key));
        }
    }

    /**
     * This method grep Metadata from the htmlpage & returns as the string.
     * @param HtmlPage page which contains metadata
     */
    public static String getMetadataFromPage(HtmlPage page) {
        String metadata = "";
        String metaPage = page.getWebResponse().getContentAsString();
        if (!(metaPage.indexOf("EntityConfig") == -1)) {
            metadata = metaPage.substring(metaPage.
                    indexOf("EntityDescriptor") - 4,
                    metaPage.lastIndexOf("EntityDescriptor") + 17);
            metadata = metadata.replaceAll("&lt;", "<");
            metadata = metadata.replaceAll("&gt;", ">");
        }
        return metadata;
    }

    /**
     * This method grep ExtendedMetadata from the htmlpage & returns the string
     * @param HtmlPage page which contains extended metadata
     */
    public static String getExtMetadataFromPage(HtmlPage page) {
        String metadata = "";
        String metaPage = page.getWebResponse().getContentAsString();
        if (!(metaPage.indexOf("EntityConfig") == -1)) {
            metadata = metaPage.substring(metaPage.
                    indexOf("EntityConfig") - 4,
                    metaPage.lastIndexOf("EntityConfig") + 13);
            metadata = metadata.replaceAll("&lt;", "<");
            metadata = metadata.replaceAll("&gt;", ">");
        }
        return metadata;
    }
    
    /**
     * This method creates the hosted SP/IDP metadata template & loads it.
     * It returns the uploaded standard & extended metadata.
     * Null is returned in case of failure.
     * @param WebClient object after admin login is successful.
     * @param Map consisting of SP/IDP data
     * @param boolean signed metadata should contain signature true or false
     */
    public static String[] importMetadata(WebClient webClient, Map m,
            boolean signed, String role) {
        String[] arrMetadata= {"", ""};
        try {
            String deployurl = "";
            String entityName = "";
            String idpmetaAlias = "";
            String spmetaAlias = "";
            String spcertAlias = "";
            String spattrqprovider = "";
            String spscertalias = "";
            String spattqsceralias = "";
            String specertalias = "";
            String spattrqecertalias = "";
            String idpattrauthority = "";
            String idpauthnauthority = "";
            String idpscertalias = "";
            String idpttrascertalias = "";
            String idpauthnascertalias = "";
            String idpecertalias = "";
            String idpattraecertalias = "";
            String idpauthnaecertalias = "";
            String idpcertAlias = "";
            String executionRealm = "";
            String cot = "";
            
            if (role.equalsIgnoreCase("IDP")) {
                deployurl = m.get(TestConstants.KEY_AMC_PROTOCOL) + "://"
                        + m.get(TestConstants.KEY_AMC_HOST) + ":"
                        + m.get(TestConstants.KEY_AMC_PORT)
                        + m.get(TestConstants.KEY_AMC_URI);
                entityName = (String)m.get(
                        TestConstants.KEY_FEDLETIDP_ENTITY_NAME);
                idpmetaAlias = (String)m.get(
                        TestConstants.KEY_IDP_METAALIAS);
                idpcertAlias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                executionRealm = (String)m.get(
                        TestConstants.KEY_ATT_EXECUTION_REALM);
                idpattrauthority = (String)m.get(
                        TestConstants.KEY_IDP_ATTRAUTHOIRTY);
                idpauthnauthority = (String)m.get(
                        TestConstants.KEY_IDP_AUTHNAUTHORITY);
                idpscertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpttrascertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpauthnascertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpecertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpattraecertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpauthnaecertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                cot = (String)m.get(TestConstants.KEY_FEDLET_COT);
                if (idpattrauthority == null ||
                        idpattrauthority.equals("")) {
                    idpattrauthority = "/attra";
                }
                if (idpauthnauthority == null ||
                        idpauthnauthority.equals(""))  {
                    idpauthnauthority = "/authna";
                }
                if (idpscertalias == null ||
                        idpscertalias.equals("")) {
                    idpscertalias = idpcertAlias;
                }
                if (idpttrascertalias == null ||
                        idpttrascertalias.equals("")) {
                    idpttrascertalias = idpcertAlias;
                }
                if (idpauthnascertalias == null ||
                        idpauthnascertalias.equals("")) {
                    idpauthnascertalias = idpcertAlias;
                }
                if (idpecertalias == null ||
                        idpecertalias.equals("")) {
                    idpecertalias = idpcertAlias;
                }
                if (idpattraecertalias == null ||
                        idpattraecertalias.equals("")) {
                    idpattraecertalias = idpcertAlias;
                }
                if (idpauthnaecertalias == null ||
                        idpauthnaecertalias.equals("")) {
                    idpauthnaecertalias = idpcertAlias;
                }
            } 
            //get sp & idp extended metadata
            FederationManager fm = new FederationManager(deployurl);
            HtmlPage metaPage;
            if (signed) {
                metaPage = fm.createMetadataTempl(webClient, entityName, true,
                        true, spmetaAlias, idpmetaAlias,
                        spattrqprovider, idpattrauthority, idpauthnauthority,
                        null, null, null, null, spcertAlias, idpcertAlias,
                        spattqsceralias, idpscertalias, idpttrascertalias,
                        null, null, null, spcertAlias, idpcertAlias,
                        spattrqecertalias, idpattraecertalias,
                        idpauthnaecertalias, null, null, null, "saml2");
            } else {
                metaPage = fm.createMetadataTempl(webClient, entityName, true,
                        true, spmetaAlias, idpmetaAlias,
                        spattrqprovider, idpattrauthority, idpauthnauthority, 
                        null,  null, null, null, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "saml2");
            }
            if (FederationManager.getExitCode(metaPage) != 0) {
                assert false;
            }
            
            String page = metaPage.getWebResponse().getContentAsString();
            if (page.indexOf("EntityDescriptor") != -1) {
                arrMetadata[0] = page.substring(
                        page.indexOf("EntityDescriptor") - 4,
                        page.lastIndexOf("EntityDescriptor") + 17);
                arrMetadata[1] = page.substring(
                        page.indexOf("EntityConfig") - 4,
                        page.lastIndexOf("EntityConfig") + 13);
            } else {
                arrMetadata[0] = null;
                arrMetadata[1] = null;
                assert false;
            }
            if ((arrMetadata[0].equals(null)) || 
                    (arrMetadata[1].equals(null))) {
                assert(false);
            } else {
                arrMetadata[0] = arrMetadata[0].replaceAll("&lt;", "<");
                arrMetadata[0] = arrMetadata[0].replaceAll("&gt;", ">");
                arrMetadata[1] = arrMetadata[1].replaceAll("&lt;", "<");
                arrMetadata[1] = arrMetadata[1].replaceAll("&gt;", ">");
                if (FederationManager.getExitCode(fm.importEntity(webClient,
                        executionRealm, arrMetadata[0], arrMetadata[1],
                        cot, "saml2")) != 0) {
                    arrMetadata[0] = null;
                    arrMetadata[1] = null;
                    assert(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrMetadata;
        
    }
}
