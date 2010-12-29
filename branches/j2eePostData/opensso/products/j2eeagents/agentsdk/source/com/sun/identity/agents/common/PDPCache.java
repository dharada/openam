/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.filter.AmFilterModule;
import com.sun.identity.agents.filter.IFilterConfigurationConstants;
import java.util.HashMap;
import java.util.Iterator;
/**
 *
 * @author mad
 */
public class PDPCache implements IPDPCache, IFilterConfigurationConstants {
    public PDPCache(Manager manager) {
        _cache = new HashMap<String, IPDPCacheEntry>();
        _ttl = manager.getConfigurationLong(CONFIG_POSTDATA_PRESERVE_TTL,
                                            DEFAULT_POSTDATA_PRESERVE_TTL);
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
            return _cache.remove(key) != null;
        }
        return false;
    }

    public IPDPCacheEntry getEntry(String key) {
        expireEntries();
        if (_cache.containsKey(key)) {
            return _cache.get(key);
        }
        return null;
    }

    private void expireEntries() {
        if (_expireThread == null) {
            _expireThread = new Thread(new ExpireThread(), "PDPCache Cleaner");
            _expireThread.start();
        }
    }

    private HashMap<String, IPDPCacheEntry> _cache;
    private static Thread _expireThread;
    private static long _ttl;

    private class ExpireThread implements Runnable {
        public void run() {
            long currentTime = System.currentTimeMillis();
            if (AmFilterModule.getModule().isLogMessageEnabled()) {
                AmFilterModule.getModule().logMessage("PDPCache$ExpireThread: " +
                        "Thread started at: " + currentTime);
            }
            for (Iterator<String> i = _cache.keySet().iterator();
                    i.hasNext(); ) {
                String key = i.next();
                IPDPCacheEntry entry = _cache.get(key);
                if (entry.getCreationTime() + _ttl < currentTime) {
                    _cache.remove(key);
                    if (AmFilterModule.getModule().isLogMessageEnabled()) {
                        AmFilterModule.getModule().logMessage("PDPCache$ExpireThread: " +
                                "Entry expired: " + key +
                                ", creation time: " + entry.getCreationTime());
                    }
                }
            }
            _expireThread = null;
        }
    }
}
