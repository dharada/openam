/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
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

package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.filter.AmFilterModule;
import com.sun.identity.agents.filter.IFilterConfigurationConstants;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/**
 *
 * @author mad
 */
public class PDPCache implements IPDPCache, IFilterConfigurationConstants {
    public PDPCache(Manager manager) {
        _cache = Collections.synchronizedMap(new HashMap<String, IPDPCacheEntry>());
        _ttl = manager.getConfigurationLong(CONFIG_POSTDATA_PRESERVE_TTL,
                                            DEFAULT_POSTDATA_PRESERVE_TTL);
        _cleanUpInterval = manager.getConfigurationLong(
                            CONFIG_POSTDATA_PRESERVE_CACHE_CLEANUP_INTERVAL,
                            DEFAULT_POSTDATA_PRESERVE_CACHE_CLEANUP_INTERVAL);
        _lastCleaningUp = System.currentTimeMillis();
    }

    public void initialize() {
   }

    public boolean addEntry(String key, IPDPCacheEntry entry) {
        expireEntries();
        return _cache.put(key, entry) != null;
    }

    public boolean removeEntry(String key) {
        expireEntries();
        if (_cache.containsKey(key)) {
           _cache.remove(key);
           return true;
        }
        return false;
    }

    public IPDPCacheEntry getEntry(String key) {
        IPDPCacheEntry entry = _cache.get(key);
        expireEntries();
        return entry;
    }

    public void expireEntries() {
        long now = System.currentTimeMillis();
        if (_lastCleaningUp + _cleanUpInterval < now) {
            if (_expireThread == null || !_expireThread.isAlive()) {
                _expireThread = new Thread(new ExpireThread(), "PDPCache Cleaner");
                _expireThread.start();
                _lastCleaningUp = now;
            }
        }
    }

    private Map<String, IPDPCacheEntry> _cache;
    private static Thread _expireThread;
    private static long _lastCleaningUp;
    private static long _cleanUpInterval;
    private static long _ttl;

    private class ExpireThread implements Runnable {
        public void run() {
            long currentTime = System.currentTimeMillis();
            if (AmFilterModule.getModule().isLogMessageEnabled()) {
                AmFilterModule.getModule().logMessage("PDPCache$ExpireThread: " +
                        "Thread started at: " + currentTime);
            }
            synchronized(_cache) {
                for (Iterator<String> i = _cache.keySet().iterator();
                        i.hasNext(); ) {
                    String key = i.next();
                    IPDPCacheEntry entry = _cache.get(key);
                    if (entry.getCreationTime() + _ttl < currentTime) {
                        i.remove();
                        if (AmFilterModule.getModule().isLogMessageEnabled()) {
                            AmFilterModule.getModule().logMessage("PDPCache$ExpireThread: " +
                                    "Entry expired: " + key +
                                    ", creation time: " + entry.getCreationTime());
                        }
                    }
                }
            }
        }
    }
}
