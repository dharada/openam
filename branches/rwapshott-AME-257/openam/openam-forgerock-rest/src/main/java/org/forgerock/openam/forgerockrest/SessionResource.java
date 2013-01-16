package org.forgerock.openam.forgerockrest;

import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.sm.OrganizationConfigManager;
import org.forgerock.json.resource.*;

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

    public static Collection<Router> getRouters(OrganizationConfigManager ocm, Router router) {
        String orgName = ocm.getOrganizationName();
        if (!orgName.endsWith("/")) {
            orgName += "/";
        }

        // NB one time registration of Servers limits the Resource to requiring a restart to pick up new servers.

        List<Router> routers = new LinkedList<Router>();
        // Register top level
        router.addRoute(RoutingMode.EQUALS, orgName + "sessions", new SessionResource(getServerIds()));

        // Considering allowing the user to refine the sessions query to each server.
        // We also need to expose the available servers via JSON to assist with the user
        // Querying the correct location.
    }

    /**
     * Returns a collection of all Server ID that are known to the OpenAM instance.
     * @return A non null, possibly empty collection of server ids.
     */
    private static Collection<String> getServerIds() {
        Vector<String> iDs;
        try {
            iDs = WebtopNaming.getAllServerIDs();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot recover from this error", e);
        }
        return iDs;
    }


    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        //To change body of implemented methods use File | Settings | File Templates.
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



        SessionRequest sreq = new SessionRequest(SessionRequest.GetValidSessions, sessionID.toString(), false);

        if (pattern != null) {
            sreq.setPattern(pattern);
        }

        SessionResponse sres = getSessionResponseWithoutRetry(svcurl, sreq);
        infos = sres.getSessionInfo();
        status[0] = sres.getStatus();
    }
}
