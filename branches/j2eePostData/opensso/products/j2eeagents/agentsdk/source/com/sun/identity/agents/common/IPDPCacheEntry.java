/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.agents.common;

import java.util.Map;

/**
 *
 * @author mad
 */
public interface IPDPCacheEntry {
    public void initialize();
    public long getCreationTime();
    public void setCreationTime(long creationTime);
    public String getOriginalURL();
    public void setOriginalURL(String originalURL);
    public Map getParameterMap();
    public void setParameterMap(Map parameterMap);
}
