package net.varanus.sdncontroller.trafficgenerator.web;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.restlet.resource.Post;

import net.varanus.sdncontroller.trafficgenerator.TrafficProperties;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.json.JSONUtils;
import net.varanus.util.text.IntProperty;


/**
 * 
 */
public final class StartTrafficResource extends TGWebResource
{
    @Post
    public String startTraffic( String json )
    {
        if (json == null) {
            return nullOp("no traffic properties provided");
        }
        else {
            try {
                Map<FlowedLink, Map<String, String>> propsMap = JSONUtils.parseMapOfMaps(
                    json,
                    FlowedLink::parse,
                    Function.identity(),
                    Function.identity());

                List<TrafficProperties> trafficProps = new ArrayList<>();
                for (Entry<FlowedLink, Map<String, String>> entry : propsMap.entrySet()) {
                    FlowedLink flowedLink = entry.getKey();
                    Map<String, String> props = entry.getValue();
                    int pps = readPositiveInt("pps", props);
                    int batchSize = readPositiveInt("batch_size", props);

                    trafficProps.add(TrafficProperties.create(flowedLink, pps, batchSize));
                }

                boolean workDone = false;
                for (TrafficProperties props : trafficProps) {
                    workDone |= getTGService().startTraffic(props);
                }

                if (workDone) {
                    return report("traffic started");
                }
                else {
                    return nullOp("traffic is already started");
                }
            }
            catch (IllegalArgumentException e) {
                return error(e.getMessage());
            }
        }
    }

    private static int readPositiveInt( String prop, Map<String, String> props )
    {
        return IntProperty.ofPositive(prop).readInt(props);
    }
}
