/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openam.session.model;

import com.sun.identity.shared.Constants;
import org.opends.server.protocols.ldap.LDAPAttribute;
import org.opends.server.types.RawAttribute;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * OpenAM Session DB Server POJO
 *
 * @author steve
 * @author jeff.schenk@forgerock.com
 */
public class AMSessionDBServer implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ATTRIBUTE_NAME_ID = "openamServerID";
    public static final String ATTRIBUTE_NAME_PROTOCOL = "openamServerProtocol";
    public static final String ATTRIBUTE_NAME_URL = "openamServerURL";
    public static final String ATTRIBUTE_NAME_ADDRESS = "openamServerAddress";
    public static final String ATTRIBUTE_NAME_IS_UP = "openamServerIsUp";
    public static final String ATTRIBUTE_NAME_TIMESTAMP = "openamServerTimeStamp";
    public static final String ATTRIBUTE_NAME_ADMIN_PORT = "adminPort";
    public static final String ATTRIBUTE_NAME_LDAP_PORT = "ldapPort";
    public static final String ATTRIBUTE_NAME_JMX_PORT = "jmxPort";
    public static final String ATTRIBUTE_NAME_REPL_PORT = "replPort";

    private String serverDN;

    private String commonName;

    private String id;
    private String protocol;
    private URL url;
    private InetSocketAddress address;
    private boolean isUp;
    private long timeStamp;
    private String adminPort;
    private String ldapPort;
    private String jmxPort;
    private String replPort;

    private Map<String, Set<String>> attributeValues = new HashMap<String, Set<String>>();

    private static SimpleDateFormat formatter = null;
    public static List<LDAPAttribute> objectClasses;

    static {
        initialize();
    }

    private static void initialize() {
        List<String> valueList = new ArrayList<String>();
        valueList.add(Constants.TOP);
        valueList.add(Constants.FR_AMSESSIONDB);
        LDAPAttribute ldapAttr = new LDAPAttribute(Constants.OBJECTCLASS, valueList);
        objectClasses = new ArrayList<LDAPAttribute>();
        objectClasses.add(ldapAttr);

        formatter = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    /**
     * Default Constructor
     */
    public AMSessionDBServer() {
    }

    /**
     * All Fields Constructor
     *
     * @param serverDN
     * @param id
     * @param protocol
     * @param url
     * @param address
     * @param up
     */
    public AMSessionDBServer(String serverDN, String id, String protocol, URL url, InetSocketAddress address,
                             boolean up) {
        this.serverDN = serverDN;
        this.id = id;
        this.protocol = protocol;
        this.url = url;
        this.address = address;
        this.isUp = up;
        this.timeStamp = Calendar.getInstance().getTimeInMillis();

        // Set the Attribute Values Array Map as Well.
        Set<String> set = new HashSet<String>();
        set.add(this.id);
        attributeValues.put(ATTRIBUTE_NAME_ID, set);

        set = new HashSet<String>();
        set.add(this.protocol);
        attributeValues.put(ATTRIBUTE_NAME_PROTOCOL, set);

        set = new HashSet<String>();
        set.add(this.url.toExternalForm());
        attributeValues.put(ATTRIBUTE_NAME_URL, set);

        set = new HashSet<String>();
        set.add(this.address.toString());
        attributeValues.put(ATTRIBUTE_NAME_ADDRESS, set);

        set = new HashSet<String>();
        set.add(Boolean.toString(this.isUp));
        attributeValues.put(ATTRIBUTE_NAME_IS_UP, set);

        set = new HashSet<String>();
        set.add(AMRecordDataEntry.toDJDateFormat(this.timeStamp));
        attributeValues.put(ATTRIBUTE_NAME_TIMESTAMP, set);

    }

    public String getServerDN() {
        return serverDN;
    }

    public String getId() {
        return id;
    }

    public String getProtocol() {
        return protocol;
    }

    public URL getUrl() {
        return url;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public boolean isUp() {
        return isUp;
    }

    public Map<String, Set<String>> getAttributeValues() {
        return attributeValues;
    }

    public void setServerDN(String serverDN) {
        this.serverDN = serverDN;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public void setUp(boolean up) {
        isUp = up;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setAttributeValues(Map<String, Set<String>> attributeValues) {
        this.attributeValues = attributeValues;
    }

    public String getAdminPort() {
        return adminPort;
    }

    public void setAdminPort(String adminPort) {
        this.adminPort = adminPort;
    }

    public String getLdapPort() {
        return ldapPort;
    }

    public void setLdapPort(String ldapPort) {
        this.ldapPort = ldapPort;
    }

    public String getJmxPort() {
        return jmxPort;
    }

    public void setJmxPort(String jmxPort) {
        this.jmxPort = jmxPort;
    }

    public String getReplPort() {
        return replPort;
    }

    public void setReplPort(String replPort) {
        this.replPort = replPort;
    }

    public List<RawAttribute> getAttrList() {
        List<RawAttribute> attrList =
                new ArrayList<RawAttribute>(this.attributeValues.size());
        // Set up all Attributes
        for (Map.Entry<String, Set<String>> entry : this.attributeValues.entrySet()) {
            Set<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                List<String> valueList = new ArrayList<String>();
                valueList.addAll(values);
                attrList.add(new LDAPAttribute(entry.getKey(), valueList));
            }
        }

        return attrList;
    }

    public static List<LDAPAttribute> getObjectClasses() {
        return objectClasses;
    }

}
