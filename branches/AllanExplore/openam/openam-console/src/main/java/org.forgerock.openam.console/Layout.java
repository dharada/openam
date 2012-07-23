package org.forgerock.openam.console;

/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 7/22/12
 * Time: 7:28 PM
 * To change this template use File | Settings | File Templates.
 */

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.Request;
import org.restlet.Response;

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ServerResource;

/**
 * Utility Resource to view the various strings
 * mapped in the Persistence.
 */
public class Layout extends ServerResource {

    /**
     * The map requested by the client application
     */
    String mapName;


    /**
     * Do not allow post to this resource.
     *
     * @return -- always false for this resource.
     */
    public boolean allowPost() {
        return false;
    }

    /**
     * Collect the values for a particular mapping and return
     * a formatted string.  By inputting "all" into the request,
     * it returns a list of the mappings available. When this
     * service returns, substituting "all" for one of the
     * returned values will return the values for that mapping key
     *
     * @param variant -- The variant describing the return mappings.
     *
     * @return -- a formatted string with the map values as a list.
     */

    @Get
    public Representation represent(Variant variant) {
        //UDCAPICache.getInstance();

        return new StringRepresentation("None Found", MediaType.TEXT_PLAIN);
    }
}