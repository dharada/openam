package org.forgerock.openam.forgerockrest.session.query;

import com.iplanet.dpro.session.Session;

import java.util.Collection;

/**
 * Defines the ability to query a Server and return Sessions from that server.
 *
 * @author robert.wapshott@forgerock.com
 */
public interface SessionQueryType {
    public Collection<Session> getAllSessions();
}
