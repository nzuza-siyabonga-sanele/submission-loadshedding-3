package wethinkcode.web;

import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class Routes {

    public static final String HOME_PAGE = "/";
    public static final String PROVINCE_ACTION ="/province.action";
    public static final String TOWN_ACTION = "/town.action";

    /**
     * The configure function is a static method that takes a WebService object as a parameter. It then calls the routes
     * method on the WebService object, passing in a lambda expression that defines the routes for the application
     *
     * @param service The WebService object that is passed to the configure method.
     */
    public static void configure( Javalin service){

        service.routes( () -> {
            get( HOME_PAGE, WebServiceController.view );
            post( PROVINCE_ACTION, WebServiceController.viewProvinces);
            post( TOWN_ACTION, WebServiceController.viewTown);
        });

    }
}