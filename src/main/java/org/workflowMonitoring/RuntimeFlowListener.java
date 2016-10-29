package org.workflowMonitoring;

import java.util.ArrayList;

import groovyjarjarantlr.collections.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.pvm.delegate.ExecutionListenerExecution;
 
 
public class RuntimeFlowListener implements ExecutionListener {
 
	static ArrayList<String> sequenceFlows =  new ArrayList<String>();
	
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		sequenceFlows.add(((ExecutionListenerExecution)execution).getEventSource().getId());
		execution.setVariable("sequenceFlows", sequenceFlows);
	}
	
}
 