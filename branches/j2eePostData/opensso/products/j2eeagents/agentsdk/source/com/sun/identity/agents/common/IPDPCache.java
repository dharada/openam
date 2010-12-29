/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.agents.common;

/**
 *
 * @author mad
 */
public interface IPDPCache {
    public void initialize();
    public boolean addEntry(String key, IPDPCacheEntry entry);
    public boolean removeEntry(String key);
    public IPDPCacheEntry getEntry(String key);
}
