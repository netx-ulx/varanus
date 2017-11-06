package net.varanus.sdncontroller.trafficgenerator.web;


import org.restlet.resource.Post;

import net.varanus.sdncontroller.types.FlowedLink;


/**
 * 
 */
public final class StopTrafficResource extends TGWebResource
{
    @Post
    public String stopTraffic( String str )
    {
        if (str == null) {
            return nullOp("no flowed-link provided");
        }
        else {
            try {
                if (str.trim().equalsIgnoreCase("all")) {
                    if (getTGService().stopAllTraffic()) {
                        return report("all traffic stopped");
                    }
                    else {
                        return nullOp("all traffic is already stopped");
                    }
                }
                else {
                    FlowedLink flowedLink = FlowedLink.parse(str);
                    if (getTGService().stopTraffic(flowedLink)) {
                        return report("traffic stopped for the provided flowed-link");
                    }
                    else {
                        return nullOp("traffic for the provided flowed-link is already stopped");
                    }
                }
            }
            catch (IllegalArgumentException e) {
                return error(e.getMessage());
            }
        }
    }
}
