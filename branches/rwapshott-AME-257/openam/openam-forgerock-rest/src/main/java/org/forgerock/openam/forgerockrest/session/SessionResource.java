/**
 * Copyright 2013 ForgeRock, Inc.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */
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

/**
 * Represents Sessions that can queried via a REST interface.
 *
 * Currently describe three different entrypoints for this Resource, useful when querying
 * Session Information:
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

    public static final String HEADER_USER_ID = "User Id";
    public static final String HEADER_TIME_REMAINING = "Time Remaining";
    private SessionQueryManager queryManager;

    public SessionResource(SessionQueryManager queryManager) {
        this.queryManager = queryManager;
    }

    /**
     * Applies the routing to the Router that this class supports.
     *
     * @param ocm Configuration required for organisation name.
     * @param router Router to apply changes to.
     */
    public static void applyRouting(OrganizationConfigManager ocm, Router router) {
        String orgName = ocm.getOrganizationName();
        if (!orgName.endsWith("/")) {
            orgName += "/";
        }

        SessionQueryManager sessionQueryManager = new SessionQueryManager(new SessionQueryFactory());
        router.addRoute(RoutingMode.STARTS_WITH, orgName + "sessions", new SessionResource(sessionQueryManager));
    }

    /**
     * Returns a collection of all Server ID that are known to the OpenAM instance.
     *
     *  @return A non null, possibly empty collection of server ids.
     */
    public Collection<String> getAllServerIds() {
        try {
            return WebtopNaming.getAllServerIDs();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot recover from this error", e);
        }
    }

    /**
     * Currently unimplemented.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Not implemented for this Resource"));
    }


    /**
     * Currently unimplemented.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Not implemented for this Resource"));
    }

    /**
     * Currently unimplemented method.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(ResourceException.getException(
                0,
                "QueryCollection is not yet implemented.",
                "Unimplmeneted",
                null));
    }

    /**
     * Perform a query against the defined servers.
     *
     * This method will resolve the id, provided by the caller, and use this to perfrom the appropriate
     * query and return these results in an initially hard coded formnat..
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
            table.add(Arrays.asList(new String[]{HEADER_USER_ID, HEADER_TIME_REMAINING}));

            Collection<SessionInfo> sessions = null;
            if (id.equals(KEYWORD_ALL)) {
                sessions = generateAllSessions();
            } else {
                sessions = generateNamedServerSession(id);
            }

            for (SessionInfo session : sessions) {

                // TODO The format of the output likely to change in the future.

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
    }

    /**
     * @param serverId Server to query.
     * @return A non null collection of SessionInfos from the named server.
     */
    private Collection<SessionInfo> generateNamedServerSession(String serverId) {
        List<String> serverList = Arrays.asList(new String[]{serverId});
        Collection<SessionInfo> sessions = queryManager.getAllSessions(serverList);
        return sessions;
    }


    /**
     * @return A non null collection of SessionInfo instances queried across all servers.
     */
    private Collection<SessionInfo> generateAllSessions() {
        Collection<SessionInfo> sessions = queryManager.getAllSessions(getAllServerIds());
        return sessions;
    }


    /**
     * @return Returns a JSON Resource which defines the available servers.
     */
    private Collection<String> generateListServers() {
        return getAllServerIds();
    }

    /**
     * Internal function for converting time in seconds to minutes.
     *
     * @param timeleft Non null string value of time in seconds.
     * @return The parsed time.
     */
    private static int convertTimeLeft(String timeleft) {
        float seconds = Long.parseLong(timeleft);
        float mins = seconds / 60;
        return Math.round(mins);
    }
}
