package net.varanus.util.openflow;


import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFValueType;


/**
 * 
 */
public final class OFOxmUtils
{
    public static <F extends OFValueType<F>> long oxmHeader( MatchField<F> field, OFVersion version )
    {
        return oxmHeader(field, OFFactories.getFactory(version));
    }

    public static <F extends OFValueType<F>> long oxmHeader( MatchField<F> field, OFFactory fact )
    {
        F defVal = MatchFieldUtils.getDefaultValue(field);
        return fact.oxms().fromValue(defVal, field).getTypeLen();
    }

    private OFOxmUtils()
    {
        // not used
    }
}
