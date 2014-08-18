/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is the DataListener for SFP changes.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */

public class SfcProviderSfpDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfpDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        /*
         * when a SFP is created we will process and send it to southbound devices. But first we need
         * to make sure all info is present or we will pass.
         */
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionPaths) {

                ServiceFunctionPaths updatedServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                Object[] serviceForwarderObj = {updatedServiceFunctionPaths};
                Class[] serviceForwarderClass = {ServiceFunctionPaths.class};
                odlSfc.executor.execute(SfcProviderRestAPI.getPutServiceFunctionPaths (serviceForwarderObj, serviceForwarderClass));

                //
                // Write the flows to the SFF
                // For now these will be both ACL and NextHop flow tables,
                // later on logic needs to be added here to write different flow
                // entry types depending on the individual switch encapsulation, etc
                //
                writeSffFlows(updatedServiceFunctionPaths);
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    private void writeSffFlows(ServiceFunctionPaths updatedServiceFunctionPaths) {
    	Iterator<ServiceFunctionPath> sfpIter = updatedServiceFunctionPaths.getServiceFunctionPath().iterator();

    	// Each Service Function Path configured
    	while(sfpIter.hasNext()) {
    		ServiceFunctionPath sfp = sfpIter.next();
    		Iterator<SfpServiceFunction> sfpSfIter = sfp.getSfpServiceFunction().iterator();

    		// Each Service Function in the Service Function Path
    		while(sfpSfIter.hasNext()) {
    			SfpServiceFunction sfpSf = sfpSfIter.next();

    			// The SFF name should be the name of the actual switch
    			SfcProviderSffFlowWriter.getInstance().setNodeInfo(sfpSf.getServiceFunctionForwarder());

    			ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sfpSf.getServiceFunctionForwarder());
    			//Iterator<SffDataPlaneLocator> sffDplIter = sff.getSffDataPlaneLocator().iterator();
    			//Iterator<ServiceFunctionDictionary> sffSfDictIter = sff.getServiceFunctionDictionary().iterator();
    			// TODO need to get the inPort and srcMac


    			// Get everything needed to write to the Next Hop table
    			ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunction(sfpSf.getName());
    			LocatorType sfLt = sf.getSfDataPlaneLocator().getLocatorType();
    				
    			// TODO the Mac DataPlanLocator choice was temporarily removed due to yangtools bug: https://bugs.opendaylight.org/show_bug.cgi?id=1467
    			// TODO get the dst MACs and the outPort from the LocatorType
    			//if(!sfLt.getImplementedInterface().equals(Mac.class)) {
    			//    What to do if its not a MAC locator??
    			//}
    			// This is the other choice, but I dont think we'll be using it
    			//else if(sfLt.getImplementedInterface().equals(Ip.class))


    			//SfcProviderSffFlowWriter.getInstance().writeSffNextHop(inPort, sfp.getPathId(), srcMac, dstMac, outPort);

    			// TODO need to get the ACL and fill in the following fields
    			//SfcProviderSffFlowWriter.getInstance().writeSffAcl(srcIp, dstIp, srcPort, dstPort, protocol, sfp.getPathId());
    		}
    	}
	}

}