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
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */

package org.forgerock.openam.session.ha.amsessionstore.store.opendj;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.AMSessionRepository;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfigurationReferenceObject;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.*;
import org.forgerock.openam.session.model.*;
import org.forgerock.openam.shared.session.ha.amsessionstore.AMSessionRepositoryType;

import org.forgerock.i18n.LocalizableMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.iplanet.dpro.session.exceptions.NotFoundException;
import com.iplanet.dpro.session.exceptions.StoreException;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.types.*;


import static org.forgerock.openam.session.ha.i18n.AmsessionstoreMessages.*;

/**
 * Provide Implementation of AMSessionRepository using the internal
 * Configuration Directory as OpenAM's Session and Token Store.
 * <p/>
 * Having a replicated Directory
 * will provide the underlying SFO/HA for OpenAM Session Data.
 *
 * @author steve
 * @author jeff.schenk@forgerock.com
 */
public class OpenDJPersistentStore extends GeneralTaskRunnable implements AMSessionRepository {

    /**
     * Single Instance
     */
    private static volatile OpenDJPersistentStore instance;

    /**
     * Session Service Configuration Reference Object
     * Provides additional information to use an external Directory outside
     * of the configured Configuration data store which is the default.
     */
    private static volatile SessionServiceConfigurationReferenceObject
            sessionServiceConfigurationReferenceObject;

    /**
     * Used when we have an explicit External Persistence Directory Store
     * besides using the normal Configuration Store in embedded or
     * external mode.
     */
    private static volatile URL externalLDAPConnectionURL;

    /**
     * Debug Logging
     */
    private static Debug debug = SessionService.sessionDebug;

    /**
     * Service Globals
     */
    private static volatile boolean shutdown = false;
    private static Thread storeThread;

    private static final int SLEEP_INTERVAL = 60 * 1000;
    private final static String ID = "OpenDJPersistentStore";

    /**
     * Grace Period
     */
    private static long gracePeriod = 5 * 60; /* 5 mins in secs */

    /**
     * Configuration Definitions
     */
    static public final String SESSION = "session";

    private static boolean caseSensitiveUUID =
            SystemProperties.getAsBoolean(com.sun.identity.shared.Constants.CASE_SENSITIVE_UUID);
    /**
     * Internal LDAP Connection.
     */
    private static boolean icConnAvailable = false;
    private static boolean externalConnAvailable = false;
    private static InternalClientConnection icConn;
    private static LDAPConnectionPool externalLDAPConnectionPool;
    /**
     * Define Session DN Constants
     */
    private static final String SM_CONFIG_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_SM_CONFIG_ROOT_SUFFIX, Constants.DEFAULT_ROOT_SUFFIX);

    private static final String SESSION_FAILOVER_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_SESSION_HA_ROOT_SUFFIX);

    private static final String SESSION_FAILOVER_HA_BASE_DN =
            SESSION_FAILOVER_HA_ROOT_SUFFIX +
                    Constants.COMMA + SM_CONFIG_ROOT_SUFFIX;

    private static final String FAM_RECORDS_BASE_DN =
            Constants.OU_FAMRECORDS + Constants.COMMA + SESSION_FAILOVER_HA_BASE_DN;

    private static final String SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE =
            Constants.AMRECORD_NAMING_ATTR + Constants.EQUALS + "%" + Constants.COMMA +
                    FAM_RECORDS_BASE_DN;

    private static final String SERVER_RECORDS_BASE_DN =
            Constants.OU_AMSESSIONDB + Constants.COMMA + SESSION_FAILOVER_HA_BASE_DN;

    private static final String SERVER_ELEMENT_DN_TEMPLATE =
            "cn" + Constants.EQUALS + "%" + Constants.COMMA +
                    SERVER_RECORDS_BASE_DN;

    /**
     * Search Constructs
     */
    private final static String SKEY_FILTER_PRE = "(sKey=";
    private final static String SKEY_FILTER_POST = ")";
    private final static String EXPDATE_FILTER_PRE = "(expirationDate<=";
    private final static String EXPDATE_FILTER_POST = ")";
    private final static String NUM_SUB_ORD_ATTR = "numSubordinates";
    private final static String ALL_ATTRS = "(" + NUM_SUB_ORD_ATTR + "=*)";
    /**
     * Return Attribute Constructs
     */
    private static LinkedHashSet<String> returnAttrs;
    private static LinkedHashSet<String> numSubOrgAttrs;
    private static LinkedHashSet<String> returnAttrs_PKEY_ONLY;
    private static LinkedHashSet<String> returnAttrs_DN_ONLY;
    private static LinkedHashSet<String> serverAttrs;
    /**
     * Deferred Operation Queue.
     */
    private static ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>
            amSessionRepositoryDeferredOperationConcurrentLinkedQueue
            = new ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>();

    /**
     * Time period between two successive runs of repository cleanup thread
     * which checks and removes expired records
     */

    private static long cleanUpPeriod = 5 * 60 * 1000; // 5 min in milliseconds

    private static long cleanUpValue = 0;

    public static final String CLEANUP_RUN_PERIOD =
            "com.sun.identity.session.repository.cleanupRunPeriod";

    /**
     * Time period between two successive runs of DBHealthChecker thread which
     * checks for Database availability.
     */
    private static long healthCheckPeriod = 1 * 60 * 1000;

    public static final String HEALTH_CHECK_RUN_PERIOD =
            "com.sun.identity.session.repository.healthCheckRunPeriod";

    /**
     * This period is actual one that is used by the thread. The value is set to
     * the smallest value of cleanUPPeriod and healthCheckPeriod.
     */
    private static long runPeriod = 1 * 60 * 1000; // 1 min in milliseconds

    /**
     * Static Initialization Stanza
     * - Set all Timing Periods.
     */
    static {
        try {
            gracePeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_GRACE_PERIOD, String.valueOf(gracePeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_GRACE_PERIOD
                    + ", using default");
        }

        try {
            cleanUpPeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_RUN_PERIOD, String.valueOf(cleanUpPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_RUN_PERIOD
                    + ", using default");
        }

        try {
            healthCheckPeriod = Integer
                    .parseInt(SystemProperties.get(HEALTH_CHECK_RUN_PERIOD,
                            String.valueOf(healthCheckPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + HEALTH_CHECK_RUN_PERIOD
                    + ", using default");
        }

        runPeriod = (cleanUpPeriod <= healthCheckPeriod) ? cleanUpPeriod
                : healthCheckPeriod;
        cleanUpValue = cleanUpPeriod;

        // Create and Initialize all Necessary Attribute Linked Sets.
        returnAttrs = new LinkedHashSet<String>();
        returnAttrs.add("dn");
        returnAttrs.add(AMRecordDataEntry.PRI_KEY);
        returnAttrs.add(AMRecordDataEntry.SEC_KEY);
        returnAttrs.add(AMRecordDataEntry.AUX_DATA);
        returnAttrs.add(AMRecordDataEntry.DATA);
        returnAttrs.add(AMRecordDataEntry.SERIALIZED_INTERNAL_SESSION_BLOB);
        returnAttrs.add(AMRecordDataEntry.EXP_DATE);
        returnAttrs.add(AMRecordDataEntry.EXTRA_BYTE_ATTR);
        returnAttrs.add(AMRecordDataEntry.EXTRA_STRING_ATTR);
        returnAttrs.add(AMRecordDataEntry.OPERATION);
        returnAttrs.add(AMRecordDataEntry.SERVICE);
        returnAttrs.add(AMRecordDataEntry.STATE);

        returnAttrs_PKEY_ONLY = new LinkedHashSet<String>();
        returnAttrs_PKEY_ONLY.add(AMRecordDataEntry.PRI_KEY);

        returnAttrs_DN_ONLY = new LinkedHashSet<String>();
        returnAttrs_DN_ONLY.add("dn");

        numSubOrgAttrs = new LinkedHashSet<String>();
        numSubOrgAttrs.add(NUM_SUB_ORD_ATTR);

        serverAttrs = new LinkedHashSet<String>();
        serverAttrs.add("cn");
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_JMX_PORT);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_ADMIN_PORT);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_LDAP_PORT);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_REPL_PORT);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_ID);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_PROTOCOL);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_URL);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_ADDRESS);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_IS_UP);
        serverAttrs.add(AMSessionDBServer.ATTRIBUTE_NAME_TIMESTAMP);


    } // End of Static Initialization Stanza.

    /**
     * Protected Singleton from being Instantiated.
     */
    private OpenDJPersistentStore(SessionServiceConfigurationReferenceObject
                                          sessionServiceConfigurationReferenceObject) {
        OpenDJPersistentStore.sessionServiceConfigurationReferenceObject
                = sessionServiceConfigurationReferenceObject;
    }

    /**
     * Provide Service Instance Access to our Singleton
     *
     * @return OpenDJPersistentStore Singleton Instance.
     * @throws StoreException
     */
    public static AMSessionRepository getInstance() throws StoreException {
        try {
            if (instance == null) {
                // Initialize the Initial Singleton Service Instance.
                initialize(null);
            }
            return instance;
        } catch (StoreException se) {
            debug.error("Unable to Initialize the AMSessionRepository Singleton Service Component!");
            return null;
        }
    }

    /**
     * Provide Service Instance Access to our Singleton
     *
     * @return OpenDJPersistentStore Singleton Instance.
     * @throws StoreException
     */
    public static AMSessionRepository getInstance(
            SessionServiceConfigurationReferenceObject
                    sessionServiceConfigurationReferenceObject) throws StoreException {
        try {
            if (instance == null) {
                // Initialize the Initial Singleton Service Instance.
                initialize(sessionServiceConfigurationReferenceObject);
            }
            return instance;
        } catch (StoreException se) {
            debug.error("Unable to Initialize the AMSessionRepository Singleton Service Component!");
            return null;
        }
    }

    /**
     * Perform Initialization
     */
    private synchronized static void initialize(SessionServiceConfigurationReferenceObject
                                                        sessionServiceConfigurationReferenceObject)
            throws StoreException {
        // Establish our instance.
        instance = new OpenDJPersistentStore(sessionServiceConfigurationReferenceObject);
        // Initialize this Service
        debug.warning("Initializing Configuration for the OpenAM Session Repository using Implementation Class: " +
                OpenDJPersistentStore.class.getSimpleName());
        if (instance.sessionServiceConfigurationReferenceObject != null) {
            debug.warning("Additional Parameters supplied by SessionService for behaviour of OpenAM Session Repository: " +
                    instance.sessionServiceConfigurationReferenceObject.toString());
        } else {
            debug.warning("No Additional Parameters supplied by SessionService for behaviour of OpenAM Session Repository.");
        }
        // *******************************
        // Set up Shutdown Thread Hook.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                internalShutdown();
            }
        });
        // ***************************************************************
        // Obtain our Directory Connection and ensure we can access our
        // Internal Directory Connection or use an External Source as
        // per configuration, whose default is o=openam-session. Which can
        // and probably will change.
        // ***************************************************************

        // **********************************************************************************
        // Interrogate the Session Service Sub Configuration Parameters.
        // To determine where our store lies.
        // Our Store can be our embedded or an External Directory where our configuration
        // is stored or an external Directory (LDAP) or a RESTful resource via HTTP. (Long Term Goal)...
        // **********************************************************************************************
        if ((sessionServiceConfigurationReferenceObject != null)
                && (sessionServiceConfigurationReferenceObject.getAmSessionRepositoryType().
                equals(AMSessionRepositoryType.external))) {
            prepareExternalConnectionPool();
        } else {
            prepareInternalConnection();
        }

        // ******************************************
        // Start our AM Repository Store Thread.
        storeThread = new Thread(instance);
        storeThread.setName(ID);
        storeThread.start();
        debug.warning(DB_DJ_STR_OK.get().toString());

        // Finish Initialization
        debug.warning("Successful Configuration Initialization for the OpenAM Session Repository using Implementation Class: " +
                OpenDJPersistentStore.class.getSimpleName());
    }

    /**
     * Perform Service Shutdown.
     */
    @Override
    public void shutdown() {
        internalShutdown();
        debug.warning(DB_AM_SHUT.get().toString());
    }

    /**
     * Internal Service Shutdown Process.
     */
    protected static void internalShutdown() {
        shutdown = true;
        debug.warning(DB_AM_INT_SHUT.get().toString());
    }

    /**
     * Service Thread Run Process Loop.
     */
    @SuppressWarnings("SleepWhileInLoop")
    @Override
    public void run() {
        while (!shutdown) {
            try {
                // Delete any expired Sessions up to now.
                deleteExpired(nowInSeconds());
                // Process any Deferred Operations
                processDeferredAMSessionRepositoryOperations();
                Thread.sleep(SLEEP_INTERVAL);
            } catch (InterruptedException ie) {
                debug.warning(DB_THD_INT.get().toString(), ie);
            } catch (StoreException se) {
                debug.warning(DB_STR_EX.get().toString(), se);
            }
        }
    }

    /**
     * Service Method
     *
     * @param obj
     * @return
     */
    public boolean addElement(Object obj) {
        return false;
    }

    /**
     * Service Method
     *
     * @param obj
     * @return
     */
    public boolean removeElement(Object obj) {
        return false;
    }

    /**
     * Service Method
     *
     * @return
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * Saves session state to the repository If it is a new session (version ==
     * 0) it inserts new record(s) to the repository. For an existing session it
     * updates repository record instead while checking that versions in the
     * InternalSession and in the record match In case of version mismatch or
     * missing record IllegalArgumentException will be thrown
     *
     * @param is reference to <code>InternalSession</code> object being saved.
     * @throws Exception if anything goes wrong.
     */
    @Override
    public void save(InternalSession is) throws Exception {
        if (is == null) {
            return;
        }
        saveImmediate(is);
    }

    private void saveImmediate(InternalSession is) throws Exception {
        try {
            SessionID sid = is.getID();
            String key = SessionUtils.getEncryptedStorageKey(sid);
            if (key == null) {
                debug.error("OpenDJPersistenceStore.save(): Primary Encrypted Key "
                        + "null, not persisting Session.");
                return;
            }
            byte[] serializedInternalSession = SessionUtils.encode(is);
            long expirationTime = is.getExpirationTime() + gracePeriod;
            String uuid = caseSensitiveUUID ? is.getUUID() : is.getUUID().toLowerCase();

            FAMRecord famRec = new FAMRecord(
                    SESSION, FAMRecord.WRITE, key, expirationTime, uuid,
                    is.getState(), sid.toString(), serializedInternalSession);

            // Persist Session Record
            writeImmediate(famRec);

        } catch (Exception e) {
            debug.error("OpenDJPersistenceStore.save(): failed "
                    + "to save Session", e);
        }
    }

    /**
     * Takes an AMRecord and writes this to the store
     *
     * @param amRootEntity The record object to store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    @Override
    public void write(AMRootEntity amRootEntity) throws StoreException {
        if (amRootEntity == null) {
            return;
        }
        writeImmediate(amRootEntity);
    }

    /**
     * Takes an AMRecord and writes this to the store
     *
     * @param record The record object to store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    private void writeImmediate(AMRootEntity record)
            throws StoreException {
        boolean found = false;
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", (record).getPrimaryKey());

        logAMRootEntity(record, "OpenDJPersistenceStore.writeImmediate: \nBaseDN:[" + baseDN.toString() + "] ");
        // Perform a search to determine if the record exists already
        // so we know whether to Update the Record or Add.
        try {
            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            // Only return the Primary Key Attributes if any.
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                    SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, Constants.FAMRECORD_FILTER, returnAttrs_PKEY_ONLY);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                final LocalizableMessage message = DB_ENT_P.get(baseDN);
                debug.message(message.toString());
                found = true;
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                debug.warning(message.toString());
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(baseDN, resultCode.toString());
                debug.warning(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(baseDN);
            debug.error(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
        // Update or store/add/bind if Primary Key not found.
        if (found) {
            updateImmediate(record);
        } else {
            storeImmediate(record);
        }
    }

    /**
     * Perform a Store Immediate to the Directory, since our record does not
     * exist per our up-stream caller.
     *
     * @param record
     * @throws StoreException
     */
    private void storeImmediate(AMRootEntity record)
            throws StoreException {
        if ((record == null) || (record.getPrimaryKey() == null)) {
            return;
        }
        AMRecordDataEntry entry = new AMRecordDataEntry(record);
        List<RawAttribute> attrList = entry.getAttrList();
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", (record).getPrimaryKey());
        attrList.addAll(AMRecordDataEntry.getObjectClasses());
        if (!icConnAvailable) {
            icConn = InternalClientConnection.getRootConnection();
        }
        AddOperation ao = icConn.processAdd(baseDN, attrList);
        ResultCode resultCode = ao.getResultCode();

        if (resultCode == ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_SVR_CREATE.get(baseDN);
            debug.message(message.toString());
        } else if (resultCode == ResultCode.ENTRY_ALREADY_EXISTS) {
            final LocalizableMessage message = DB_SVR_CRE_FAIL.get(baseDN);
            debug.warning(message.toString());
        } else {
            final LocalizableMessage message = DB_SVR_CRE_FAIL2.get(baseDN, resultCode.toString());
            debug.warning(message.toString());
            throw new StoreException(message.toString());
        }
    }

    /**
     * Perform an Update Immediate to the Directory, since our record does already
     * exist per our up-stream caller.
     *
     * @param record
     * @throws StoreException
     */
    private void updateImmediate(AMRootEntity record)
            throws StoreException {
        List<RawModification> modList = createModificationList(record);
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", (record).getPrimaryKey());
        if (!icConnAvailable) {
            icConn = InternalClientConnection.getRootConnection();
        }
        ModifyOperation mo = icConn.processModify(baseDN, modList);
        ResultCode resultCode = mo.getResultCode();

        if (resultCode == ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_SVR_MOD.get(baseDN);
            debug.message("OpenDJPersistence.updateImmediate: [" + message + "]");
        } else {
            final LocalizableMessage message = DB_SVR_MOD_FAIL.get(baseDN, resultCode.toString());
            debug.warning(message.toString());
            throw new StoreException(message.toString());
        }
    }

    /**
     * Prepare the RAW LDAP Modifications List for Directory
     * consumption.
     *
     * @param record
     * @return List<RawModification>
     * @throws StoreException
     */
    private List<RawModification> createModificationList(AMRootEntity record)
            throws StoreException {
        List<RawModification> mods = new ArrayList<RawModification>();
        AMRecordDataEntry entry = new AMRecordDataEntry(record);
        List<RawAttribute> attrList = entry.getAttrList();

        for (RawAttribute attr : attrList) {
            RawModification mod = new LDAPModification(ModificationType.REPLACE, attr);
            mods.add(mod);
        }
        return mods;
    }

    /**
     * Deletes a record from the store.
     *
     * @param id The id of the record to delete from the store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     * @throws com.iplanet.dpro.session.exceptions.NotFoundException
     *
     */
    @Override
    public void delete(String id) throws StoreException, NotFoundException {
        deleteImmediate(id);
    }

    /**
     * Deletes session record from the repository.
     *
     * @param sid session ID.
     * @throws Exception if anything goes wrong.
     */
    @Override
    public void delete(SessionID sid) throws Exception {
        deleteImmediate(sid);
    }

    /**
     * Private Helper method for Delete Immediately by Session ID.
     *
     * @param sid
     * @throws Exception
     */
    private void deleteImmediate(SessionID sid) throws Exception {
        deleteImmediate(SessionUtils.getEncryptedStorageKey(sid));
    }

    /**
     * Private Helper method for Delete Immediately by Session ID.
     *
     * @param id
     * @throws StoreException
     * @throws NotFoundException
     */
    private void deleteImmediate(String id)
            throws StoreException, NotFoundException {
        if ((id == null) || (id.isEmpty())) {
            return;
        }
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", id);

        if (!icConnAvailable) {
            icConn = InternalClientConnection.getRootConnection();
        }
        DeleteOperation dop = icConn.processDelete(baseDN.toString());
        ResultCode resultCode = dop.getResultCode();

        if (resultCode != ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_ENT_DEL_FAIL.get(baseDN);
            debug.warning(message.toString() +
                    ", Result Code: " + resultCode.getResultCodeName().toString());
        }
    }

    /**
     * Delete all Expired Sessions, within Default Limits.
     *
     * @throws Exception
     */
    @Override
    public void deleteExpired() throws Exception {
        deleteExpired(nowInSeconds());
    }

    /**
     * Delete Expired Sessions older than specified
     * expiration Date.
     *
     * @param expDate The expDate in seconds
     * @throws StoreException
     */
    @Override
    public void deleteExpired(long expDate)
            throws StoreException {
        try {
            if (debug.messageEnabled()) {
                debug.message("OpenDJPersistence:deleteExpired Polling for any Expired Sessions older than: "
                        + getDate(expDate * 1000));
            }
            // Initialize Filter.
            StringBuilder filter = new StringBuilder();
            filter.append(EXPDATE_FILTER_PRE).append(expDate).append(EXPDATE_FILTER_POST);

            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            InternalSearchOperation iso = icConn.processSearch(FAM_RECORDS_BASE_DN,
                    SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, filter.toString(), returnAttrs_PKEY_ONLY);
            ResultCode resultCode = iso.getResultCode();
            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResults = iso.getSearchEntries();
                // Now Obtain the results, only results should be only the Primary Keys for quick result set.
                if ((searchResults != null) && (!searchResults.isEmpty())) {
                    for (SearchResultEntry entry : searchResults) {
                        List<Attribute> attributes = entry.getAttribute(AMRecordDataEntry.PRI_KEY);
                        if ((attributes != null) && (!attributes.isEmpty())) {
                            Iterator<AttributeValue> iterator = attributes.get(0).iterator();
                            if (iterator.hasNext()) {
                                String primaryKey = null;
                                try {
                                    primaryKey = iterator.next().getNormalizedValue().toString();
                                    deleteImmediate(primaryKey);
                                } catch (NotFoundException nfe) {
                                    final LocalizableMessage message = DB_ENT_NOT_FOUND.get(primaryKey);
                                    debug.warning(message.toString());
                                }
                            } // Iterator Check.
                        } // End of Check for Attributes
                    } // End of Inner For Loop of searchResults.
                } // End of Null Empty Result.
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(FAM_RECORDS_BASE_DN);
                debug.error(message.toString());
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(FAM_RECORDS_BASE_DN, resultCode.toString());
                debug.error(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(FAM_RECORDS_BASE_DN);
            debug.error(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        } catch (Exception ex) {
            if (!shutdown) {
                debug.error(DB_ENT_EXP_FAIL.get().toString(), ex);
            } else {
                debug.error(DB_ENT_EXP_FAIL.get().toString(), ex);
            }
        }
    }

    /**
     * Retrieve a persisted Internal Session.
     *
     * @param sid session ID
     * @return
     * @throws Exception
     */
    @Override
    public InternalSession retrieve(SessionID sid) throws Exception {
        try {
            String key = SessionUtils.getEncryptedStorageKey(sid);
            AMRootEntity amRootEntity = this.read(key);
            InternalSession is = null;
            if ((amRootEntity != null) &&
                    (amRootEntity.getSerializedInternalSessionBlob() != null)) {
                is = (InternalSession) SessionUtils.decode(amRootEntity.getSerializedInternalSessionBlob());
                logAMRootEntity(amRootEntity, "OpenDJPersistenceStore.retrieve:\nFound Session ID:[" + key + "] ");
            } else {
                debug.warning("OpenDJPersistenceStore.retrieve: Not Found for Session ID:[" + key + "] ");
            }
            // Return Internal Session.
            return is;
        } catch (Exception e) {
            debug.error("OpenDJPersistentStore.retrieve(): failed retrieving "
                    + "session", e);
            return null;
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     *
     * @param uuid User's universal unique ID.
     * @return Map of all Session for the user
     * @throws Exception if there is any problem with accessing the session
     *                   repository.
     */
    @Override
    public Map<String, String> getSessionsByUUID(String uuid) throws SessionException {
        try {
            AMRecord amRecord = (AMRecord) this.read(uuid);
            if ((amRecord != null) && (amRecord.getExtraStringAttributes() != null)) {
                return amRecord.getExtraStringAttributes();
            }
            return null;
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Read the Persisted Session Record from the Store.
     *
     * @param id The primary key of the record to find
     * @return
     * @throws NotFoundException
     * @throws StoreException
     */
    @Override
    public AMRootEntity read(String id)
            throws NotFoundException, StoreException {
        if ((id == null) || (id.isEmpty())) {
            return null;
        }
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", id);
        try {

            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                    SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, Constants.FAMRECORD_FILTER, returnAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList searchResult = iso.getSearchEntries();
                if (!searchResult.isEmpty()) {
                    SearchResultEntry entry =
                            (SearchResultEntry) searchResult.get(0);
                    List<Attribute> attributes = entry.getAttributes();
                    Map<String, Set<String>> results =
                            EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
                    // UnMarshal
                    AMRecordDataEntry dataEntry = new AMRecordDataEntry(baseDN.toString(), AMRecord.READ, results);
                    // Return UnMarshaled Object
                    logAMRootEntity(dataEntry.getAMRecord(), "OpenDJPersistenceStore.read: \nBaseDN:[" + baseDN.toString() + "] ");
                    // Return UnMarshaled Object
                    return dataEntry.getAMRecord();
                } else {
                    return null;
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                debug.warning(message.toString());
                return null;
            } else {
                Object[] params = {baseDN, resultCode};
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(baseDN, resultCode.toString());
                debug.warning(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(baseDN);
            debug.warning(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
    }

    /**
     * Read with Security Key.
     *
     * @param id The secondary key on which to search the store
     * @return
     * @throws StoreException
     * @throws NotFoundException
     */
    @Override
    public Set<String> readWithSecKey(String id)
            throws StoreException, NotFoundException {
        try {

            StringBuilder filter = new StringBuilder();
            filter.append(SKEY_FILTER_PRE).append(id).append(SKEY_FILTER_POST);

            if (debug.messageEnabled()) {
                debug.message("OpenDJPersistence.readWithSecKey: Attempting Read of BaseDN:[" + FAM_RECORDS_BASE_DN + "]");
            }
            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            InternalSearchOperation iso = icConn.processSearch(FAM_RECORDS_BASE_DN,
                    SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, filter.toString(), returnAttrs);
            ResultCode resultCode = iso.getResultCode();

            // Interrogate the LDAP ResultCode
            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();
                if (!searchResult.isEmpty()) {
                    Set<String> result = new HashSet<String>();
                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();
                        Map<String, Set<String>> results =
                                EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
                        Set<String> value = results.get(AMRecordDataEntry.DATA);
                        if (value != null && !value.isEmpty()) {
                            for (String v : value) {
                                result.add(v);
                            }
                        }
                    }
                    // Show Success
                    final LocalizableMessage message = DB_R_SEC_KEY_OK.get(id, Integer.toString(result.size()));
                    if (debug.messageEnabled()) {
                        debug.message(message.toString());
                    }
                    // return result
                    return result;
                } else {
                    return null;
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(FAM_RECORDS_BASE_DN);
                debug.message(message.toString());
                return null;
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(FAM_RECORDS_BASE_DN, resultCode.toString());
                debug.error(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(FAM_RECORDS_BASE_DN);
            debug.error(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
    }

    /**
     * Get Record Counts
     *
     * @param id
     * @return
     * @throws StoreException
     */
    @Override
    public Map<String, Long> getRecordCount(String id)
            throws StoreException {
        try {
            StringBuilder filter = new StringBuilder();
            filter.append(SKEY_FILTER_PRE).append(id).append(SKEY_FILTER_POST);

            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            InternalSearchOperation iso = icConn.processSearch(FAM_RECORDS_BASE_DN,
                    SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, filter.toString(), returnAttrs);
            ResultCode resultCode = iso.getResultCode();
            // Interrogate the LDAP Result Code.
            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();

                if (!searchResult.isEmpty()) {
                    Map<String, Long> result = new HashMap<String, Long>();

                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();
                        Map<String, Set<String>> results =
                                EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);

                        String key = "";
                        Long expDate = new Long(0);

                        Set<String> value = results.get(AMRecordDataEntry.AUX_DATA);

                        if (value != null && !value.isEmpty()) {
                            for (String v : value) {
                                key = v;
                            }
                        }

                        value = results.get(AMRecordDataEntry.EXP_DATE);

                        if (value != null && !value.isEmpty()) {
                            for (String v : value) {
                                expDate = AMRecordDataEntry.toAMDateFormat(v);
                            }
                        }

                        result.put(key, expDate);
                    }

                    final LocalizableMessage message = DB_GET_REC_CNT_OK.get(id, Integer.toString(result.size()));
                    debug.message(message.toString());
                    return result;
                } else {
                    return null;
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(FAM_RECORDS_BASE_DN);
                debug.message(message.toString());
                return null;
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(FAM_RECORDS_BASE_DN, resultCode.toString());
                debug.warning(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(FAM_RECORDS_BASE_DN);
            debug.warning(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
    }

    /**
     * Get DB Statistics
     *
     * @return
     */
    @Override
    public DBStatistics getDBStatistics() {
        DBStatistics stats = DBStatistics.getInstance();

        try {
            stats.setNumRecords(getNumSubordinates());
        } catch (StoreException se) {
            final LocalizableMessage message = DB_STATS_FAIL.get(se.getMessage());
            debug.warning(message.toString());
            stats.setNumRecords(-1);
        }

        return stats;
    }

    /**
     * Helper method to obtain number of Subordinates.
     *
     * @return
     * @throws StoreException
     */
    protected int getNumSubordinates()
            throws StoreException {
        int recordCount = -1;

        try {
            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            InternalSearchOperation iso = icConn.processSearch(FAM_RECORDS_BASE_DN,
                    SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, ALL_ATTRS, numSubOrgAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();

                if (!searchResult.isEmpty()) {
                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();

                        for (Attribute attr : attributes) {
                            if (attr.isVirtual() && attr.getName().equals(NUM_SUB_ORD_ATTR)) {
                                Iterator<AttributeValue> values = attr.iterator();

                                while (values.hasNext()) {
                                    AttributeValue value = values.next();

                                    try {
                                        recordCount = Integer.parseInt(value.toString());
                                    } catch (NumberFormatException nfe) {
                                        final LocalizableMessage message = DB_STATS_NFS.get(nfe.getMessage());
                                        debug.warning(message.toString());
                                        throw new StoreException(message.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(FAM_RECORDS_BASE_DN);
                debug.warning(message.toString());
                throw new StoreException(message.toString());
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(FAM_RECORDS_BASE_DN, resultCode.toString());
                debug.warning(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(FAM_RECORDS_BASE_DN);
            debug.error(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
        // return the recordCount from the Query against the Store.
        return recordCount;
    }

    /**
     * Return Service Run Period.
     *
     * @return long current run period.
     */
    @Override
    public long getRunPeriod() {
        return runPeriod;
    }

    /**
     * Obtain a List of Configured Servers
     *
     * @return Set<AMSessionDBServer>
     * @throws StoreException
     */
    public Set<AMSessionDBServer> getServers()
            throws StoreException {
        Set<AMSessionDBServer> serverList = new HashSet<AMSessionDBServer>();
        try {
            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            InternalSearchOperation iso = icConn.processSearch(SERVER_RECORDS_BASE_DN,
                    SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, "objectclass=*", serverAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();

                if (!searchResult.isEmpty()) {
                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();
                        AMSessionDBServer server = new AMSessionDBServer();

                        for (Attribute attribute : attributes) {
                            if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_ADMIN_PORT)) {
                                server.setAdminPort(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_JMX_PORT)) {
                                server.setJmxPort(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_LDAP_PORT)) {
                                server.setLdapPort(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_REPL_PORT)) {
                                server.setReplPort(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_ID)) {
                                server.setId(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_ADDRESS)) {
                                server.setAddress(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_IS_UP)) {
                                server.setUp(Boolean.valueOf(attribute.iterator().next().getValue().toString()));
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_PROTOCOL)) {
                                server.setProtocol(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_TIMESTAMP)) {
                                server.setTimeStamp(attribute.iterator().next().getValue().toString());
                            } else if (attribute.getName().equalsIgnoreCase(AMSessionDBServer.ATTRIBUTE_NAME_URL)) {
                                server.setUrl(attribute.iterator().next().getValue().toString());
                            } else {
                                final LocalizableMessage message = DB_UNK_ATTR.get(attribute.getName());
                                debug.warning(message.toString());
                            }
                        }
                        serverList.add(server);
                    }
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(SERVER_RECORDS_BASE_DN);
                debug.warning(message.toString());
                return null;
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(SERVER_RECORDS_BASE_DN, resultCode.toString());
                debug.warning(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(SERVER_RECORDS_BASE_DN);
            debug.error(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }

        return serverList;
    }

    /**
     * Register Server Entry
     *
     * @param id
     * @param protocol
     * @param url
     * @param address
     * @param isUp
     * @param isLocal
     */
    public synchronized void registerServerEntry(String id, String protocol, URL url, InetSocketAddress address,
                                                 boolean isUp, boolean isLocal) throws StoreException {
        // Validate.
        if ((id == null) || (id.isEmpty())) {
            debug.error("Unable to Register Server Entry, since no ID Provided!");
            return;
        }
        if ((protocol == null) || (protocol.isEmpty())) {
            debug.error("Unable to Register Server Entry, since no Protocol Provided!");
            return;
        }
        if ((url == null) || (url.toString().isEmpty())) {
            debug.error("Unable to Register Server Entry, since no URL Provided!");
            return;
        }
        if (address == null) {
            debug.error("Unable to Register Server Entry, since no Network Socket Address Provided!");
            return;
        }
        if (!icConnAvailable) {
            icConn = InternalClientConnection.getRootConnection();
        }

        // Formulate the DN for the Server.
        boolean found = false;
        String serverDN = SERVER_ELEMENT_DN_TEMPLATE.replace("%", url.toExternalForm());

        debug.message("OpenDJPersistenceStore.registerServerEntry: \nServerDN:[" + serverDN.toString() + "] ");
        // Perform a search to determine if the record exists already
        // so we know whether to Update the Record or Add.
        try {
            if (!icConnAvailable) {
                icConn = InternalClientConnection.getRootConnection();
            }
            // Only return the Primary Key Attributes if any.
            InternalSearchOperation iso = icConn.processSearch(serverDN.toString(),
                    SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, Constants.FAMRECORD_FILTER, returnAttrs_DN_ONLY);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                final LocalizableMessage message = DB_ENT_P.get(serverDN);
                debug.message(message.toString());
                found = true;
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(serverDN);
                debug.warning(message.toString());
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(serverDN, resultCode.toString());
                debug.warning(message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(serverDN);
            debug.error(message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }

        // Update or store/add/bind if Server DN not found.
        if (found) {
            updateServer(serverDN, id, protocol, url, address, isUp);
        } else {
            storeServer(serverDN, id, protocol, url, address, isUp);
        }
    }

    /**
     * Store a New Server to the AM Session DB.
     *
     * @param serverDN
     * @param id
     * @param protocol
     * @param url
     * @param address
     * @param isUp
     * @throws StoreException
     */
    private void storeServer(String serverDN, String id, String protocol, URL url, InetSocketAddress address,
                             boolean isUp) throws StoreException {
        AMSessionDBServer entry = new AMSessionDBServer(serverDN, id, protocol, url, address, isUp);
        List<RawAttribute> attrList = entry.getAttrList();
        attrList.addAll(AMSessionDBServer.getObjectClasses());
        if (!icConnAvailable) {
            icConn = InternalClientConnection.getRootConnection();
        }
        AddOperation ao = icConn.processAdd(serverDN, attrList);
        ResultCode resultCode = ao.getResultCode();

        if (resultCode == ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_SVR_CREATE.get(serverDN);
            debug.message(message.toString());
        } else if (resultCode == ResultCode.ENTRY_ALREADY_EXISTS) {
            final LocalizableMessage message = DB_SVR_CRE_FAIL.get(serverDN);
            debug.warning(message.toString());
        } else {
            final LocalizableMessage message = DB_SVR_CRE_FAIL2.get(serverDN, resultCode.toString());
            debug.warning(message.toString());
            throw new StoreException(message.toString());
        }
    }

    /**
     * Update the Server on the AM Session DB.
     *
     * @param serverDN
     * @param id
     * @param protocol
     * @param url
     * @param address
     * @param isUp
     * @throws StoreException
     */
    private void updateServer(String serverDN, String id, String protocol, URL url, InetSocketAddress address,
                              boolean isUp) throws StoreException {
        AMSessionDBServer entry = new AMSessionDBServer(serverDN, id, protocol, url, address, isUp);
        List<RawModification> modList = new ArrayList<RawModification>();
        List<RawAttribute> attrList = entry.getAttrList();

        for (RawAttribute attr : attrList) {
            RawModification mod = new LDAPModification(ModificationType.REPLACE, attr);
            modList.add(mod);
        }

        if (!icConnAvailable) {
            icConn = InternalClientConnection.getRootConnection();
        }
        ModifyOperation mo = icConn.processModify(serverDN, modList);
        ResultCode resultCode = mo.getResultCode();

        if (resultCode == ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_SVR_MOD.get(serverDN);
            debug.message("OpenDJPersistence.updateServer: [" + message + "]");
        } else {
            final LocalizableMessage message = DB_SVR_MOD_FAIL.get(serverDN, resultCode.toString());
            debug.warning(message.toString());
            throw new StoreException(message.toString());
        }
    }


    /**
     * De-Register a Server based upon the specified URL.
     *
     * @param serverUrl
     */
    public synchronized void deRegisterServer(String serverUrl) {
        if ((serverUrl == null) || (serverUrl.isEmpty())) {
            return;
        }
        if (!icConnAvailable) {
            icConn = InternalClientConnection.getRootConnection();
        }

        // TODO -- DeRegister a Server.

        StringBuilder dn = new StringBuilder();
        //dn.append(Constants.HOST_NAMING_ATTR).append(Constants.EQUALS).append(serverUrl);
        //dn.append(Constants.COMMA).append(Constants.HOSTS_BASE_DN);
        //dn.append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());

        DeleteOperation dop = icConn.processDelete(dn.toString());
        ResultCode resultCode = dop.getResultCode();

        if (resultCode != ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_DEL_FAIL.get(dn);
            //Log.logger.log(Level.WARNING, message.toString());
        }
    }


    /**
     * Private Helper Method to Obtain a DN from a Host URL.
     *
     * @param urlHost
     * @return
     */
    private static String getFQDN(String urlHost) {
        try {
            URL url = new URL(urlHost);
            return url.getHost();
        } catch (MalformedURLException mue) {
            final LocalizableMessage message = DB_MAL_URL.get(urlHost);
            debug.warning(message.toString());
            return urlHost;
        }
    }

    /**
     * Process any and all deferred AM Session Repository Operations.
     */
    private synchronized void processDeferredAMSessionRepositoryOperations() {
        int count = 0;
        ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>
                tobeRemoved
                = new ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>();
        for (AMSessionRepositoryDeferredOperation amSessionRepositoryDeferredOperation :
                amSessionRepositoryDeferredOperationConcurrentLinkedQueue) {
            count++;

            // TODO -- Build out Implementation to invoke Session Repository Events.
            // TODO -- Add Statistics Gathering for Post to Admin Statistics View.

            // Place Entry in Collection to be removed from Queue.
            tobeRemoved.add(amSessionRepositoryDeferredOperation);
        }
        amSessionRepositoryDeferredOperationConcurrentLinkedQueue.removeAll(tobeRemoved);
        if (count > 0) {
            debug.warning("Performed " + count + " Deferred Operations.");
        }
    }

    /**
     * Logging Helper Method.
     *
     * @param amRootEntity
     * @param message
     */
    private void logAMRootEntity(AMRootEntity amRootEntity, String message) {
        // Set to Message to Error to see messages.
        //if (debug.messageEnabled()) {
        debug.error(
                ((message != null) && (!message.isEmpty()) ? message : "") +
                        "\nService:[" + amRootEntity.getService() + "]," +
                        "\n     Op:[" + amRootEntity.getOperation() + "]," +
                        "\n     PK:[" + amRootEntity.getPrimaryKey() + "]," +
                        "\n     SK:[" + amRootEntity.getSecondaryKey() + "]," +
                        "\n  State:[" + amRootEntity.getState() + "]," +
                        "\nExpTime:[" + amRootEntity.getExpDate() + " = "
                        + getDate(amRootEntity.getExpDate() * 1000) + "]," +
                        "\n     IS:[" + (
                        (amRootEntity.getSerializedInternalSessionBlob() != null) ?
                                amRootEntity.getSerializedInternalSessionBlob().length + " bytes]" : "null]") +
                        "\n   Data:[" + (
                        (amRootEntity.getData() != null) ?
                                amRootEntity.getData().length() + " bytes]" : "null]") +
                        "\nAuxData:[" + (
                        (amRootEntity.getAuxData() != null) ?
                                amRootEntity.getAuxData().length() + " bytes]" : "null].")
        );
        //}
    }

    /**
     * Return current Time in Seconds.
     *
     * @return long Time in Seconds.
     */
    private static long nowInSeconds() {
        return Calendar.getInstance().getTimeInMillis() / 1000;
    }

    /**
     * Return specified Milliseconds in a Date Object.
     *
     * @param milliseconds
     * @return Date
     */
    private static Date getDate(long milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(milliseconds);
        return cal.getTime();
    }

    /**
     * Prepare the Internal Connection for Use as our
     * Session Persistent store.
     *
     * @throws StoreException
     */
    private synchronized static void prepareInternalConnection() throws StoreException {
        try {
            // Using Embedded Directory Store or where ever the OpenAM Configuration Store resides.
            icConn = InternalClientConnection.getRootConnection();
            icConnAvailable = true;
            externalConnAvailable = false;
            // Continue with established Connection to ensure we have our top level DN for our Storage use.
            InternalSearchOperation iso = icConn.processSearch(SESSION_FAILOVER_HA_BASE_DN,
                    SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                    0, 0, false, Constants.FAMRECORD_FILTER, returnAttrs_DN_ONLY);
            debug.warning("Search for base container: " + SESSION_FAILOVER_HA_BASE_DN +
                    ", yielded Result Code:[" + iso.getResultCode().toString() + "]");
            if (iso.getResultCode() == ResultCode.SUCCESS) {
                final LocalizableMessage message = DB_ENT_P.get(SESSION_FAILOVER_HA_BASE_DN);
                debug.message(message.toString());
            } else if (iso.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(SESSION_FAILOVER_HA_BASE_DN);
                debug.warning(message.toString());
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(SESSION_FAILOVER_HA_BASE_DN, iso.getResultCode().toString());
                debug.warning(message.toString());
                icConnAvailable = false;
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException directoryException) {
            debug.error("Unable to obtain the Internal Root Container for Session Persistence!",
                    directoryException);
            icConnAvailable = false;
        }
    }

    /**
     * Prepare the External Connection Pool for Use as our
     * Session Persistent store.
     *
     * @throws StoreException
     */
    private synchronized static void prepareExternalConnectionPool() throws StoreException {
        LDAPConnection ldapConnection = null;
        try {
            externalLDAPConnectionURL = new URL(sessionServiceConfigurationReferenceObject.getSessionRepositoryURL());
            if (!externalLDAPConnectionURL.getProtocol().startsWith("ldap")) {
                // TODO Support other Protocols, such as http[s] for restful calls...
                throw new IllegalArgumentException(
                        "Currently External Session Persistence only supports LDAP as a storage protocol, Protocol" +
                                externalLDAPConnectionURL.getProtocol() + ", is Invalid!");
            }

            // Establish an independent LDAP Connection Pool to this Resource.
            externalLDAPConnectionPool = new LDAPConnectionPool("OpenAM_SF_EXT_STORE_POOL",
                    sessionServiceConfigurationReferenceObject.getMinPoolSize(),
                    sessionServiceConfigurationReferenceObject.getMaxPoolSize(),
                    externalLDAPConnectionURL.getHost(),
                    externalLDAPConnectionURL.getPort(),
                    sessionServiceConfigurationReferenceObject.getSessionStoreUserName(),
                    sessionServiceConfigurationReferenceObject.getSessionStorePassword());

            ldapConnection = externalLDAPConnectionPool.getConnection();



            // TODO --

            // Continue with established Connection to ensure we have our top level DN for our Storage use.
            LDAPSearchResults results = ldapConnection.search(sessionServiceConfigurationReferenceObject.getSessionRepositoryRootDN(),
                    LDAPv2.SCOPE_ONE, "(objectclass=*)", new String[]{}, false);
            if ( (results != null) && (results.getCount()>0) )
            {
                debug.error("Search for base container: " + sessionServiceConfigurationReferenceObject.getSessionRepositoryRootDN() +
                        ", was successful and yielded Entries Found:[" + results.getCount() + "]");


            } else {

                debug.error("Search for base container: " + sessionServiceConfigurationReferenceObject.getSessionRepositoryRootDN() +
                        ", Failed!");
            }


            /*

            LDAPSearchResults results = ldapConnection.search(sessionServiceConfigurationReferenceObject.getSessionRepositoryRootDN(),
             SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
             0, 0, false, Constants.FAMRECORD_FILTER, returnAttrs_DN_ONLY);


             debug.warning("Search for base container: " + SESSION_FAILOVER_HA_BASE_DN +
             ", yielded Result Code:[" + iso.getResultCode().toString() + "]");
             if (iso.getResultCode() == ResultCode.SUCCESS) {
             final LocalizableMessage message = DB_ENT_P.get(SESSION_FAILOVER_HA_BASE_DN);
             debug.message(message.toString());
             } else if (iso.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
             final LocalizableMessage message = DB_ENT_NOT_P.get(SESSION_FAILOVER_HA_BASE_DN);
             debug.warning(message.toString());
             } else {
             final LocalizableMessage message = DB_ENT_ACC_FAIL.get(SESSION_FAILOVER_HA_BASE_DN, iso.getResultCode().toString());
             debug.warning(message.toString());
             icConnAvailable = false;
             throw new StoreException(message.toString());
             }

             **/

            icConnAvailable = true;
            externalConnAvailable = true;

        } catch (MalformedURLException mue) {
            icConnAvailable = false;
            externalConnAvailable = false;
            throw new StoreException("Invalid URL supplied for External Session Persistence Store, " +
                    "Session Failover will not be Available! " + mue.getMessage(), mue);

        } catch (com.sun.identity.shared.ldap.LDAPException ldapException) {
            icConnAvailable = false;
            externalConnAvailable = false;
            throw new StoreException("LDAP Exception occurred obtaining External LDAP Connection Pool, " +
                    "Session Failover will not be Available! " + ldapException.getLDAPErrorMessage(), ldapException);
        } finally {
            if (ldapConnection != null) {
                externalLDAPConnectionPool.close(ldapConnection);
            }
        }
    }

}
