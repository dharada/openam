package org.forgerock.openam.console;

import com.iplanet.sso.SSOException;
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
public class ConsoleServiceConfig extends ServerResource {
    @Get("json")
    public Representation get(Variant variant) {
        //UDCAPICache.getInstance();
        JSONObject result = new JSONObject();

        try {
            SSOToken adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            String service = (String) getRequest().getAttributes().get("service");
            String scope = (String) getRequest().getAttributes().get("scope");
            if (scope == null) scope = "all";

            if (service != null)  {
                ServiceManager sm = new ServiceManager(adminToken);
                ServiceConfigManager scm = new ServiceConfigManager( service, adminToken) ;

                result = scmAsJSON(scm,scope) ;
            }

        } catch (Exception e)       {

        }
        return new JsonRepresentation(result);
    }


    JSONObject scmAsJSON(ServiceConfigManager scm,String scope)    {
        ServiceConfig sc;
        JSONObject   jo = new JSONObject();

        try {
            if (scope.equals("all"))  {
                jo.accumulate("version",scm.getVersion());
                jo.accumulate("Instances",new JSONArray(scm.getInstanceNames()));
                jo.accumulate("GroupName",new JSONArray(scm.getGroupNames()));

                ServiceConfig gc = scm.getGlobalConfig(null);
                if (gc != null) jo.accumulate("GlobalConfig",scAsJSON(gc));
                ServiceConfig oc = scm.getOrganizationConfig("/",null);
                if (oc != null) jo.accumulate("GlobalConfig",scAsJSON(oc)) ;
            };
            if (scope.equals("global") )  {
                ServiceConfig gc = scm.getGlobalConfig(null);
                if (gc != null)  jo.accumulate("config",scAsJSON(gc));
            };
            if (scope.equals("org" )) {
                ServiceConfig oc = scm.getOrganizationConfig("/",null);
                if (oc != null) jo.accumulate("config",scAsJSON(oc)) ;
            }


        } catch (SMSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSONException e)  {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SSOException e)  {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    return jo;
    }

    JSONObject  scAsJSON(ServiceConfig sc)   {
        JSONObject jo = new JSONObject();
        JSONObject jsub = new JSONObject();

        try {
            jo.accumulate("Attributes",new JSONObject(sc.getAttributes()));

            for (Iterator items = sc.getSubConfigNames().iterator(); items.hasNext();) {
                String attrName = (String) items.next();
                jsub.accumulate(attrName,scAsJSON(sc.getSubConfig(attrName)));
            }
            jo.accumulate("SubConfigs", jsub);

        } catch (SMSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SSOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return jo;
    }
}