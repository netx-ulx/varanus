package net.varanus.sdncontroller.test;


import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.lang.Unsigned;


/**
 * 
 */
public class NiciraExtensionsTest implements IFloodlightModule, IOFSwitchListener
{
    private static final Logger LOG = LoggerFactory.getLogger(NiciraExtensionsTest.class);

    private IOFSwitchService switchService;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices()
    {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
    {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IOFSwitchService.class);
    }

    @Override
    public void init( FloodlightModuleContext context ) throws FloodlightModuleException
    {
        this.switchService = ModuleUtils.getServiceImpl(context, IOFSwitchService.class);
        switchService.addOFSwitchListener(this);
    }

    @Override
    public void startUp( FloodlightModuleContext context ) throws FloodlightModuleException
    {
        // do nothing
    }

    @Override
    public void switchActivated( DatapathId switchId )
    {
        IOFSwitch sw = switchService.getActiveSwitch(switchId);
        if (sw != null) {
            LOG.debug("Switch added: {}", switchId);

            OFFactory fact = sw.getOFFactory();

            long reg0ID = fact.oxms().nxReg0(U32.ZERO).getTypeLen();
            long reg1ID = fact.oxms().nxReg1(U32.ZERO).getTypeLen();
            long reg2ID = fact.oxms().nxReg2(U32.ZERO).getTypeLen();

            OFFlowAdd flowAdd = fact.buildFlowAdd()
                .setTableId(TableId.of(0))
                .setPriority(1)
                .setMatch(fact.buildMatch()
                    .setExact(MatchField.NX_REG0, U32.of(2))
                    .setMasked(MatchField.NX_REG1, U32.of(1), U32.of(1))
                    .build())
                .setInstructions(Arrays.<OFInstruction>asList(
                    fact.instructions().applyActions(Arrays.asList(
                        fact.actions().setField(fact.oxms().nxReg1(U32.of(1))),
                        fact.actions().buildNxRegMove()
                            .setSrc(reg0ID)
                            .setDst(reg1ID)
                            .setSrcOffset(0)
                            .setDstOffset(16)
                            .setNBits(16)
                            .build(),
                        fact.actions().buildNxStackPush()
                            .setField(reg1ID)
                            .setOffset(16)
                            .setNBits(16)
                            .build(),
                        fact.actions().buildNxStackPop()
                            .setField(reg2ID)
                            .setOffset(0)
                            .setNBits(16)
                            .build(),
                        fact.actions().buildNxOutputReg()
                            .setSrc(reg2ID)
                            .setOfsNbits((0 << 6) | (16 - 1))
                            .setMaxLen(Unsigned.MAX_SHORT)
                            .build()))))
                .build();

            sw.write(flowAdd);
        }
    }

    @Override
    public void switchAdded( DatapathId switchId )
    {
        // do nothing
    }

    @Override
    public void switchRemoved( DatapathId switchId )
    {
        // do nothing
    }

    @Override
    public void switchPortChanged( DatapathId switchId, OFPortDesc port, PortChangeType type )
    {
        // do nothing
    }

    @Override
    public void switchChanged( DatapathId switchId )
    {
        // do nothing
    }

    @Override
    public void switchDeactivated( DatapathId switchId )
    {
        // do nothing
    }
}
