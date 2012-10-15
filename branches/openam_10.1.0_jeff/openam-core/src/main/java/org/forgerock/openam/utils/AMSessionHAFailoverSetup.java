/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010] [ForgeRock AS]
 *
 */
package org.forgerock.openam.utils;

import com.iplanet.dpro.session.exceptions.StoreException;
import com.iplanet.dpro.session.service.AMSessionRepository;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.opends.server.core.AddOperation;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.ldap.LDAPAttribute;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.types.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * AMSessionHAFailoverSetup
 * <p/>
 * This utility helper class provides the necessary
 * methods to perform the simple construction
 * and persistence of the configuration entry
 * to set the proper sub-configuration element
 * for Site and Session HA Failover.
 *
 * Pattern used applied from @see OpenDJPersistentStore.
 *
 * @author jeff.schenk@forgerock.com
 */
public class AMSessionHAFailoverSetup implements Constants {

    /**
     * Global Constants
     */
    public static final String AM_SESSION_SERVICE = "iPlanetAMSessionService";
    private static final String DEFAULT_SITE_SERVICE_ID = "Site";

    /**
     * Internal Directory Connection.
     */
    private static InternalClientConnection icConn;

    /**
     * Directory Helper Static Collection Classes
     */
    private static List<LDAPAttribute> objectClasses;
    private static LinkedHashSet<String> returnAttrs;
    private static LinkedHashSet<String> returnAttrs_DN_ONLY;

    /**
     * Instantiate Singleton Static Interface.
     */
    private static AMSessionHAFailoverSetup instance = new AMSessionHAFailoverSetup();

    /**
     * Define Session DN Constants
     */
    private static final String SM_CONFIG_ROOT_DN =
            SystemPropertiesManager.get(AMSessionRepository.SYS_PROPERTY_SM_CONFIG_ROOT_SUFFIX,
                    Constants.DEFAULT_ROOT_SUFFIX);

    private static final String SERVICES_BASE_ROOT_DN =
            "ou" + Constants.EQUALS + "services" + Constants.COMMA +
                    SM_CONFIG_ROOT_DN;

    // Example of Depicted DN:
    //dn: ou=MySite,ou=default,ou=GlobalConfig,ou=1.0,ou=iPlanetAMSessionService,
    //       ou=services,dc=openam,dc=forgerock,dc=org
    private static final String SITE_SESSION_FAILOVER_HA_SERVICES_BASE_DN_TEMPLATE =
            "ou" + Constants.EQUALS + "%1" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "default" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "GlobalConfig" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "1.0" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "%2" + Constants.COMMA +
                    SERVICES_BASE_ROOT_DN;
    /**
     * Default locked down constructor.
     */
    private AMSessionHAFailoverSetup() {
    }

    /**
     * Provide a singleton static instance.
     *
     * @return AMSessionHAFailoverSetup Instance.
     */
    public static AMSessionHAFailoverSetup getInstance() {
        return instance;
    }

    /**
     * Static Stanza Definition.
     */
    static {
        // Create and Initialize all Necessary Attribute Linked Sets.

        // DN and All Attributes to be returned.
        returnAttrs = new LinkedHashSet<String>();
        returnAttrs.add("dn");
        returnAttrs.add("objectClass");
        returnAttrs.add("ou");
        returnAttrs.add("sunKeyValue");
        returnAttrs.add("sunserviceID");
        returnAttrs.add("sunsmspriority");

        // DN Only return
        returnAttrs_DN_ONLY = new LinkedHashSet<String>();
        returnAttrs_DN_ONLY.add("dn");

        // Set up ObjectClasses
        List<String> valueList = new ArrayList<String>();
        valueList.add(Constants.TOP);
        valueList.add("organizationalUnit");
        valueList.add("sunServiceComponent");
        LDAPAttribute ldapAttr = new LDAPAttribute(Constants.OBJECTCLASS, valueList);
        objectClasses = new ArrayList<LDAPAttribute>();
        objectClasses.add(ldapAttr);

    } // End of Static Stanza.

    /**
     * Creates a new Site Sub configuration specifically for Session HA FO.
     *
     * @param siteName    Name of sub configuration, in our case the Site Name.
     * @param serviceName Name of the Specific Service.
     * @param values      Map of attribute name to its values.
     * @return boolean Indicates True, if method was successful or not.
     */
    public boolean createSiteAndSessionHAFOElementEntry(String siteName, String serviceName, Map values)
            throws StoreException {
        return setElementEntry(siteName, DEFAULT_SITE_SERVICE_ID, serviceName, values);
    }

    /**
     * Private Helper Method to perform a Configuration Element
     * Entry Set to either add or update the existing conifguration
     * element entry.
     *
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    private static boolean setElementEntry(String siteName, String serviceID, String serviceName, Map values) throws StoreException {
        boolean found;
        String baseDN = SITE_SESSION_FAILOVER_HA_SERVICES_BASE_DN_TEMPLATE.replace("%1", siteName).replace("%2", serviceName);
        // Perform a search to determine if the record exists already
        // so we know whether to Update the Record or Add.
        ResultCode resultCode = null;
        try {
            icConn = InternalClientConnection.getRootConnection();
            // Only return the Primary Key Attributes if any.
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                    SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, OBJECTCLASS_FILTER, returnAttrs);
            resultCode = iso.getResultCode();
            // Check Result Code on Search.
            if (resultCode == ResultCode.SUCCESS) {
                found = true;
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                found = false;
            } else {
                throw new StoreException("Unable to Read BaseDN:[" + baseDN +
                        "], LDAP ResultCode:[" + resultCode.toString() + "]");
            }
        } catch (DirectoryException dex) {
            throw new StoreException("Unable to Read BaseDN:[" + baseDN +
                    "], LDAP ResultCode:[" + ((resultCode != null) ? resultCode.toString() : "null") + "]", dex);
        }
        // Update/Modify or Store/Add/Bind if Element not found.
        if (found) {
            return modify(baseDN, serviceID, values);
        } else {
            return add(baseDN, serviceID, values);
        }
    }

    /**
     * Private helper method to perform the Addition of the Element Entry to the Configuration Directory.
     * @param serviceID - Sun Service Identifier.
     * @param values - ELement Attributes
     * @return boolean - Indicates if successful or not.
     * @throws StoreException
     */
    private static boolean add(String baseDN, String serviceID, Map values)
            throws StoreException {
        // Construct our Attribute List to be Applied.
        List<RawAttribute> attributeList = createAttributeList(serviceID, values);
        // Obtain our Internal Connection to Configuration Directory Store.
        icConn = InternalClientConnection.getRootConnection();
        // Add the Configuration Element to the Configuration Directory DIT.
        AddOperation ao = icConn.processAdd(baseDN, attributeList);
        ResultCode resultCode = ao.getResultCode();
        if (resultCode == ResultCode.SUCCESS) {
            return true;
        } else if (resultCode == ResultCode.ENTRY_ALREADY_EXISTS) {
            return false;
        } else {
            throw new StoreException("Unable to Add Configuration Element BaseDN:[" + baseDN +
                    "], LDAP ResultCode:[" + resultCode.toString() + "]");
        }
    }

    /**
     * Private helper method to perform the Modification of the Element Entry in the Configuration Directory.
     *
     * @param baseDN
     * @param serviceID - Sun Service Identifier.
     * @param values - ELement Attributes
     * @return boolean - Indicates if successful or not.
     * @throws StoreException
     */
    private static boolean modify(String baseDN, String serviceID, Map values)
            throws StoreException {
        // Construct our Attribute List then Transform to a Modification List.
        List<RawAttribute> attributeList = createAttributeList(serviceID, values);
        List<RawModification> modList = createModificationList(attributeList);
        // Obtain the Configuration Directory Internal Connection.
        icConn = InternalClientConnection.getRootConnection();
        // Apply the Modifications
        ModifyOperation mo = icConn.processModify(baseDN, modList);
        ResultCode resultCode = mo.getResultCode();
        // Interrogate the Result Code.
        if (resultCode == ResultCode.SUCCESS) {
            return true;
        } else {
            throw new StoreException("Unable to Modify Configuration Element BaseDN:[" + baseDN +
                    "], LDAP ResultCode:[" + resultCode.toString() + "]");
        }
    }

    /**
     * Prepare the RAW LDAP Modifications List for Directory
     * consumption.
     *
     * @param attrList Attribute List to be used in creation of a LDAP Modification List.
     * @return List<RawModification>
     */
    private static List<RawModification> createModificationList(List<RawAttribute> attrList)
            throws StoreException {
        List<RawModification> mods = new ArrayList<RawModification>();
        for (RawAttribute attr : attrList) {
            RawModification mod = new LDAPModification(ModificationType.REPLACE, attr);
            mods.add(mod);
        }
        return mods;
    }

    /**
     * Private Helper Method to create a New LDAP Attribute List
     * for those Attributes Applicable to this Entry.
     *
     * @param serviceID
     * @param values
     * @return
     */
    private static List<RawAttribute> createAttributeList(String serviceID, Map values) {
        // Construct our Element Entry to Add the new Entry to the DIT.
        List<RawAttribute> attrList = new ArrayList<RawAttribute>();
        attrList.addAll(objectClasses);
        // Construct Service Key Value
        List<String> valueList = new ArrayList<String>();
        for(Object key:values.keySet())
        {   valueList.add((String)key+EQUALS+values.get(key)); }
        LDAPAttribute sunKeyValue_LDAPAttribute = new LDAPAttribute("sunKeyValue", valueList);
        attrList.add(sunKeyValue_LDAPAttribute);
        // Construct the Service ID Attribute
        valueList = new ArrayList<String>();
        valueList.add(serviceID);
        LDAPAttribute sunServiceID_LDAPAttribute = new LDAPAttribute("sunserviceID", valueList);
        attrList.add(sunServiceID_LDAPAttribute);
        // Construct the SMS Priority
        valueList = new ArrayList<String>();
        valueList.add("0");
        LDAPAttribute sunsmspriority_LDAPAttribute = new LDAPAttribute("sunsmspriority", valueList);
        attrList.add(sunsmspriority_LDAPAttribute);
        // Return our attribute List.
        return attrList;
    }
}
