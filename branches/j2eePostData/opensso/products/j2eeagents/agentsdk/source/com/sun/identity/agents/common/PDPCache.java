/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.Manager;
import java.util.HashMap;
/**
 *
 * @author mad
 */
public class PDPCache implements IPDPCache{
    public PDPCache(Manager manager) {
        _cache = new HashMap<String, IPDPCacheEntry>();
    }

    public void initialize() {
    }

    public boolean addEntry(String key, IPDPCacheEntry entry) {
        return _cache.put(key, entry) != null;
    }

    public boolean removeEntry(String key) {
        if (_cache.containsKey(key)) {
            return _cache.remove(key) != null;
        }
        return false;
    }

    public IPDPCacheEntry getEntry(String key) {
        if (_cache.containsKey(key)) {
            return _cache.get(key);
        }
        return null;
    }

    private HashMap<String, IPDPCacheEntry> _cache;
}
