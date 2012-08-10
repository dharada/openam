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
public class ConsoleServiceGlobalSchema extends ServerResource {
    @Get("json")
    public Representation get(Variant variant) {
        //UDCAPICache.getInstance();
        JSONObject result = new JSONObject();

        try {
            SSOToken adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            String service = (String) getRequest().getAttributes().get("service");

            if (service != null)  {
                ServiceManager sm = new ServiceManager(adminToken);
                ServiceSchema ss;

                ServiceSchemaManager ssm = new ServiceSchemaManager( service, adminToken) ;

                if ((ss = ssm.getSchema(SchemaType.GLOBAL)) != null) {
                    result.accumulate("Schema", ssAsJSON(ss)) ;
                }
            }

        } catch (Exception e)       {

        }
        return new JsonRepresentation(result);
    }

    JSONObject  ssAsJSON(ServiceSchema ss)   {
        JSONObject jo = new JSONObject();

        try {
            if (ss.getName() != null) {
                jo.accumulate("SchemaName", ss.getName());
            }
            for (Iterator items = ss.getAttributeSchemaNames().iterator(); items.hasNext();) {
                String attrName = (String) items.next();
                jo.accumulate(attrName, ss.getAttributeSchema(attrName).toJSON());
            }

            Set ssNames = ss.getSubSchemaNames();
            // Sub-schemas
            if (ssNames.size() > 0) {
                JSONObject ssjo = new JSONObject();
                Iterator items = ssNames.iterator();
                while (items.hasNext()) {
                    JSONObject ssAttr = new JSONObject();
                    String sSchemaName = (String)items.next();
                    ServiceSchema sSchema = ss.getSubSchema(sSchemaName) ;
                    ssjo.accumulate(sSchemaName,ssAsJSON(sSchema));
                }
                jo.accumulate("Schema",ssjo);
            }
        } catch (SMSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return jo;
    }
}