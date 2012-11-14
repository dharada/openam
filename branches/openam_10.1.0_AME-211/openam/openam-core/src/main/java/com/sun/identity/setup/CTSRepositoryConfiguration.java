/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock US Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package com.sun.identity.setup;

import com.iplanet.am.util.SSLSocketFactoryManager;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.LDAPUtils;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.shared.ldap.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.*;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;


/**
 * Provide a CTS Repository Configuration class for the internal/external
 * Configuration Directory as OpenAM's Session and Token Store.
 * <p/>
 *
 * ** This is a package protected Class! **
 *
 * @author jeff.schenk@forgerock.com
 */
class CTSRepositoryConfiguration {
    private static final String CTSRepository;
    /**
     * Our CTS Repository Configuration Setup Instance.
     */
    private static CTSRepositoryConfiguration instance = new CTSRepositoryConfiguration();

    // Initialization Static Stanza.
    static {
        ResourceBundle rb = ResourceBundle.getBundle(
            SetupConstants.PROPERTY_FILENAME);
        CTSRepository = rb.getString("ctsRepository");
    }

    // Limit to a Singleton Instance.
    private CTSRepositoryConfiguration() {
    }
    
    public static CTSRepositoryConfiguration getInstance() {
        return instance;
    }

    /**
     * Perform configuration of the CTS Repository.
     *
     * @param ctsRepository - Map Key Value pairs for CTS Repository Configuration.
     * @param basedir - Specified Base Directory for Repository.
     * @param servletCtx - Servlet Context for accessing files.
     * @param adminToken - OpenAM Admin Token for Authorized Access.
     * @throws Exception
     */
    void configure(
        Map ctsRepository,
        String basedir,
        ServletContext servletCtx,
        SSOToken adminToken
    ) throws Exception {
        String type = 
            (String) ctsRepository.get(SetupConstants.CONFIG_VAR_DATA_STORE);
        // If type not specified, use the Default of 'dirServer'.
        if (type == null) {
            type = SetupConstants.SMS_DS_DATASTORE;
        }

        ResourceBundle rb = ResourceBundle.getBundle(
            SetupConstants.SCHEMA_PROPERTY_FILENAME);
        String configName = "";

        //opendsSmsSchema
        //OPENDS_SMS_PROPERTY_FILENAME = "opendsSmsSchema";

        //dsSmsSchema
        //DS_SMS_PROPERTY_FILENAME

        String strFiles = rb.getString(SetupConstants.ODSEE_LDIF);

        if (type.equals(SetupConstants.UM_LDAPv3ForOpenDS)) {
            strFiles = rb.getString(SetupConstants.OpenDS_LDIF);
            configName = "OpenDJ";
        } else if (type.equals(SetupConstants.UM_LDAPv3ForAD)) {
            strFiles = rb.getString(SetupConstants.AD_LDIF);
            configName = "Active Directory";
        } else if (type.equals(SetupConstants.UM_LDAPv3ForADDC)) {
            strFiles = rb.getString(SetupConstants.AD_LDIF);
            configName = "Active Directory with Domain Name";
            type = SetupConstants.UM_LDAPv3ForAD;
        } else if (type.equals(SetupConstants.UM_LDAPv3ForADAM)) {
            strFiles = rb.getString(SetupConstants.ADAM_LDIF);
            configName = "Active Directory Application Mode";
        } else if (type.equals(SetupConstants.UM_LDAPv3ForTivoli)) {
            strFiles = rb.getString(SetupConstants.TIVOLI_LDIF);
            configName = "Tivoli Directory Server";
        }

        loadSchema(ctsRepository, basedir, servletCtx, strFiles, type);
        addSubConfig(ctsRepository, type, configName, adminToken);
    }

    private void addSubConfig(
        Map ctsRepository,
        String type,
        String configName,
        SSOToken adminToken
    ) throws SMSException, SSOException, IOException {
        // Obtain our
        String xml =  getResourceContent(CTSRepository);
        if (xml != null) {
            Map data = ServicesDefaultValues.getDefaultValues();
            xml = StringUtils.strReplaceAll(xml, "@SM_CONFIG_ROOT_SUFFIX@",
                XMLUtils.escapeSpecialCharacters((String)data.get(
                    SetupConstants.SM_CONFIG_ROOT_SUFFIX)));
            xml = StringUtils.strReplaceAll(xml, "@UM_CONFIG_ROOT_SUFFIX@",
                XMLUtils.escapeSpecialCharacters((String) ctsRepository.get(
                    SetupConstants.USER_STORE_ROOT_SUFFIX)));
            xml = StringUtils.strReplaceAll(xml,
                "@" + SetupConstants.UM_DIRECTORY_SERVER + "@",
                XMLUtils.escapeSpecialCharacters(getHost(ctsRepository)));
            xml = StringUtils.strReplaceAll(xml,
                "@" + SetupConstants.UM_DIRECTORY_PORT + "@",
                XMLUtils.escapeSpecialCharacters(getPort(ctsRepository)));
            xml = StringUtils.strReplaceAll(xml, "@UM_DS_DIRMGRDN@", 
                XMLUtils.escapeSpecialCharacters(getBindDN(ctsRepository)));
            xml = StringUtils.strReplaceAll(xml, "@UM_DS_DIRMGRPASSWD@",
                XMLUtils.escapeSpecialCharacters(getBindPassword(ctsRepository)));

            String s = (String) ctsRepository.get(SetupConstants.USER_STORE_SSL);
            String ssl = ((s != null) && s.equals("SSL")) ? "true" : "false";
            xml = StringUtils.strReplaceAll(xml, "@UM_SSL@", ssl);
            xml = StringUtils.strReplaceAll(xml, "@CONFIG_NAME@", configName);
            xml = StringUtils.strReplaceAll(xml, "@CONFIG_ID@", type);

            registerService(xml, adminToken);
        }
    }

    static String getHost(Map ctsRepository) {
        return (String)ctsRepository.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST);
    }
    
    static String getPort(Map ctsRepository) {
        return (String)ctsRepository.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT);
    }
    
    static String getBindDN(Map ctsRepository) {
        return (String) ctsRepository.get(SetupConstants.CONFIG_VAR_DS_MGR_DN);
    }
    
    static String getBindPassword(Map ctsRepository) {
        return (String) ctsRepository.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
    }

    static String getSSL(Map ctsRepository) {
        return (String) ctsRepository.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_SSL);
    }

    private void loadSchema(
        Map userRepo, 
        String basedir,
        ServletContext servletCtx,
        String strFiles,
        String type
    ) throws Exception {
        LDAPConnection ld = null;
        try {
            ld = getLDAPConnection(userRepo);
            String dbName = getDBName(userRepo, ld);
            List schemas = writeSchemaFiles(basedir, dbName, 
                servletCtx, strFiles, userRepo, type);
            for (Iterator i = schemas.iterator(); i.hasNext(); ) {
                String file = (String)i.next();
                Object[] params = {file};
                SetupProgress.reportStart("emb.loadingschema", params);
                LDAPUtils.createSchemaFromLDIF(file, ld);
                SetupProgress.reportEnd("emb.success", null);

                File f = new File(file);
                f.delete();
            }
        } finally {
            disconnectDServer(ld);
        }
    }
    
    private List writeSchemaFiles(
        String basedir, 
        String dbName,
        ServletContext servletCtx,
        String strFiles,
        Map ctsRepository,
        String type
    ) throws Exception {
        List files = new ArrayList();

        StringTokenizer st = new StringTokenizer(strFiles);
        while (st.hasMoreTokens()) {
            String file = st.nextToken();
            InputStreamReader fin = new InputStreamReader(
                AMSetupServlet.getResourceAsStream(servletCtx, file));
            StringBuilder sbuf = new StringBuilder();
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
            FileWriter fout = null;
            try {
                int idx = file.lastIndexOf("/");
                String absFile = (idx != -1) ? file.substring(idx+1) 
                    : file;
                String outfile = basedir + "/" + absFile;
                fout = new FileWriter(outfile);
                String inpStr = sbuf.toString();
                inpStr = StringUtils.strReplaceAll(inpStr, 
                    "@DB_NAME@", dbName);
                String suffix = (String) ctsRepository.get(
                    SetupConstants.CTS_VAR_ROOT_SUFFIX);
                if (suffix != null) {
                    inpStr = StringUtils.strReplaceAll(inpStr, 
                        "@ctsRootSuffix@", suffix);
                }
                fout.write(ServicesDefaultValues.tagSwap(inpStr));
                files.add(outfile);
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
            }
        }
        return files;
    }
    
    private String getResourceContent(String resName) 
        throws IOException {
        BufferedReader rawReader = null;
        
        String content = null;

        try {
            rawReader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resName)));
            StringBuilder buff = new StringBuilder();
            String line = null;

            while ((line = rawReader.readLine()) != null) {
                buff.append(line);
            }

            rawReader.close();
            rawReader = null;
            content = buff.toString();
        } finally {
            if (rawReader != null) {
                rawReader.close();
            }
        }
        return content;
    }
    
    private void disconnectDServer(LDAPConnection ld)
        throws LDAPException {
        if ((ld != null) && ld.isConnected()) {
            ld.disconnect();
        }
    }
    
    private LDAPConnection getLDAPConnection(Map ctsRepository)
        throws Exception {
        String s = (String) ctsRepository.get(SetupConstants.USER_STORE_SSL);
        boolean ssl = ((s != null) && s.equals("SSL"));
        LDAPConnection ld = (ssl) ? new LDAPConnection(
            SSLSocketFactoryManager.getSSLSocketFactory()) :
            new LDAPConnection();
        ld.setConnectTimeout(300);

        int port = Integer.parseInt(getPort(ctsRepository));
        ld.connect(3, getHost(ctsRepository), port,
            getBindDN(ctsRepository), getBindPassword(ctsRepository));
        return ld;
    }

    private String getDBName(Map ctsRepository, LDAPConnection ld)
        throws LDAPException {
        String suffix = (String) ctsRepository.get(
            SetupConstants.USER_STORE_ROOT_SUFFIX);
        return LDAPUtils.getDBName(suffix, ld);
    }

    private void registerService(String xml, SSOToken adminSSOToken)
            throws SSOException, SMSException, IOException {
        ServiceManager serviceManager = new ServiceManager(adminSSOToken);
        InputStream serviceStream = null;
        try {
            serviceStream = (InputStream) new ByteArrayInputStream(
                    xml.getBytes());
            serviceManager.registerServices(serviceStream);
        } finally {
            if (serviceStream != null) {
                serviceStream.close();
            }
        }
    }

    static ServiceConfig getOrgConfig(SSOToken adminToken)
            throws SMSException, SSOException {
        ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, adminToken);
        ServiceConfig cfg = svcCfgMgr.getOrganizationConfig("", null);
        Map values = new HashMap();
        if (cfg == null) {
            OrganizationConfigManager orgCfgMgr =
                    new OrganizationConfigManager(adminToken, "/");
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                    IdConstants.REPO_SERVICE, adminToken);
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            Set attrs = orgSchema.getAttributeSchemas();

            for (Iterator iter = attrs.iterator(); iter.hasNext();) {
                AttributeSchema as = (AttributeSchema) iter.next();
                values.put(as.getName(), as.getDefaultValues());
            }
            cfg = orgCfgMgr.addServiceConfig(IdConstants.REPO_SERVICE,
                    values);
        }
        return cfg;
    }

}
