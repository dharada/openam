package org.forgerock.openam.forgerockrest.session.query;

import com.iplanet.dpro.session.Session;

import java.util.Collection;

/**
 * Represents an implementation which can perform a query of Sessions against a server.
 *
 * Note: If there is any complexity surrounding performing a query, then it can be wrapped in this call and
 * abstracted from the caller.
 *
 * NB Can be easily expanded with new functions like 'Sessions for a user' and 'Session count'
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionQueryManager {

    private SessionQueryFactory queryFactory;

    public SessionQueryManager(SessionQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Collection<Session> getAllSessions(String serverId) {
        return null;
    }
}
