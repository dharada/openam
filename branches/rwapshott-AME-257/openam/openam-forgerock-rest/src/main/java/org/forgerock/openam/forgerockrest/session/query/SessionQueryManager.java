package org.forgerock.openam.forgerockrest.session.query;

import com.iplanet.dpro.session.share.SessionInfo;

import java.util.Collection;

/**
 * Provides the function to query a collection of OpenAM server instances. Uses the SessionQueryFactory to
 * determine the most appropriate mechanism for performing the query.
 *
 * @author robert.wapshott@forgerock.com
 *
 *
 *
 * Represents an implementation which can perform a query of Sessions against a server.
 *
 * Note: If there is any complexity surrounding performing a query, then it can be wrapped in this call and
 * abstracted from the caller.
 *
 * NB Can be easily expanded with new functions like 'Sessions for a user' and 'Session count'
 *
 */
public class SessionQueryManager {
    private SessionQueryFactory queryFactory;
    private Collection<String> serverIds;
    // Refactor this so a Session Query is initialised with a collection of servers and performs its
    // query, probably with no arguments.

    // Return type is probably more important. Do we want Session Info's? Or some other TO?

    // Then Session Resource can be initialised with a collection of Session Queries and select the
    // appropriate one based on the parameter that is passed in.

    // probaly
    // session/all
    // session/servers
    // sesssion/<server-id>

    /// because we must have something after the routing location ("session")

    /**
     * Intialise the SessionQueryManager and provide the OpenAM server ids that it should apply to.
     *
     * @param queryFactory Non null instance.
     *
     * @param serverIds One or more server id's. Typically this value can be generated using
     *                  {@link com.iplanet.services.naming.WebtopNaming#getAllServerIDs()} which will provide all
     *                  server id's known to OpenAM.
     */
    public SessionQueryManager(SessionQueryFactory queryFactory, Collection<String> serverIds) {
        this.queryFactory = queryFactory;
        this.serverIds = serverIds;
    }

    /**
     * Query all servers allocated to this SessionQueryManager for their Sessions.
     *
     * @return Returns all sessions across all servers.
     */
    public Collection<SessionInfo> getAllSessions() {
        // impl note, this could be a Map of Server -> Sessions

        return null;
    }
}
