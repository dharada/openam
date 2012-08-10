package org.forgerock.openam.console;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.resource.Resource;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import java.security.AccessController;
import java.util.*;

import org.restlet.ext.json.JsonRepresentation ;

/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 7/28/12
 * Time: 8:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleServices extends ServerResource {
    @Get("json")
    public Representation get(Variant variant) {
        //UDCAPICache.getInstance();
        JSONObject result = new JSONObject();

        try {
            SSOToken adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            String service = (String) getRequest().getAttributes().get("service");

            ServiceManager sm = new ServiceManager(adminToken);

            Set sNames = sm.getServiceNames();

            for (Iterator items = sNames.iterator(); items.hasNext();) {
                String svcName = (String) items.next();
                JSONObject jo = new JSONObject();
                jo.accumulate("name",svcName);
                result.accumulate("serviceName",jo);
            }

            ResourceBundle

        } catch (Exception e)       {

        }
        return new JsonRepresentation(result);
    }
}