package org.forgerock.openam.forgerockrest.session;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.sm.OrganizationConfigManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openam.forgerockrest.ReadOnlyResource;
import org.forgerock.openam.forgerockrest.session.query.impl.RemoteSessionQuery;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * Represents Sessions on each or all servers as a read only resource.
 */
public class SessionResource extends ReadOnlyResource {

    private Collection<String> serverIds;

    public SessionResource(Collection<String> serverIds) {
        this.serverIds = serverIds;
    }

    /**
     * Applies the routing to the Router that this class supports.
     *
     * @param ocm Configuration required for organisation name.
     *
     * @param router Router to apply changes to.
     */
    public static void applyRouting(OrganizationConfigManager ocm, Router router) {
        String orgName = ocm.getOrganizationName();
        if (!orgName.endsWith("/")) {
            orgName += "/";
        }

        // NB one time registration of Servers limits the Resource to requiring a restart to pick up new servers.

        // Register top level
        Collection<String> serverIds = getServerIds();
        router.addRoute(RoutingMode.EQUALS, orgName + "sessions", new SessionResource(serverIds));

        // NB We could easily add more routing at this point for each server which would allow the user
        // to query just the sessions on a server.
    }

    /**
     * Returns a collection of all Server ID that are known to the OpenAM instance.
     * @return A non null, possibly empty collection of server ids.
     */
    public static Collection<String> getServerIds() {
        Vector<String> iDs;
        try {
            iDs = WebtopNaming.getAllServerIDs();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot recover from this error", e);
        }
        return iDs;
    }


    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleResource(new Resource("ID", "0", new JsonValue("Query Collection")));
    }

    /**
     * Perform a query against the defined servers.
     *
     * {@inheritDoc}
     */
    public void readInstance(ServerContext context, String id, ReadRequest request, ResultHandler<Resource> handler) {

        // For each server, perform the query
        // NB, collecting up all the sessions from all servers will not be performant...
        // Does the framework have any way of pagenating the results?
        // Does the query framework lend itsself to querying in any way?

        // Should we bypass this anyway and use SFO?...

        List<SessionInfo> sessions = new LinkedList<SessionInfo>();

        for (String serverId : serverIds) {
            SSOToken adminToken = RemoteSessionQuery.getAdminToken();


            SessionRequest sreq = new SessionRequest(SessionRequest.GetValidSessions, adminToken.getTokenID().toString(), false);
            URL serviceURL;
            try {
                serviceURL = Session.getSessionServiceURL(serverId);
            } catch (SessionException e) {
                throw new IllegalStateException(e);
            }

            SessionResponse sres = null;
            try {
                sres = getSessionResponseWithoutRetry(serviceURL, sreq);
            } catch (SessionException e) {
                throw new IllegalStateException(e);
            }

            sessions.addAll(sres.getSessionInfo());

        }
        Resource resource = new Resource("ID", "Revision", new JsonValue(sessions.size()));
        handler.handleResult(resource);
    }

    private SessionResponse getSessionResponseWithoutRetry(URL svcurl,
                                                           SessionRequest sreq) throws SessionException {
        SessionResponse sres = null;
        Object context = RestrictedTokenContext.getCurrent();

        SSOToken appSSOToken = null;

        // This feels like a code smell to me.
        // Why can't the call coming from an Auth Module get a Context ready before hand?
//        if (!isServerMode() && !(this.sessionID.getComingFromAuth())) {
//            appSSOToken = (SSOToken) AccessController.doPrivileged(
//                    AdminTokenAction.getInstance());
//            createContext(appSSOToken);
//        }

        try {
            if (context != null) {
                sreq.setRequester(RestrictedTokenContext.marshal(context));
            }
            sres = Session.sendPLLRequest(svcurl, sreq);
        } catch (Exception e) {
            throw new SessionException(e);
        }

        return sres;
    }
}
