package org.forgerock.openam.forgerockrest.session.query.impl;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryType;

import java.net.URL;
import java.security.AccessController;
import java.util.Collection;
import java.util.Map;

/**
 * Performs a query against a remote server.
 */
public class RemoteSessionQuery implements SessionQueryType {
    private String serverId;
    private static Debug debug = SessionService.sessionDebug;

    public RemoteSessionQuery(String serverId) {
        this.serverId = serverId;
    }

    public Collection<SessionInfo> getAllSessions() {
        URL svcurl = null;
        try {
            svcurl = Session.getSessionServiceURL(serverId);
            String sid = getAdminToken().getTokenID().toString();

            SessionRequest sreq = new SessionRequest(SessionRequest.GetValidSessions, sid, false);
            SessionResponse sres = getSessionResponse(svcurl, sreq);

            Map sessionsMap = sres.getSessionsForGivenUUID();
            debug.message(sessionsMap.size() + " size of session map.");

        } catch (SessionException e) {
            debug.warning("Failed to fetch sessions from " + serverId, e);
            return null;
        }
        return null;
    }

    /**
     * Fetches the admin token by querying the SessionService.
     *
     * @return Non null SSOToken for the admin user.
     *
     * @throws IllegalStateException
     *
     * TODO Do something sensible with this
     */
    public static SSOToken getAdminToken() throws IllegalStateException {
        SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        if (token == null) {
            throw new IllegalStateException("Failed to get the admin token.");
        }
        return token;
    }

    private static SessionResponse getSessionResponse(URL svcurl,
                                                      SessionRequest sreq) throws SessionException {
        try {
            Object context = RestrictedTokenContext.getCurrent();
            if (context != null) {
                sreq.setRequester(RestrictedTokenContext.marshal(context));
            }

            SessionResponse sres = Session.sendPLLRequest(svcurl, sreq);
            if (sres.getException() != null) {
                throw new SessionException(sres.getException());
            }
            return sres;
        } catch (SessionException se) {
            throw se;
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }
}
