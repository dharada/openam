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
public class ConsoleServiceSchema extends ServerResource {
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
                ServiceSchemaManager ssm = new ServiceSchemaManager( service, adminToken) ;

                result= ssmAsJSON(ssm,scope) ;
            }

        } catch (Exception e)       {

        }
        return new JsonRepresentation(result);
    }


    JSONObject ssmAsJSON(ServiceSchemaManager ssm,String scope)    {
        ServiceSchema ss;
        JSONObject   jo = new JSONObject();


        try {
            if (scope.equals("all"))  {
                jo.accumulate("version",ssm.getVersion());
                jo.accumulate("I18NFile",ssm.getI18NFileName());
                jo.accumulate("I18NJar",ssm.getI18NJarURL());
                jo.accumulate("I18NKey",ssm.getI18NKey());
                jo.accumulate("ServiceHierarchy",ssm.getServiceHierarchy());
                jo.accumulate("PropViewBean",ssm.getPropertiesViewBeanURL());
            };
            if ((scope.equals("global")) || (scope.equals("all")))  {
                if ((ss = ssm.getSchema(SchemaType.GLOBAL)) != null) {
                    jo.accumulate("Schema", ssAsJSON(ss)) ;
                }
            }
            if ((scope.equals("org")) || (scope.equals("all")))  {
                if ((ss = ssm.getSchema(SchemaType.ORGANIZATION)) != null) {
                    jo.accumulate("Schema", ssAsJSON(ss)) ;
                }
            }
            if ((scope.equals("dynamic")) || (scope.equals("all")))  {
                if ((ss = ssm.getSchema(SchemaType.DYNAMIC)) != null) {
                    jo.accumulate("Schema", ssAsJSON(ss)) ;
                }
            }
            if ((scope.equals("user")) || (scope.equals("all")))  {
                if ((ss = ssm.getSchema(SchemaType.USER)) != null) {
                    jo.accumulate("Schema", ssAsJSON(ss)) ;
                }
            }
            if ((scope.equals("policy")) || (scope.equals("all")))  {
                if ((ss = ssm.getSchema(SchemaType.POLICY)) != null) {
                    jo.accumulate("Schema", ssAsJSON(ss)) ;
                }
            }
        } catch (SMSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSONException e)  {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return jo;
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
                jo.accumulate("SubSchemas",ssjo);
            }
        } catch (SMSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return jo;
    }
}