package org.forgerock.openam.forgerockrest.session;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.sm.OrganizationConfigManager;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openam.forgerockrest.ReadOnlyResource;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryFactory;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryManager;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * Represents Sessions that can queried via a REST interface.
 *
 * Currently describe three different ways of accessing the Session information:
 *
 * <ul>
 *     <li>All - All sessions across all servers known to OpenAM.</li>
 *     <li>Servers - Lists all servers that are known to OpenAM.</li>
 *     <li>[server-id] - Lists the servers for that server instance.</li>
 * </ul>
 *
 * This resources acts as a read only resource for the moment.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionResource extends ReadOnlyResource {

    public static final String KEYWORD_ALL = "all";
    public static final String KEYWORD_LIST = "list";

    // TODO - Convert this to use the SessionQeryManager to complete the query.

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

        router.addRoute(RoutingMode.STARTS_WITH, orgName + "sessions", new SessionResource());
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

        Resource resource = null;
        if (id.equals(KEYWORD_LIST)) {
            Collection<String> servers = generateListServers();
            resource = new Resource(KEYWORD_LIST, "0", new JsonValue(servers));
        } else {
            List<List<String[]>> table = new LinkedList<List<String[]>>();
            table.add(Arrays.asList(new String[]{"User Id", "Time Remaining"}));

            Collection<SessionInfo> sessions = null;
            if (id.equals(KEYWORD_ALL)) {
                sessions = generateAllSessions();
            } else {
                sessions = generateNamedServerSession(id);
            }

            for (SessionInfo session : sessions) {

                int timeleft = convertTimeLeft(session.timeleft);
                String username = (String) session.properties.get("UserId");

                table.add(Arrays.asList(new String[]{ username, Integer.toString(timeleft) }));
            }

            resource = new Resource("Sessions", "0", new JsonValue(table));
        }

        if (resource == null) {
            throw new IllegalStateException("Resource cannot be undefined.");
        }

        handler.handleResult(resource);




        // For each server, perform the query
        // NB, collecting up all the sessions from all servers will not be performant...
        // Does the framework have any way of pagenating the results?
        // Does the query framework lend itsself to querying in any way?

        // Should we bypass this anyway and use SFO?...

//        List<SessionInfo> sessions = new LinkedList<SessionInfo>();
//
//        for (String serverId : serverIds) {
//            SSOToken adminToken = RemoteSessionQuery.getAdminToken();
//
//
//            SessionRequest sreq = new SessionRequest(SessionRequest.GetValidSessions, adminToken.getTokenID().toString(), false);
//            URL serviceURL;
//            try {
//                serviceURL = Session.getSessionServiceURL(serverId);
//            } catch (SessionException e) {
//                throw new IllegalStateException(e);
//            }
//
//            SessionResponse sres = null;
//            try {
//                sres = getSessionResponseWithoutRetry(serviceURL, sreq);
//            } catch (SessionException e) {
//                throw new IllegalStateException(e);
//            }
//
//            sessions.addAll(sres.getSessionInfo());
//
//        }
//        Resource resource = new Resource("ID", "Revision", new JsonValue(sessions.size()));
//        handler.handleResult(resource);
    }

    private static int convertTimeLeft(String timeleft) {
        float seconds = Long.parseLong(timeleft);
        float mins = seconds / 60;
        return Math.round(mins);
    }

    private Collection<SessionInfo> generateNamedServerSession(String serverId) {
        List<String> serverList = Arrays.asList(new String[]{serverId});
        SessionQueryManager queryManager = new SessionQueryManager(new SessionQueryFactory(), serverList);
        Collection<SessionInfo> sessions = queryManager.getAllSessions();
        return sessions;
    }

    private Collection<SessionInfo> generateAllSessions() {
        SessionQueryManager queryManager = new SessionQueryManager(new SessionQueryFactory(), getServerIds());
        Collection<SessionInfo> sessions = queryManager.getAllSessions();
        return sessions;
    }


    /**
     * @return Returns a JSON Resource which defines the available servers.
     */
    private Collection<String> generateListServers() {
        return getServerIds();
    }
}
