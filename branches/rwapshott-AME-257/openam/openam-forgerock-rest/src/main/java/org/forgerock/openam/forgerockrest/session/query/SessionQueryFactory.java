package org.forgerock.openam.forgerockrest.session.query;

import org.forgerock.openam.forgerockrest.session.query.impl.RemoteSessionQuery;

/**
 * SessionQueryFactory provides a means of generating SessionQueryTypes based on the server id that is provided.
 * The factory pattern is also quite suitable for mocking.
 */
public class SessionQueryFactory {
    public SessionQueryType getSessionQueryType(String serverId) {
        return new RemoteSessionQuery(serverId);
    }
}
