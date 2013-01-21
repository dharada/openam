package org.forgerock.openam.forgerockrest.session.query;

import com.iplanet.dpro.session.share.SessionInfo;

import java.util.Collection;

/**
 * Defines the ability to query a Server and return Session based information from that server.
 *
 * This interface can easily be expanded to support more queries.
 *
 * @author robert.wapshott@forgerock.com
 */
public interface SessionQueryType {
    /**
     * Query a server and return all the Sessions that are stored on the server.
     *
     * @return Non null but possibly empty collection of Sessions.
     */
    public Collection<SessionInfo> getAllSessions();
}
