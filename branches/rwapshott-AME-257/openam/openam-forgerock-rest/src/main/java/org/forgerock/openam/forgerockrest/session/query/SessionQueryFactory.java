package org.forgerock.openam.forgerockrest.session.query;

import org.forgerock.openam.forgerockrest.session.query.impl.RemoteSessionQuery;

/**
 * SessionQueryFactory provides a means of generating SessionQueryTypes based on the server id that is provided.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionQueryFactory {
    /**
     * Implementation is currently hard-coded to return the RemoteSessionQuery.
     *
     * @param serverId Non null server id.
     * @return A non null SessionQueryType based on the id.
     */
    public SessionQueryType getSessionQueryType(String serverId) {
        return new RemoteSessionQuery(serverId);
    }
}
