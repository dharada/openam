/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.Manager;
import java.util.Map;

/**
 *
 * @author mad
 */
public class PDPCacheEntry implements IPDPCacheEntry {
    public PDPCacheEntry(Manager manager) {

    }
    
    public PDPCacheEntry(String originalURL,
                         Map parameterMap,
                         long creationTime) {
        _originalURL = originalURL;
        _parameterMap = parameterMap;
        _creationTime = creationTime;
    }

    public void initialize() {
    }

    public long getCreationTime() {
        return _creationTime;
    }

    public void setCreationTime(long _creationTime) {
        this._creationTime = _creationTime;
    }

    public String getOriginalURL() {
        return _originalURL;
    }

    public void setOriginalURL(String _originalURL) {
        this._originalURL = _originalURL;
    }

    public Map getParameterMap() {
        return _parameterMap;
    }

    public void setParameterMap(Map _parameterMap) {
        this._parameterMap = _parameterMap;
    }

    private String _originalURL;
    private Map _parameterMap;
    private long _creationTime;
}
