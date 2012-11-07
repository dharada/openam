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
package com.sun.identity.saml2.common;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.coretoken.interfaces.AMTokenRepository;
import com.sun.identity.coretoken.interfaces.AMTokenSAML2Repository;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.model.FAMRecord;

import javax.jms.IllegalStateException;
import java.util.*;


/**
 * This class is used in SAML2  mode to store/recover serialized
 * state of Assertion/Response object
 */
public class CTSPersistentSAML2Store extends GeneralTaskRunnable
    implements AMTokenSAML2Repository {

    /**
     * This Instances Server ID.
     */
    private static String serverId;

    /**
     * Indicator for Persistence Store.
     */
    private static boolean isDatabaseUp = false;
    private static boolean lastLoggedDBStatusIsUp = false;

    /**
     * Reference the AMTokenSAML2Repository Implementation Class for our
     * persistence operations.
     */
    private static volatile AMTokenSAML2Repository amTokenSAML2Repository;

    /**
     * grace period before expired session records are removed from the
     * repository
     */
    private static long gracePeriod = 5 * 60; /* 5 mins in secs */

    private static final String CLEANUP_GRACE_PERIOD =
        "com.sun.identity.session.repository.cleanupGracePeriod";

    private static final String BRIEF_DB_ERROR_MSG =
        "SAML2 failover service is not functional due to Token Persistent Store unavailability.";

    private static final String DB_ERROR_MSG =
        "SAML2 Token Persistent Store is not available at this moment."
            + "Please check with the system administrator " +
                    "for appropriate actions";

    private static final String LOG_MSG_DB_BACK_ONLINE =
        "SESSION_DATABASE_BACK_ONLINE";

    private static final String LOG_MSG_DB_UNAVAILABLE =
        "SESSION_DATABASE_UNAVAILABLE";

    /**
     * Time period between two successive runs of repository cleanup thread
     * which checks and removes expired records
     */

    private static long cleanUpPeriod = 5 * 60 * 1000; // 5 min in milliseconds

    private static long cleanUpValue = 0;

    public static final String CLEANUP_RUN_PERIOD =
        "com.sun.identity.saml2.repository.cleanupRunPeriod";

    /**
     * Time period between two successive runs of DBHealthChecker thread which
     * checks for Database availability.
     */
    private static long healthCheckPeriod = 1 * 60 * 1000;

    public static final String HEALTH_CHECK_RUN_PERIOD =
        "com.sun.identity.saml2.repository.healthCheckRunPeriod";

    /**
     * This period is actual one that is used by the thread. The value is set to
     * the smallest value of cleanUPPeriod and healthCheckPeriod.
     */
    private static long runPeriod = 1 * 60 * 1000; // 1 min in milliseconds


    static Debug debug = Debug.getInstance("amToken");
    private String SAML2="saml2";

    /**
     * Static Initialization Stanza.
     */
    static {
        try {
            gracePeriod = Integer.parseInt(SystemPropertiesManager.get(
                    CLEANUP_GRACE_PERIOD, String.valueOf(gracePeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_GRACE_PERIOD
                    + ", using default");
        }

        try {
            cleanUpPeriod = Integer.parseInt(SystemPropertiesManager.get(
                    CLEANUP_RUN_PERIOD, String.valueOf(cleanUpPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_RUN_PERIOD
                    + ", using default");
        }

        try {
            healthCheckPeriod = Integer
                    .parseInt(SystemPropertiesManager.get(HEALTH_CHECK_RUN_PERIOD,
                            String.valueOf(healthCheckPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + HEALTH_CHECK_RUN_PERIOD
                    + ", using default");
        }

        runPeriod = (cleanUpPeriod <= healthCheckPeriod) ? cleanUpPeriod
                : healthCheckPeriod;
        cleanUpValue = cleanUpPeriod;
    }

   /**
    *
    * Constructs new AMTokenSAML2Repository
    * @exception Exception when cannot create a new SAML2 repository
    *
    */
   public CTSPersistentSAML2Store() throws Exception {

        String thisSessionServerProtocol = SystemPropertiesManager
                .get(Constants.AM_SERVER_PROTOCOL);
        String thisSessionServer = SystemPropertiesManager
                .get(Constants.AM_SERVER_HOST);
        String thisSessionServerPortAsString = SystemPropertiesManager
                .get(Constants.AM_SERVER_PORT);
        String thisSessionURI = SystemPropertiesManager
                .get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

        if (thisSessionServerProtocol == null
                || thisSessionServerPortAsString == null
                || thisSessionServer == null) {
            throw new SessionException(SessionBundle.rbName,
                    "propertyMustBeSet", null);
        }

        serverId = WebtopNaming.getServerID(thisSessionServerProtocol,
                thisSessionServer, thisSessionServerPortAsString,
                thisSessionURI);
        initPersistSession();   

        SystemTimer.getTimer().schedule(this, new Date((
            System.currentTimeMillis() / 1000) * 1000));
    }

    /**
     *
     * Initialize the Reference to our CTS Persistent Store.
     */
    private void initPersistSession() {
        try {
            // Acquire a Reference to our Singleton for accessing
            // the Token Repository.
            amTokenSAML2Repository = SAML2RepositoryFactory.getInstance();
            if (amTokenSAML2Repository != null) {
                isDatabaseUp = true;
            } else {
                // This Throw will be caught below.
                throw new Exception("Unable to acquire amTokenSAML2Repository Reference from Factory!");
            }
        } catch (Exception e) {
            isDatabaseUp = false;
            debug.error(BRIEF_DB_ERROR_MSG);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        }

    }

   /**
    * Retrives existing SAML2 object from persistent datastore
    * @param samlKey primary key 
    * @return SAML2 object, if failed, return null. 
    */
   public Object retrieveSAML2Token(String samlKey) {
        if (!isDatabaseUp) {
            return null;
        }
        try {
            FAMRecord famRec = new FAMRecord (
                SAML2, FAMRecord.READ, samlKey, 0, null, 0, null, null);
           
            FAMRecord retRec = null; //pSession.send(famRec);

            byte[] blob = retRec.getSerializedInternalSessionBlob();
            Object retObj = SessionUtils.decode(blob);
            return retObj;
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
            return null;
        } catch (Exception e) {
            debug.message("AMTokenSAML2Repository.retrieve(): failed retrieving "
                    + "SAML2 object", e);
            return null;
        }
    }

   /**
    * Retrives a list of existing SAML2 object from persistent datastore with
    * secodaryKey
    *
    * @param secKey Secondary Key 
    * @return SAML2 object, if failed, return null. 
    */
   public List retrieveSAML2TokenWithSecondaryKey(String secKey) {
        if (!isDatabaseUp) {
            return null;
        }
        try {
            FAMRecord famRec = new FAMRecord(SAML2,
                FAMRecord.READ_WITH_SEC_KEY, null, 0, secKey, 0, null, null);
           
            FAMRecord retRec = null; // pSession.send(famRec);
            Map map = retRec.getExtraStringAttributes();
            if ((map != null) && (!map.isEmpty())) {
                Vector blobs = (Vector)map.values().iterator().next();

                if ((blobs != null) && (!blobs.isEmpty())) {
                    List list = new ArrayList();
                    for(int i=0; i<blobs.size(); i++) {
                        byte[] blob = (byte[])blobs.get(i);
                        Object obj = SessionUtils.decode(blob);
                        list.add(obj);
                    }
                    return list;
                }
            }
            return null;
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
            return null;
        } catch (Exception e) {
            debug.message("AMTokenSAML2Repository.retrieve(): failed retrieving "
                    + "SAML2 object", e);
            return null;
        }
    }

   /**
    * Deletes the SAML2 object by given primary key from the repository
    * @param samlKey primary key 
    */
   public void deleteSAML2Token(String samlKey)  {
        if (!isDatabaseUp) {
            return;
        }
        try {
            FAMRecord famRec = new FAMRecord (
                SAML2, FAMRecord.DELETE, samlKey, 0, null, 0, null, null);
            FAMRecord retRec = null; // pSession.send(famRec);
        /**
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        **/
        } catch (Exception e) {
            debug.error("AMTokenSAML2Repository.delete(): failed deleting "
                    + "SAML2 object", e);
        }
    }

    /**
     * Deletes expired SAML2 object from the repository
     * @exception Exception When Unable to delete the expired SAML2 object
     */
    public void deleteExpiredSAML2Tokens()  {
        if (!isDatabaseUp) {
            return;
        }
        try {
            long date = System.currentTimeMillis() / 1000;     
            FAMRecord famRec = new FAMRecord (
                 SAML2, FAMRecord.DELETEBYDATE, null,
                 date, null, 0, null, null);
            FAMRecord retRec = null; // pSession.send(famRec);
            /**
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
             **/
        } catch (Exception e) {
            debug.error("AMTokenSAML2Repository.deleteExpired(): failed "
                    + "deleting Expired saml2 object", e);
        }
    }

   /**
    * Saves SAML2 data into the SAML2 Repository
    * @param samlKey primary key 
    * @param samlObj saml object such as Response, IDPSession
    * @param expirationTime expiration time 
    * @param secKey Secondary Key 
    */
    public void saveSAML2Token(String samlKey, Object samlObj, long expirationTime,
        String secKey) {

        if (!isDatabaseUp) {
            return;
        }

        try {
            byte[] blob = SessionUtils.encode(samlObj);
            FAMRecord famRec = new FAMRecord (
                SAML2, FAMRecord.WRITE, samlKey, expirationTime, secKey,
                0, null, blob);
            FAMRecord retRec = null; // pSession.send(famRec);
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        } catch (Exception e) {
            debug.error("AMTokenSAML2Repository.save(): failed "
                    + "to save SAML2 object", e);
        }
    }

    /**
     * This method is invoked to log a message in the following two cases:
     * 
     * (1) the DB is detected down by either the user requests
     * (retrieve/save/delete) or the background checker thread:
     * Log message: HA_DATABASE_UNAVAILABLE (2) the DB is detected
     * available again by the background health checker thread => Log message:
     * HA_DATABASE_BACK_ONLINE
     * 
     * The flag "lastLoggedDBStatusIsUp" is used to avoid logging the same DB
     * status again and again if the status actually doesn't change over time.
     * 
     * Please also note that if the DB is already down in the very beginning
     * when starting the AM instance, there will be no message being logged
     * since at this time the session service is not fully initialized yet
     * therefore no sso token can be generated and used for the logging purpose.
     * Nevertheless, the appropriate logging will be done later when the
     * background thread kicks in.
     * 
     */
    private void logDBStatus() {

        SessionService ss = SessionService.getSessionService();

        if (!isDatabaseUp && lastLoggedDBStatusIsUp) {
            ss.logSystemMessage(LOG_MSG_DB_UNAVAILABLE,
                    java.util.logging.Level.WARNING);
            lastLoggedDBStatusIsUp = false;
        }
        if (isDatabaseUp && !lastLoggedDBStatusIsUp) {
            ss.logSystemMessage(LOG_MSG_DB_BACK_ONLINE,
                    java.util.logging.Level.INFO);
            lastLoggedDBStatusIsUp = true;
        }
    }

    public long getRunPeriod() {
        return runPeriod;
    }
    
    public boolean addElement(Object obj) {
        return false;
    }
    
    public boolean removeElement(Object obj) {
        return false;
    }
    
    public boolean isEmpty() {
        return true;
    }
  
    
    /**
     * Monitoring logic used by background thread This thread is used for both
     * cleanup of expired sessions in the repository and for the Repository health
     * checking. The thread always runs with smallest value of cleanUpPeriod and
     * healthCheckPeriod.
     */
     public void run() {
        String classMethod="DefaultJMQSAML2Repository.run: "; 
        try {

            if (debug.messageEnabled()) {
                debug.message(classMethod + "Cleaning expired SAML2 records");
            }

            /*
             * Clean up is done based on the cleanUpPeriod even though the
             * thread runs based on the runPeriod.
             */
            if (SAML2Utils.isSAML2FailOverEnabled() && (cleanUpValue <= 0)) {
                deleteExpiredSAML2Tokens();
                cleanUpValue = cleanUpPeriod;
            }
            cleanUpValue = cleanUpValue - runPeriod;

            /*
             * HealthChecking is done based on the runPeriod but only when
             * the Database is down.
             */
           if (SAML2Utils.isSAML2FailOverEnabled() && (!isDatabaseUp)) {
                initPersistSession();
                logDBStatus();
            }
        } catch (Exception e) {
            debug.error("AMTokenSAML2Repository.run(): Exception in thread",
                    e);
        }

    }


}
