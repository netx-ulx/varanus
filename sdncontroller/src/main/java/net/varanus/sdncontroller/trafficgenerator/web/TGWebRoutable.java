package net.varanus.sdncontroller.trafficgenerator.web;


import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;


/**
 * 
 */
public final class TGWebRoutable implements RestletRoutable
{
    @Override
    public Restlet getRestlet( Context context )
    {
        Router router = new Router(context);
        router.attach("/start", StartTrafficResource.class);
        router.attach("/stop", StopTrafficResource.class);
        return router;
    }

    @Override
    public String basePath()
    {
        return "/wm/varanus/trafficgenerator";
    }
}
