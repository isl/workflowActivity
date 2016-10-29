package org.workflowMonitoring;

import java.util.Date;
import java.util.Map;



public class WorkflowLayerAggregator {
	
	String url;
	double workflowExecutionTime;
	double workflowDelayTime;
	double workflowTransitionDelayTime;
	
	public WorkflowLayerAggregator(String url) {

		this.url = url;
	}


    public void aggregateAndSendMetrics(Map<String, Long> transitionDelayTimes,
			Map<String, Long> durationTaskTimes,
			Map<String, Long> managementDelayTimes,
			Map<String, Long> queuingDelayTimes, String processBusinessKey) throws Exception {
		
    	sendAndCalculateTaskAndWorkflowExecutionTime(durationTaskTimes, processBusinessKey);
		sendAndCalculateTaskAndWorkflowTransitionDelayTime(transitionDelayTimes, processBusinessKey);
		sendAndCalculateTaskAndWorkflowDelayTime(managementDelayTimes, queuingDelayTimes, processBusinessKey);
		System.out.println("Bika gia to calculation twn tasks5");

		// Calculation of WorkflowProcessTime
		sendAndCalculateWorkflowProcessTimeAndOverallExecutionTime(processBusinessKey);
		System.out.println("Bika gia to calculation twn tasks8");

		
	}

	private void sendAndCalculateWorkflowProcessTimeAndOverallExecutionTime(String processBusinessKey) throws Exception {
		
		KairosDbClient kairosDbClient = new KairosDbClient(url);

		double workflowProcessTime = this.workflowDelayTime + this.workflowExecutionTime + this.workflowTransitionDelayTime;
		kairosDbClient.putAlreadyInstantiateMetric(processBusinessKey+ "_workflowProcessTime", new Date().getTime(), workflowProcessTime );
		
		// and for the metric of Overall in activiti is the same as the workflowProcess Time so
		kairosDbClient.putAlreadyInstantiateMetric(processBusinessKey+ "_overallWorkflowExecutionTime", new Date().getTime(), workflowProcessTime );

		System.out.println("Bika gia to calculation twn tasks4");

	}


	private void sendAndCalculateTaskAndWorkflowDelayTime(Map<String, Long> managementDelayTimes,Map<String, Long> queuingDelayTimes, String processBusinessKey) {
		// TODO Auto-generated method stub
//		KairosDbClient kairosDbClient = new KairosDbClient(url);
		double workflowDelayTime = 0;
		System.out.println("Bika gia to calculation twn tasks3");

	
		// 1st for taskManagementDelayTimes
		// 2nd for taskQueuingDelayTimes
		
		// 3rd put the workflowDelayTimes
		
		//put the workflow delay time
		this.workflowDelayTime = workflowDelayTime;
	}


	private void sendAndCalculateTaskAndWorkflowTransitionDelayTime(Map<String, Long> transitionDelayTimes, String processBusinessKey) throws Exception {
		
		KairosDbClient kairosDbClient = new KairosDbClient(url);
		double workflowTransitionDelayTime = 0;
		System.out.println("Bika gia to calculation twn tasks1");
		// here we are putting the transistion delay task times
		for ( Map.Entry e : transitionDelayTimes.entrySet())
		{
			String metricName = e.getKey().toString();
			Double dmilli = Double.parseDouble(e.getValue().toString());
			workflowTransitionDelayTime = workflowTransitionDelayTime +dmilli;
			kairosDbClient.putAlreadyInstantiateMetric( metricName +"_taskTransDelayTime", new Date().getTime(), e.getValue());
		}
		
		//put of the WorkflowTransistionDelayTime
		kairosDbClient.putAlreadyInstantiateMetric(processBusinessKey + "_workflowTransDelayTime", new Date().getTime(), workflowTransitionDelayTime);
	
		this.workflowTransitionDelayTime = workflowTransitionDelayTime;
		
	}


	private void sendAndCalculateTaskAndWorkflowExecutionTime(Map<String, Long> durationTaskTimes, String processBussinessKey) throws Exception {
		
		KairosDbClient kairosDbClient = new KairosDbClient(url);
		double workflowExecutionTime = 0;
		System.out.println("Bika gia to calculation twn tasks2");

		// here we are putting the task execution Times
		for (Map.Entry e : durationTaskTimes.entrySet()){
			String metricName = e.getKey().toString();
			Double dmilli = Double.parseDouble(e.getValue().toString());
			workflowExecutionTime = workflowExecutionTime + dmilli;
			kairosDbClient.putAlreadyInstantiateMetric( metricName +"_taskExecTime", new Date().getTime(), e.getValue());
		}
		
		//put of the WorkflowExecutionTime
		kairosDbClient.putAlreadyInstantiateMetric(processBussinessKey + "_workflowExecTime", new Date().getTime(), workflowExecutionTime);
		this.workflowExecutionTime = workflowExecutionTime;
	}


	
	
	

}
