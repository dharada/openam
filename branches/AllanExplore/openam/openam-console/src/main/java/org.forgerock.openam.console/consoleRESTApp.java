package org.forgerock.openam.console;

/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 7/22/12
 * Time: 6:07 PM
 * To change this template use File | Settings | File Templates.
 */


import com.sun.identity.sm.ServiceSchema;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.data.MediaType;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Directory;
import org.restlet.resource.Get;
import org.forgerock.openam.console.Layout;


/**
 * The Application Root Class.  Maps all of the resources.
 */
public class consoleRESTApp extends Application {

    /**
     * Creates a new DemoApplication object.
     */
    public consoleRESTApp() {
        //empty
    }

    /**
     * Public Constructor to create an instance of DemoApplication.
     *
     * @param parentContext - the org.restlet.Context instance
     */
    public consoleRESTApp(Context parentContext) {
        super(parentContext);
    }

    /**
     * The Restlet instance that will call the correct resource
     * depending up on URL mapped to it.
     *
     * @return -- The resource Restlet mapped to the URL.
     */
    @Override
    public Restlet createInboundRoot() {
        Router router = new Router(getContext());

        router.attach("/services/",ConsoleServices.class);  // returns a list of services
        router.attach("/orgs/",ConsoleOrgs.class);
        router.attach("/services/{service}",ConsoleServices.class) ;
        router.attach("/services/{service}/schema",ConsoleServiceSchema.class) ;
        router.attach("/services/{service}/config",ConsoleServiceConfig.class) ;
        router.attach("/services/{service}/globalSchema",ConsoleServiceGlobalSchema.class) ;

        return router;
    }
}

