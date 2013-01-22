package org.forgerock.openam.forgerockrest.session.query;

import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.shared.debug.Debug;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides the ability to query a collection of OpenAM servers for Session information. Uses the
 * SessionQueryFactory to determine the most appropriate mechanism for performing the query and handles any
 * complexity around querying Sessions.
 *
 * This manager should easily be expanded to support new functions like 'Session Count' or 'Get Sessions for User'.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionQueryManager {

    private static Debug debug = Debug.getInstance("frRest");

    private SessionQueryFactory queryFactory;
    private Collection<String> serverIds;

    /**
     * Intialise the SessionQueryManager and provide the OpenAM server ids that it should apply to.
     *
     * @param queryFactory Non null instance.
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

        List<SessionInfo> sessions = new LinkedList<SessionInfo>();

        for (String server : serverIds) {
            SessionQueryType queryType = queryFactory.getSessionQueryType(server);

            Collection<SessionInfo> queriedSessions = queryType.getAllSessions();

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        "SessionQueryManager#getAllSessions() :: Queried {0} from: {1}",
                        queriedSessions.size(),
                        server));
            }

            sessions.addAll(queriedSessions);
        }

        return sessions;
    }
}
