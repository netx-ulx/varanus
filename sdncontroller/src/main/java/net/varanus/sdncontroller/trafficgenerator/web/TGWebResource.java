package net.varanus.sdncontroller.trafficgenerator.web;


import org.restlet.resource.ServerResource;

import net.varanus.sdncontroller.trafficgenerator.ITrafficGeneratorService;


/**
 * 
 */
abstract class TGWebResource extends ServerResource
{
    protected final ITrafficGeneratorService getTGService()
    {
        return (ITrafficGeneratorService)getContext().getAttributes()
            .get(ITrafficGeneratorService.class.getCanonicalName());
    }

    protected static String report( String msg )
    {
        return "STATUS: " + msg + "\n";
    }

    protected static String nullOp( String msg )
    {
        return "STATUS: nothing done (" + msg + ")\n";
    }

    protected static String error( String msg )
    {
        return "ERROR: " + msg + "\n";
    }
}
