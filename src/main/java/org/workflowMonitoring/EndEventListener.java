package org.workflowMonitoring;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.history.HistoricActivityInstance;
import org.kairosdb.client.builder.DataPoint;

 
 public class EndEventListener implements ExecutionListener {
 
	//the below values were static 
	private  Map<String,Long> transitionDelayTimes;
	private  Map<String,Long> durationTaskTimes;
	private  Map<String,Long> managementDelayTimes;
	private  Map<String,Long> queuingDelayTimes;
	
	//Fidelity variables	
	private Map<String,Map<Long,Double>> workflowHistoryMeasurements;// first we have the name of the metric , then the timestamp of the datapoint and the value of the datapoint in double
	private Map<String,Map<String,Double>> workflowUserConstrains; // the first string equals with the metric name (same as above) and 
	// the second string equals with the word "low" or "high" which depicts the type of the threshold
	// and the double equals with the value of the constraint (it could be a random number)
	
	// the same as above
	private Map<String,Map<Long,Double>> taskHistoryMeasurements;	
	private Map<String,Map<String,Double>> taskUserConstrains;
	
	double workflowExecutionTime;
	double workflowDelayTime;
	double workflowTransitionDelayTime;
	String url = "http://localhost:8088/";
	String publisherUrl;
	KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");
	
	long startTimeEndEvent = new java.util.Date().getTime();
	
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		KairosDbClient client = new KairosDbClient(url);
		client = client.initializeFullBuilder(client);

		centralClient = centralClient.initializeFullBuilder(centralClient);
		
		// push the metrics to the central TSDB not yet
//		KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");
		
		
		// get the flows
		Map<String,Object> vars =  execution.getVariables();
		ArrayList<String> flows = (ArrayList<String>) vars.get("sequenceFlows");
		assertNotNull(flows);
//		for(int i=0; i<flows.size(); i++)
//		{
//			System.out.println("The " + i + "st" + " flow is : " + flows.get(i));
//		}
		
		// where the workflow has passed from till the activity before endEvent
		ArrayList<HistoricActivityInstance> inq = (ArrayList<HistoricActivityInstance>) execution.getEngineServices().getHistoryService()
				.createHistoricActivityInstanceQuery()
				.orderByHistoricActivityInstanceStartTime()
				.asc()
				.list();
		
		assertNotNull(inq);
//		System.out.println("Size of inq equals with : " + inq.size());
//		for(int i=0; i< inq.size(); i++)
//		{
//		      assertNotNull(inq.get(i).getStartTime());
//		      System.out.println("StartTime for taskId " + inq.get(i).getProcessDefinitionId() + " and startTime " + inq.get(i).getStartTime() + " and " + inq.get(i).getActivityName());
//		      assertNotNull(inq.get(i).getEndTime());
//		      System.out.println("EndTime for taskId " + inq.get(i).getProcessDefinitionId()+ " and endTime " + inq.get(i).getEndTime() + " and " + inq.get(i).getActivityName());
//		}	    
		//client.deleteMetrics();
		
		
		
		// FROM HERE AND DOWN WE AGGREAGE AND CALCULATE VALUES WITH DEPENDENcYAGGREGATION IN PLACES THAT TWE CALL THE SUBSCRIBER
		// AND BY THE USAGE OF THE WORKFLOW AGGREGATOR
	    String workflowNameMetric = execution.getProcessDefinitionId().replace(":","");
	    String serviceIp = (String) execution.getVariable("serviceIp");
	    this.publisherUrl = "tcp://"+serviceIp+":5563/";
		System.out.println("publisher ip equals with : " + this.publisherUrl);
//	    initializeWorkflowPastMeasurements(workflowNameMetric, client);
//	    initializeTaskPastMeasurements(workflowNameMetric, client);
//	    initializeWorkflowPastMeasurements(workflowNameMetric, centralClient);
//	    initializeTaskPastMeasurements(workflowNameMetric, centralClient);
		
		// TIME METRICS
	    double trans = setTransitionDelayTimes(workflowNameMetric,flows,inq, client);
	    double dur = setTaskDurationTimes(workflowNameMetric,inq, client);
	    double delay = setTaskDelayTimes(workflowNameMetric,execution, client);
		System.out.println("Trans equals with : " + trans + " and duration equals with : " + dur + " and delay equals with : " + delay);   
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_TransitionDelayTime", startTimeEndEvent, trans);
    	centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_TransitionDelayTime", startTimeEndEvent, trans);

	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_DelayTime", startTimeEndEvent, delay);
    	centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_DelayTime", startTimeEndEvent, delay);

	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_ExecutionTime", startTimeEndEvent, dur);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_ExecutionTime", startTimeEndEvent, dur);

	    
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_ProcessTime", startTimeEndEvent, trans + dur + delay);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_ProcessTime", startTimeEndEvent, trans + dur + delay);

	    
	    
    
	    // SECURITY METRICS
	    double incidentRate = getWorkflowAuthenticationSecurity(workflowNameMetric, client);
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_authSecurity", startTimeEndEvent, incidentRate);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_authSecurity", startTimeEndEvent, incidentRate);

	    
	    
	    // RELIABILITY METRICS
	    Map<String,Double> mttrValues = subscribeMtbfTotalUpTimesNumOfBreakDowns(workflowNameMetric,client);
	    Map<String,Double>  taskAvailabilityMetrics = calculationAndPutOfTaskAvailabilityMetrics(workflowNameMetric,mttrValues,client);//inside this method the task availability values are being put
	    double workflowAvailabilityMetric = calculateWorkflowAvailability(taskAvailabilityMetrics);
	    System.out.println("WorkflowAvailability equals with " + workflowAvailabilityMetric);
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_availability", startTimeEndEvent, workflowAvailabilityMetric);
    	centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_availability", startTimeEndEvent, workflowAvailabilityMetric);

	    calculationTaskFailureRate(workflowNameMetric,client); // puts the taskFailureRate in KairosDB
	    double totalWorkflowFailureTime = calculationWorkflowFailureTime(client);	
	    System.out.println("WorkflowFailureTime equals with : " + workflowNameMetric + "_failureTime and with totalWorkflowFailureTime :" + totalWorkflowFailureTime);
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_failureTime", startTimeEndEvent, totalWorkflowFailureTime);
    	centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_failureTime", startTimeEndEvent, totalWorkflowFailureTime);

	    
	    // COST METRICS
	    double workflowCost = calculateWorkflowCost(workflowNameMetric,inq);
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_workflowCost", startTimeEndEvent, workflowCost);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_workflowCost", startTimeEndEvent, workflowCost);
	    double workflowEngineCost = calculateWorkflowEngineCost(inq, flows, workflowNameMetric, client);
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_workflowEngineCost", startTimeEndEvent, workflowEngineCost);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_workflowEngineCost", startTimeEndEvent, workflowEngineCost);
	    taskEnactmentCostCalculation(workflowNameMetric, inq, client);
	    taskExecutionCost(workflowNameMetric, inq, client);
	    
	    
	    // FIDELITY METRICS
//	    initializeWorkflowPastMeasurements(workflowNameMetric, client);
	    double workflowFidelity = calculateWorkflowFidelity(workflowNameMetric, client); // it will calculate based on past metrics and then based on new
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_workflowFidelity", new Date().getTime(), workflowFidelity);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_workflowFidelity", new Date().getTime(), workflowFidelity);
//	    initializeTaskPastMeasurements(workflowNameMetric, client);
	    taskCalculationFidelity(workflowNameMetric, client);
	    
	}
	
	

	private void initializeTaskPastMeasurements(String workflowNameMetric,KairosDbClient client) throws Exception {
		
		String taskMetricMeasurements [] = { "_failureRate", "_availability", "_exec", "_delayProcessTask"};

		System.out.println("Task History measurements to be put");
		// for the reliabilityUserPage
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 100.0);    
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 23.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 0.10);

		
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 98.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 1.4);

		
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 4000.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 2333.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 23.0);

		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 500000.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 232323.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 0.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_UserPage"+taskMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 3.0);

		
		// for the reliabilityObjectAdminPage
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 99.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 22.0);

		
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 99.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 23.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 22.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 1.0);

		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 0.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 21212.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 3.0);
		
		
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 10000000.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_ObjectAdminPage"+taskMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 0.0);

		
		System.out.println("Task History measurements are putted");
	}



	private void initializeWorkflowPastMeasurements(String workflowNameMetric, KairosDbClient client) throws Exception {

		String workflowMetricMeasurements [] = { "_TransitionDelayTime", "_ProcessTime", "_availability", "_failureRate"
		};
		
		System.out.println("Workflow History measurements to be put");
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(9), 23.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2), 145.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[0], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(8), 1001.0);

		
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2), 999.0);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[1], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), 10001.0);
		
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), 0.99);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2), 1.99);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4), 22.99);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[2], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5), 26.99);

		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), 0.3);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2), 0.4);
		client.putAlreadyInstantiateMetric(workflowNameMetric+workflowMetricMeasurements[3], System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4), 49.0);
	
		System.out.println("Workflow History measurements are putted");

	}



	private void taskCalculationFidelity(String workflowNameMetric, KairosDbClient client) throws Exception {
		
		// First initialize with the threasholds with a standar way (certain numbers)
		String taskMetricMeasurements [] = { "_failureRate", "_availability", "_exec", "_delayProcessTask"};
		
		String typeOfTasks [] = {"UserPage", "PermAdminPage", "ObjectPage",
	          "OuUserPage", "RolePage", "UserDetailPage", 
	          "PermPage", "ObjectAdminPage", "RoleAdminPage"
	          , "OuPermPage", "SdDynamicPage", "SdStaticPage"};
		
		double numberOfTaskMeasurements;
		double taskFidelityComFunction;
		double taskFidelity;
		
		// we put the same values of the same type of constrains for all the tasks
		taskUserConstrains = new HashMap<String,Map<String,Double>>();

		for(int i =0 ; i< taskMetricMeasurements.length; i++){
	
			System.out.println(" Now the userContraints to put are  for the task metric of : " + taskMetricMeasurements[i]);
			
			Map<String,Double> threasholds = new HashMap<String,Double>();
			
			if(taskMetricMeasurements[i].equals("_failureRate")){
				threasholds.put("up", 100.0);
				threasholds.put("down", 0.02);
			}
			else if (taskMetricMeasurements[i].equals("_availability")){
				threasholds.put("up",99.0);
				threasholds.put("down", 1.4);
			}
			else if (taskMetricMeasurements[i].equals("_exec")){
				threasholds.put("up", 6000.0);
				threasholds.put("down", 120.01);
			}
			else{
				// not needed for failureRate because ideally we want no failures
				threasholds.put("up", 600000.0);
				threasholds.put("down", 0.1);
			}
			taskUserConstrains.put(taskMetricMeasurements[i], threasholds); // four values 
		}
		
		// now take the taskHistoryMeasurements
		taskHistoryMeasurements = new HashMap<String,Map<Long,Double>>();
		
		
		for (int i=0; i<typeOfTasks.length; i++){
			
			for (int j=0; j<taskMetricMeasurements.length; j++){
				
				System.out.println("Query for the data point of : " + workflowNameMetric+"_task_"+typeOfTasks[i]+taskMetricMeasurements[j]);
				String metricName = workflowNameMetric+"_task_"+typeOfTasks[i]+taskMetricMeasurements[j];
				List<DataPoint> listDatapoints = client.QueryDataPointsAbsolute(metricName, new Date(0), null);
				
				if(listDatapoints.size() >0) {
	                System.out.println("The size of the task listDataPoint with metric name "+ metricName + "equals with: " + listDatapoints.size());
	                Map<Long, Double> mapOfMetric = new HashMap<Long,Double>();

	                for (int k = 0; k < listDatapoints.size(); k++) {
	                    Long timestamp = listDatapoints.get(k).getTimestamp();
	                    Object value = listDatapoints.get(k).getValue();
	                    Double dvalue = Double.parseDouble(value.toString());
	                    mapOfMetric.put(timestamp, dvalue);
	                }
	                taskHistoryMeasurements.put(metricName, mapOfMetric);
				}
				
			}
		
		}
		
		for(int i=0; i<typeOfTasks.length;i++){
			
			numberOfTaskMeasurements = 0;
			taskFidelityComFunction = 0;
			taskFidelity = 0;
			
			for(int j=0; j<taskMetricMeasurements.length; j++){
				System.out.println("Going to calculate the fidelity of the task named : " + typeOfTasks[i] + " and with measurement is : " + (workflowNameMetric+"_task_"+typeOfTasks[i]+taskMetricMeasurements[j]));

				Map<Long,Double> taskValues = taskHistoryMeasurements.get(workflowNameMetric+"_task_"+typeOfTasks[i]+taskMetricMeasurements[j]);

				Map<String,Double> threasholds = taskUserConstrains.get(taskMetricMeasurements[j]);
				double upThreasHold = threasholds.get("up");
				double downThreasHold = threasholds.get("down");
				double dvalue;
				
				if(taskValues != null){
				numberOfTaskMeasurements = numberOfTaskMeasurements + taskValues.size();
				for(Map.Entry e : taskValues.entrySet()){
					dvalue = Double.parseDouble(e.getValue().toString());
					
					if(dvalue <= upThreasHold && downThreasHold <= dvalue){
						taskFidelityComFunction = taskFidelityComFunction +1;
						System.out.println("TaskMetric : " + workflowNameMetric+"_task_"+typeOfTasks[i]+taskMetricMeasurements[j] + " fulfill the threasholds");
					}
					else
						{
						taskFidelityComFunction = taskFidelityComFunction + 0; // reduntant... just to show how it works in relation to the QM of workflow
						System.out.println("TaskMetric : " + workflowNameMetric+"_task_"+typeOfTasks[i]+taskMetricMeasurements[j] + " does not fulfill the threasholds");
						}
				}
				
				}
			}
			if( numberOfTaskMeasurements > 0){
			taskFidelity = taskFidelityComFunction / numberOfTaskMeasurements;
			System.out.println("The task fidelity: "+  workflowNameMetric+"_"+typeOfTasks[i]+ "_taskFidelity and value : " + taskFidelity);
			client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+typeOfTasks[i]+ "_taskFidelity" , new Date().getTime(), taskFidelity);
			centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+typeOfTasks[i]+ "_taskFidelity" , new Date().getTime(), taskFidelity);
			}
		}
		
	}


	private double calculateWorkflowFidelity(String workflowNameMetric, KairosDbClient client) throws Exception {		
		
		// Implementation of the Workflow Fidelity
		// First fill the threashold with numbers for each of the metrics with a standar way (certain numbers)
		double workflowFidelity;
		double workflowFidelityComFunction;
		double numberOfMeasurements;
		String workflowMetricMeasurements [] = { "_TransitionDelayTime", "_ProcessTime", "_availability", "_failureTime"
		};
		workflowUserConstrains = new HashMap<String,Map<String,Double>>();
		
		// insertion of certain threasHolds
		for (int i=0; i<workflowMetricMeasurements.length; i++){
			
			System.out.println(" Now the userContraints to put are  for the metric of : " + workflowNameMetric+workflowMetricMeasurements[i]);
			
			Map<String,Double> threasholds = new HashMap<String,Double>();
			
			if(workflowMetricMeasurements[i].equals("_TransitionDelayTime")){
				threasholds.put("up", 1000.0);
				threasholds.put("down", 0.01);
			}
			else if (workflowMetricMeasurements[i].equals("_ProcessTime")){
				threasholds.put("up",900000.0);
				threasholds.put("down", 1000.0);
			}
			else if (workflowMetricMeasurements[i].equals("_availability")){
				threasholds.put("up", 23.0);
				threasholds.put("down", 1.01);
			}
			else{
				// not needed for failureRate bevause ideally we want no failures
				threasholds.put("up", 50.0);
				threasholds.put("down", 0.4);
			}
			workflowUserConstrains.put(workflowNameMetric+workflowMetricMeasurements[i], threasholds);
		}
		// then take from kairosdb
		
		// now take the workflowHistoryMeasurements
		workflowHistoryMeasurements = new HashMap<String,Map<Long,Double>>();
		
		for (int i=0; i<workflowMetricMeasurements.length; i++){
			
			// the date here could be changed to a certain time period, here we take from the start of all time for the linux epoch
			// which is the 01/01/1970 . This could changed and take value from the deriviation of OWL-Q
			List<DataPoint> listDatapoints = client.QueryDataPointsAbsolute(workflowNameMetric+workflowMetricMeasurements[i], new Date(0), null);
			
			if(listDatapoints.size() >0) {
                System.out.println("The size of the workflow listDataPoint with metric name "+ workflowNameMetric+workflowMetricMeasurements[i] + "equals with: " + listDatapoints.size());
                Map<Long, Double> mapOfMetric = new HashMap<Long,Double>();

                for (int j = 0; j < listDatapoints.size(); j++) {
                    Long timestamp = listDatapoints.get(j).getTimestamp();
                    Object value = listDatapoints.get(j).getValue();
                    Double dvalue = Double.parseDouble(value.toString());
                    mapOfMetric.put(timestamp, dvalue);
                }
                workflowHistoryMeasurements.put(workflowNameMetric+workflowMetricMeasurements[i], mapOfMetric);
			}
			
		}
		
		// then calculate we have the threasholds and the values and calculation
		workflowFidelityComFunction=0;
		numberOfMeasurements = 0;
		// calculation of the comp_fuction as defined in the QM of Workflow
		for(int i=0; i< workflowMetricMeasurements.length; i++){
			
			Map<String,Double> threasholds = workflowUserConstrains.get(workflowNameMetric+workflowMetricMeasurements[i]);
			Map<Long,Double> workflowValues = workflowHistoryMeasurements.get(workflowNameMetric+workflowMetricMeasurements[i]);
			
			double upThreasHold = threasholds.get("up");
			double downThreasHold = threasholds.get("down");
			double dvalue;
			
			// adition of the number of measurements of a certain metric
			System.out.println("We are taking the workflowValues for the metric of : " +workflowNameMetric+workflowMetricMeasurements[i] );
			numberOfMeasurements = numberOfMeasurements + workflowValues.size();
			
			for(Map.Entry e : workflowValues.entrySet()){
				dvalue = Double.parseDouble(e.getValue().toString());
				
				if(dvalue <= upThreasHold && downThreasHold <= dvalue){
					workflowFidelityComFunction = workflowFidelityComFunction +1;
					System.out.println("WorkflowMetric : " + workflowNameMetric+workflowMetricMeasurements[i] + " fulfill the threasholds");
				}
				else
					{workflowFidelityComFunction = workflowFidelityComFunction + 0; // reduntant... just to show how it works in relation to the QM of workflow
					System.out.println("WorkflowMetric : " + workflowNameMetric+workflowMetricMeasurements[i] + " does not fulfill the threasholds");
					}
			
			}
		}
	
		// then return the value
		workflowFidelity = workflowFidelityComFunction / numberOfMeasurements;
		System.out.println("workflowFidelity equals with : " + workflowFidelity + " and numberOfMeasurementsWith : " + numberOfMeasurements);
		return workflowFidelity;
	}


	private void taskExecutionCost(String workflowNameMetric, ArrayList<HistoricActivityInstance> inq, KairosDbClient client) throws Exception {
		String activityType;
		String activityName;
		double taskExecutionCost;
		double directLaborCost;
		double machineCost;
		double additionalResourcesCost;
		double randomValue;
		
		final String prefixMetricNames[] = {
		          "UserPage","PermAdminPage","ObjectPage","OuUserPage","RolePage","UserDetailPage",
		          "PermPage","ObjectAdminPage","RoleAdminPage","OuPermPage",
		          "SdDynamicPage","SdStaticPage"
			 };
		
		
		int j =0;
		for(int i=0; i<inq.size(); i++){
			 activityType = inq.get(i).getActivityType();
			if(activityType.equals("serviceTask") || activityType.equals("userTask") || activityType.equals("scriptTask")){
				activityName = inq.get(i).getActivityName();
				activityName = activityName.replace(" ","");
				j=0;
				for(; j< prefixMetricNames.length; j++){
					
				if(activityName.contains(prefixMetricNames[j])){
				
				randomValue = randomTaskNumber(0, 1);
				
				if(randomValue < 0.5){
					directLaborCost = 0;
					machineCost = randomTaskNumber();}
				else{
					machineCost = 0;
					directLaborCost = randomTaskNumber();
					}
				
				additionalResourcesCost = randomTaskNumber();
				
				System.out.println("directLaborCost equals to : " + directLaborCost + " machineCost equals to : " + machineCost + " additionalResourcesCost equals to : " + additionalResourcesCost);
			    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+prefixMetricNames[j]+"_directLaborCost", startTimeEndEvent, directLaborCost);
			    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+prefixMetricNames[j]+ "_machineCost", startTimeEndEvent, machineCost);
			    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+prefixMetricNames[j]+ "_additionalResourcesCost", startTimeEndEvent, additionalResourcesCost);
			    
			    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+prefixMetricNames[j]+"_directLaborCost", startTimeEndEvent, directLaborCost);
			    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+prefixMetricNames[j]+ "_machineCost", startTimeEndEvent, machineCost);
			    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+prefixMetricNames[j]+ "_additionalResourcesCost", startTimeEndEvent, additionalResourcesCost);
			    
				// taskEnactmentCost
				taskExecutionCost = directLaborCost + machineCost + additionalResourcesCost;
				System.out.println("taskExecutionCost with name : " + workflowNameMetric + "_" +prefixMetricNames[j] + "_taskExecutionCost and value : " + taskExecutionCost);
			    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j]+"_taskExecutionCost", startTimeEndEvent, taskExecutionCost);
			    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j]+"_taskExecutionCost", startTimeEndEvent, taskExecutionCost);
					}
				}
				if(j== prefixMetricNames.length){
					randomValue = randomTaskNumber(0, 1);
					
					if(randomValue < 0.5){
						directLaborCost = 0;
						machineCost = randomTaskNumber();}
					else{
						machineCost = 0;
						directLaborCost = randomTaskNumber();
						}
					
					additionalResourcesCost = randomTaskNumber();
					
					System.out.println("directLaborCost equals to : " + directLaborCost + " machineCost equals to : " + machineCost + " additionalResourcesCost equals to : " + additionalResourcesCost);
				    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName+"_directLaborCost", startTimeEndEvent, directLaborCost);
				    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName+ "_machineCost", startTimeEndEvent, machineCost);
				    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName+ "_additionalResourcesCost", startTimeEndEvent, additionalResourcesCost);
				    
				    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName+"_directLaborCost", startTimeEndEvent, directLaborCost);
				    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName+ "_machineCost", startTimeEndEvent, machineCost);
				    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName+ "_additionalResourcesCost", startTimeEndEvent, additionalResourcesCost);
				    
					// taskEnactmentCost
					taskExecutionCost = directLaborCost + machineCost + additionalResourcesCost;
					System.out.println("taskExecutionCost with name : " + workflowNameMetric + "_" +activityName + "_taskExecutionCost and value : " + taskExecutionCost);
				    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName +"_taskExecutionCost", startTimeEndEvent, taskExecutionCost);
				    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName +"_taskExecutionCost", startTimeEndEvent, taskExecutionCost);
				}
	     }
	  }
		
    }


	private void taskEnactmentCostCalculation(String workflowNameMetric, ArrayList<HistoricActivityInstance> inq, KairosDbClient client) throws Exception {

		String activityType;
		String activityName;
		double setUpCost =0 ;
		double tearDownCost =0;
		double taskEnactmentCost =0;
		
		final String prefixMetricNames[] = {
		          "UserPage","PermAdminPage","ObjectPage","OuUserPage","RolePage","UserDetailPage",
		          "PermPage","ObjectAdminPage","RoleAdminPage","OuPermPage",
		          "SdDynamicPage","SdStaticPage"
			 };
		int j=0;
		for(int i=0; i<inq.size(); i++){
			 activityType = inq.get(i).getActivityType();
			if(activityType.equals("serviceTask") || activityType.equals("userTask") || activityType.equals("scriptTask")){
				activityName = inq.get(i).getActivityName();
				activityName = activityName.replace(" ","");
				setUpCost = randomTaskNumber(1, 10);
				tearDownCost = randomTaskNumber(1, 10);
				j=0;
				for(; j< prefixMetricNames.length; j++)
				{
				if(activityName.contains(prefixMetricNames[j])){
				// setUpCost
				System.out.println("setUpCost with metric name :  " + workflowNameMetric+"_"+ prefixMetricNames[j] +"_setUpCost and value : " + setUpCost);
			    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j] +"_setUpCost", startTimeEndEvent, setUpCost);
			    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j] +"_setUpCost", startTimeEndEvent, setUpCost);

				// tearDownCost
				System.out.println("tearDownCost with metric name : " + workflowNameMetric+"_"+ prefixMetricNames[j] +"_tearDownCost and value : " + tearDownCost);
			    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j] +"_tearDownCost", startTimeEndEvent, tearDownCost);
			    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j] +"_tearDownCost", startTimeEndEvent, tearDownCost);

				// taskEnactmentCost
				taskEnactmentCost = setUpCost + tearDownCost;
				System.out.println("taskEnactmentCost with metric name : " + workflowNameMetric+"_"+ prefixMetricNames[j] + "_taskEnactmentCost and value : " + taskEnactmentCost);
			    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j] + "_taskEnactmentCost", startTimeEndEvent, taskEnactmentCost);
			    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+ prefixMetricNames[j] + "_taskEnactmentCost", startTimeEndEvent, taskEnactmentCost);
					}
				
				}
				if(j == prefixMetricNames.length){
					
					// setUpCost
					System.out.println("setUpCost with name :  " + activityName +"_setUpCost and value : " + setUpCost);
				    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName +"_setUpCost", startTimeEndEvent, setUpCost);
				    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName +"_setUpCost", startTimeEndEvent, setUpCost);

					// tearDownCost
					System.out.println("tearDownCost with name : " + activityName + "_tearDownCost and value : " + tearDownCost);
				    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName + "_tearDownCost", startTimeEndEvent, tearDownCost);
				    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName + "_tearDownCost", startTimeEndEvent, tearDownCost);

					// taskEnactmentCost
					taskEnactmentCost = setUpCost + tearDownCost;
					System.out.println("taskEnactmentCost with name : " + activityName + "_taskEnactmentCost and value : " + taskEnactmentCost);
				    client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName + "_taskEnactmentCost", startTimeEndEvent, taskEnactmentCost);
				    centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+activityName + "_taskEnactmentCost", startTimeEndEvent, taskEnactmentCost);
				}
			}
		}
		
	}
		
	


	private double calculateWorkflowEngineCost(ArrayList<HistoricActivityInstance> inq, ArrayList<String> flows, String workflowNameMetric, KairosDbClient client ) throws Exception {
		
		
		double workflowEngineCost = 0 ;
		
		double managingWorkflowInstanceCost = 0 ;
		double workflowPerfMillisec =0 ;
		
		double resourceTransferCost = 0;
		
		double resourceCalculationCost = 0;
		int numberOfTasks=0;

		
		for(int i=0; i<inq.size(); i++)
		workflowPerfMillisec = workflowPerfMillisec + inq.get(i).getDurationInMillis();
		
		// calculation of the managingWorkflow instance cost
		double hoursMWIC =((workflowPerfMillisec / (1000*60*60)) % 24);
		managingWorkflowInstanceCost = hoursMWIC * randomTaskNumber();
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_managingWorkflowInstanceCost", startTimeEndEvent, managingWorkflowInstanceCost);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_managingWorkflowInstanceCost", startTimeEndEvent, managingWorkflowInstanceCost);
		System.out.println("managingWorkflowInstanceCost equals with : " + managingWorkflowInstanceCost);
		
		
		// calculation of resourceTransferCost
		resourceTransferCost = flows.size() * randomTaskNumber(1, 10);
		System.out.println("resourceTransferCost equals with : " + resourceTransferCost);
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_resourceTransferCost", startTimeEndEvent, resourceTransferCost);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_resourceTransferCost", startTimeEndEvent, resourceTransferCost);

		
		for(int i=0; i<inq.size(); i++){
			String activityType = inq.get(i).getActivityType();
			if(activityType.equals("serviceTask") || activityType.equals("userTask") || activityType.equals("scriptTask")){
				numberOfTasks++;
			}
		}
		
		// calculation of resourcesCalculationCost
		resourceCalculationCost = numberOfTasks * randomTaskNumber();
		System.out.println("resourcesCalcualtionCost equals with : " + resourceCalculationCost);
	    client.putAlreadyInstantiateMetric(workflowNameMetric + "_resourceCalculationCost", startTimeEndEvent, resourceCalculationCost);
	    centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_resourceCalculationCost", startTimeEndEvent, resourceCalculationCost);

		
		workflowEngineCost = managingWorkflowInstanceCost + resourceTransferCost + resourceCalculationCost;
		System.out.println("WorkflowEngineCost equals with : " + workflowEngineCost);
		
		return workflowEngineCost;
	}


	private double randomTaskNumber(double min, double max) {
		
		Random r = new Random();
		
		double randomValue = min + (max - min) * r.nextDouble();
		return randomValue;
	}


	// calculation of the workflowCost
	private double calculateWorkflowCost(String workflowNameMetric, ArrayList<HistoricActivityInstance> inq) {
		String activityType;
		String activityName;
		Random r = new Random();
		double costWorkflow = 0;
		
		for(int i=0; i<inq.size(); i++){
			activityType = inq.get(i).getActivityType();
			if(activityType.equals("serviceTask") || activityType.equals("userTask") || activityType.equals("scriptTask")){
				activityName = inq.get(i).getActivityName();
				System.out.println("Activity with name :" + activityName + " had duration of->" + inq.get(i).getDurationInMillis() );
				costWorkflow = costWorkflow + randomTaskNumber();
			}
		}
		System.out.println("Calculation of workflowCost equals to : " + costWorkflow + " measuring units");
		return costWorkflow;
	}


	// generation of a random number in order to help with the use of the cost
	private double randomTaskNumber() {
		Random r = new Random();
		int rangeMin = 0 ;
		int rangeMax = 1000;
		double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
		return randomValue;
	}


	private double calculationWorkflowFailureTime(KairosDbClient client) throws InterruptedException {

		// in this implementation the workflowFailureTime equals with
		// the total instance workflow failure TIme
//		Subscriber sub = new Subscriber();
		Subscriber sub = new Subscriber(this.publisherUrl);
		double totalDownTime = 0;
		
		// put of the task totalDownTimeMetrics
		sub.getSubscriberTotalDownTimeMetrics();
		Map<String,Double> totalDownTimeValuesMap = sub.getTotalDownTimesMetrics();
		System.out.println("The TotalDownTime has length equal to : " + totalDownTimeValuesMap.size());
		
	    Iterator it = totalDownTimeValuesMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        Double value = Double.parseDouble(pair.getValue().toString());
	        totalDownTime = totalDownTime + value;
//	        it.remove(); // avoids a ConcurrentModificationException
	    }
		System.out.println("The totalDownTime for the workflow equals with : " + totalDownTime);
	    return totalDownTime;
		
	}


	private void calculationTaskFailureRate(String workflowNameMetric, KairosDbClient client) throws Exception {

//		Subscriber sub = new Subscriber();
		Subscriber sub = new Subscriber(this.publisherUrl);

		// get mtbf values
		sub.getSubscriberMtbfMetrics();
		Map<String,Double> mtbfValuesMap = sub.getMtbfMetrics();
		System.out.println("To put the failure rate for the tasks we have the mtbf has length equal to : " + mtbfValuesMap.size());
		double failureRate = 0;
		
		// put the failure Rate Values
		Iterator it = mtbfValuesMap.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        System.out.println(pair.getKey() + " = " + pair.getValue());
		        Double value = Double.parseDouble(pair.getValue().toString());
		        String metricName = pair.getKey().toString();
		        metricName = metricName.replace("_mtbf","");
		        metricName = metricName.replace("reliability", "");
		        metricName = "task_"+ metricName + "_failureRate";
		        failureRate = 1/value;
		        System.out.println("The name that is going to be put is" + metricName +" with value :"+ 1/value);
		        System.out.println("Metric to be put is : " + workflowNameMetric + "_" + metricName);
		        client.putAlreadyInstantiateMetric(workflowNameMetric + "_" +metricName, new Date().getTime(), failureRate);
		        centralClient.putAlreadyInstantiateMetric(workflowNameMetric + "_" +metricName, new Date().getTime(), failureRate);
		 //       it.remove(); // avoids a ConcurrentModificationException
		    }
	}


	private double calculateWorkflowAvailability(Map<String, Double> taskAvailabilityMetrics) {
		
		double workflowAvailability =0;
		
	    Iterator it = taskAvailabilityMetrics.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        Double value = Double.parseDouble(pair.getValue().toString());
	        if( workflowAvailability == 0)
	        	workflowAvailability = value;
	        else
	        workflowAvailability = workflowAvailability * value;
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    return workflowAvailability;
	}

	private Map<String,Double>  subscribeMtbfTotalUpTimesNumOfBreakDowns(String workflowNameMetric, KairosDbClient client) throws Exception {

//		Subscriber sub = new Subscriber();
		Subscriber sub = new Subscriber(this.publisherUrl);

		// put of the mtbf values
		sub.getSubscriberMtbfMetrics();
		Map<String,Double> mtbfValuesMap = sub.getMtbfMetrics();
		System.out.println("The mtbf has length equal to : " + mtbfValuesMap.size());
		printReliabilityMapDoubles(workflowNameMetric ,mtbfValuesMap, client);
		
		// put of the task totalUpTimeMetrics
		sub.getSubscriberTotalUpTimeMetrics();
		Map<String,Double> totalUpTimeValuesMap = sub.getTotalUpTimesMetrics();
		System.out.println("The TotalUpTime has length equal to : " + totalUpTimeValuesMap.size());
		printReliabilityMapLongs(workflowNameMetric, totalUpTimeValuesMap, client);
		
		// put of the task totalDownTimeMetrics
		sub.getSubscriberTotalDownTimeMetrics();
		Map<String,Double> totalDownTimeValuesMap = sub.getTotalDownTimesMetrics();
		System.out.println("The TotalDownTime has length equal to : " + totalDownTimeValuesMap.size());
		printReliabilityMapLongs(workflowNameMetric, totalDownTimeValuesMap, client);
		
		// put of the task totalBreakDownMetrics
		sub.getSubscriberTotalBreakDownMetrics();
		Map<String,Double> breakDownMap = sub.getBreakDownMetrics();
		System.out.println("The BreakDown has length equal to : " + breakDownMap.size());
		printReliabilityMapDoubles(workflowNameMetric,breakDownMap, client);
		
		// calculation and put of the mttrValues
		// take the values again for totalDownTimeValuesMap AND for breakDownMap cause we removed them for reasons of cuncurrency!!!!
		sub.getSubscriberTotalDownTimeMetrics();
		totalDownTimeValuesMap = sub.getTotalDownTimesMetrics();
		sub.getSubscriberTotalBreakDownMetrics();
		breakDownMap = sub.getBreakDownMetrics();
		Map<String,Double> mttrValuesMap = calculateMttrValues(totalDownTimeValuesMap, breakDownMap);
		System.out.println("The mttr values has length equal to  : " + mttrValuesMap.size());
		printReliabilityMapDoubles(workflowNameMetric, mttrValuesMap, client);
		
		return mttrValuesMap;
	}

	private Map<String, Double>  calculationAndPutOfTaskAvailabilityMetrics(String workflowNameMetric, Map<String, Double> mttrValues, KairosDbClient client) throws Exception {
		

		Map<String, Double> taskAvailabilityValues = new HashMap<String,Double>();
		
		 final String prefixMetricNames[] = {
		          "UserPage","PermAdminPage","ObjectPage","OuUserPage","RolePage","UserDetailPage",
		          "PermPage","ObjectAdminPage","RoleAdminPage","OuPermPage",
		          "SdDynamicPage","SdStaticPage"
			 };
		
//		Subscriber sub = new Subscriber();
		Subscriber sub = new Subscriber(this.publisherUrl);

		// get of the mtbf values
		sub.getSubscriberMtbfMetrics();
		Map<String,Double> mtbfValuesMap = sub.getMtbfMetrics();
		System.out.println("The mtbf has length equal to : " + mtbfValuesMap.size());

	
		double mtbf=0;
		double mttr =0;
		double availabilityValue =0 ;
		for(int i =0; i< prefixMetricNames.length; i++){
			if(mtbfValuesMap.containsKey("reliability"+prefixMetricNames[i]+"_mtbf") && mttrValues.containsKey("reliability"+prefixMetricNames[i]+"_mttr")){
			mtbf = mtbfValuesMap.get("reliability"+prefixMetricNames[i]+"_mtbf");
			mttr = mttrValues.get("reliability"+prefixMetricNames[i]+"_mttr");
			// two more puts of mttr and mtbf
			availabilityValue = mtbf / (mtbf+mttr);
			taskAvailabilityValues.put(workflowNameMetric+"_task_"+prefixMetricNames[i]+"_availability", availabilityValue);
			System.out.println("TaskAvailability with name :" +workflowNameMetric+"_task_"+prefixMetricNames[i]+"_availability"+ " and value : " + availabilityValue );
			client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_"+prefixMetricNames[i]+"_availability", new Date().getTime(), availabilityValue);
			centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_task_"+prefixMetricNames[i]+"_availability", new Date().getTime(), availabilityValue);
			}
		
		}
		return taskAvailabilityValues;
	}
	
	private Map<String, Double> calculateMttrValues(Map<String, Double> totalDownTimesMetrics,Map<String, Double> breakDownMap) {
		
		
		 final String prefixMetricNames[] = {
		          "UserPage","PermAdminPage","ObjectPage","OuUserPage","RolePage","UserDetailPage",
		          "PermPage","ObjectAdminPage","RoleAdminPage","OuPermPage",
		          "SdDynamicPage","SdStaticPage"
			 };
		 
//		System.out.println("We will print the maps in order to check what they include");
//		printMap(totalDownTimesMetrics);
//		printMap(breakDownMap); 
		
		Map<String,Double> mttrValuesMap = new HashMap<String,Double>();
		
		Double downTime;
		double breakDowns;
		//strict value for humanUnavailability
		double humanUnavailability= 0.35;
		double mttr;
		for(int i=0 ; i< prefixMetricNames.length; i++){
		
			if(totalDownTimesMetrics.containsKey("reliability"+prefixMetricNames[i]+"_totalDownTime") 
					&& breakDownMap.containsKey("reliability"+prefixMetricNames[i]+"_nBreakDowns")){
			downTime = totalDownTimesMetrics.get("reliability"+prefixMetricNames[i]+"_totalDownTime");
			breakDowns = breakDownMap.get("reliability"+prefixMetricNames[i]+"_nBreakDowns");
			
			mttr = downTime +humanUnavailability / breakDowns;
			mttrValuesMap.put("reliability"+prefixMetricNames[i]+"_mttr", mttr);
			System.out.println("Mttr with name :" +"reliability"+prefixMetricNames[i]+"_mttr" + " and value : " + mttr );
			}
		}
		return mttrValuesMap;
	}


	// method to get the securityMetrics : DependencyAggregator
	private double getWorkflowAuthenticationSecurity(String workflowName, KairosDbClient client) throws Exception {

//		KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");
//		Subscriber sub = new Subscriber();
		
		Subscriber sub = new Subscriber(this.publisherUrl);

		sub.getSubscriberSecurityMetrics();
		Map<String,Double> securityValuesMap = sub.getSecurityMetrics();
		System.out.println("The securityMap has length equal to : " + securityValuesMap.size());
		
		double failedLogIn = securityValuesMap.get("failedAuthentications");
		client.putAlreadyInstantiateMetric(workflowName+ "_taskFailedAuthSecurity", new Date().getTime(), failedLogIn);
		centralClient.putAlreadyInstantiateMetric(workflowName+ "_taskFailedAuthSecurity", new Date().getTime(), failedLogIn);

		System.out.println("failedLogIn equals with : " + failedLogIn);
		
		
		double totalAttemptsOfAuth = securityValuesMap.get("totalAttemptsOfAuthentication");
		client.putAlreadyInstantiateMetric(workflowName + "_totalAttemptsOfAuthentication", new Date().getTime(), totalAttemptsOfAuth);
		centralClient.putAlreadyInstantiateMetric(workflowName + "_totalAttemptsOfAuthentication", new Date().getTime(), totalAttemptsOfAuth);
		System.out.println("Total Attempts of Auth equals with : " + totalAttemptsOfAuth);
		

		double successLogIn = totalAttemptsOfAuth - failedLogIn  ;
		client.putAlreadyInstantiateMetric(workflowName + "_taskSuccessAuthSecurity", new Date().getTime(), successLogIn);
		centralClient.putAlreadyInstantiateMetric(workflowName + "_taskSuccessAuthSecurity", new Date().getTime(), successLogIn);
		System.out.println("totalSuccessLogInAttempts equals with : " + successLogIn);
		
		
		double taskVulAuth = 0; // SHOULD BE TAKEN FROM THE EXTERNAL SERVICE OF SECURITY
		//for us the task authentication security is the logIn of the system so we adapt to that
		double authenticationSecutiry = 0;
		if(failedLogIn > 0)
		authenticationSecutiry = totalAttemptsOfAuth / failedLogIn + taskVulAuth ;
		System.out.println("fortress_taskAuthSecurity equals with : " + authenticationSecutiry);
		client.putAlreadyInstantiateMetric(workflowName+"_taskAuthSecutiry", new Date().getTime(), authenticationSecutiry);
		centralClient.putAlreadyInstantiateMetric(workflowName+"_taskAuthSecutiry", new Date().getTime(), authenticationSecutiry);

		return authenticationSecutiry;
	}

	// method to take the delay time from subscriber
	private double setTaskDelayTimes(String workflowNameMetric, DelegateExecution execution, KairosDbClient client) throws Exception {
		// TODO Auto-generated method stub
		// Here we are going to set the TaskDelayTimes 
		// for us will be 
		// ManagementDelayTime as the WaitTimeInMilliseconds
		// and QueuingDelayTime as the LockTimeInMilliseconds
//		Subscriber sub = new Subscriber();
		Subscriber sub = new Subscriber(this.publisherUrl);

		//test of the subscriber for all the values
		sub.getSubscriberDelayMetrics();
		Map<String,Double> delayValuesMap = sub.getStartEndDelayMetrics();
		System.out.println("The delayMap has length equal to : " + delayValuesMap.size());
		
		// first put the process Delay Time to kairosDB 
		putProcessDelayTime(workflowNameMetric, delayValuesMap, client);
		// second put each of the raw metrics to KairosDB 
		double delayWorkflowTime = printTaskDelayMap(workflowNameMetric, delayValuesMap, client);
		return delayWorkflowTime;
	}


	// calculation of the taskProcessDelayTimes : DependencyAggregator
	private void putProcessDelayTime(String workflowNameMetric, Map<String, Double> delayValuesMap, KairosDbClient client) throws Exception {

		 final String prefixMetricNames[] = {
	          "user","permAdmin","object","ouUser","role","userDetail",
	          "perm","objectAdmin","roleAdmin","ouPerm",
	          "sdDynamic","sdStatic"
		 };
		
//		 KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");
		 
		 double startEntityDelay = 0.0;
		 double endEntityDelay = 0.0;
		 double taskProcessTime;
		 
		 for(int i=0; i< prefixMetricNames.length; i++){
			 
			 if(delayValuesMap.containsKey(prefixMetricNames[i]+"Delay_latency_milliseconds") || 
					 delayValuesMap.containsKey(prefixMetricNames[i]+"AnswerDelay_latency_milliseconds")){
			 
			 System.out.println("The prefixMetrixName for the latency is  : " + prefixMetricNames[i]);
			 if(delayValuesMap.get(prefixMetricNames[i]+"Delay_latency_milliseconds") != null)
			 startEntityDelay = delayValuesMap.get(prefixMetricNames[i]+"Delay_latency_milliseconds");
			 if(delayValuesMap.get(prefixMetricNames[i]+"AnswerDelay_latency_milliseconds") != null)
			 endEntityDelay = delayValuesMap.get(prefixMetricNames[i]+"AnswerDelay_latency_milliseconds");
			 taskProcessTime = startEntityDelay + endEntityDelay;
			 String modifiedName;
			 if(prefixMetricNames[i].equals("user"))
				 modifiedName = "UserPage";
			 else if (prefixMetricNames[i].equals("permAdmin"))
				 modifiedName = "PermAdminPage";
			 else if (prefixMetricNames[i].equals("object"))
				 modifiedName = "ObjectPage";
			 else if (prefixMetricNames[i].equals("ouUser"))
				 modifiedName = "OuUserPage";
			 else if (prefixMetricNames[i].equals("role"))
				 modifiedName = "RolePage";
			 else if (prefixMetricNames[i].equals("userDetail"))
				 modifiedName = "UserDetailPage";
			 else if (prefixMetricNames[i].equals("objectAdmin"))
				 modifiedName = "ObjectAdminPage";
			 else if (prefixMetricNames[i].equals("roleAdmin"))
				 modifiedName = "RoleAdminPage";
			 else if (prefixMetricNames[i].equals("ouPerm"))
				 modifiedName = "OuPermPage";
			 else if (prefixMetricNames[i].equals("sdDynamic"))
				 modifiedName = "SdDynamicPage";
			 else if (prefixMetricNames[i].equals("sdStatic"))
				 modifiedName = "SdStaticPage";
			 else if (prefixMetricNames[i].equals("perm"))
				 modifiedName = "PermPage";
			 else
				 modifiedName= prefixMetricNames[i];
			 
			 System.out.println("Metric to be put is : " + workflowNameMetric+"_task_"+modifiedName+"_delayProcessTask and with value  : " + taskProcessTime);
		 	 client.putAlreadyInstantiateMetric(workflowNameMetric+"_task_"+modifiedName+"_delayProcessTask", new Date().getTime(), taskProcessTime);
			 centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_task_"+modifiedName+"_delayProcessTask", new Date().getTime(), taskProcessTime);

			 }
		 }
		 
	}


	// method putting the task execution times WILL NEED EDITING ONCE we ARE GETTING THE TIMES FROM PROMETHEUS IN FAVOR OF THE TASK 
	// DEPECTING TO EXTERNAL SERVICE AS THE fortress-web
	private double setTaskDurationTimes(String workflowNameMetric, ArrayList<HistoricActivityInstance> inq, KairosDbClient client) throws Exception {
		String activityType;
		String activityName;
		durationTaskTimes =  new HashMap<String,Long>();
		
		for(int i=0; i<inq.size(); i++){
			activityType = inq.get(i).getActivityType();
			if(activityType.equals("serviceTask") || activityType.equals("userTask") || activityType.equals("scriptTask")){
				activityName = inq.get(i).getActivityName();
				System.out.println("Activity with name :" + activityName + " had duration of->" + inq.get(i).getDurationInMillis() );
				durationTaskTimes.put(activityName, inq.get(i).getDurationInMillis());
			}
		}
		
		double taskDurWorflow = printDurMap(durationTaskTimes, workflowNameMetric, client);
		
		return taskDurWorflow;
	}
	
	

	// method for putting the transition delay times
	private double setTransitionDelayTimes(String workflowNameMetric, ArrayList<String> flows, ArrayList<HistoricActivityInstance> inq, KairosDbClient client) throws Exception {
		transitionDelayTimes = new HashMap<String,Long>();
		Date startTime;
		Date endTime;
		long transitionDelay;
		Date beforeEnd = inq.get(inq.size()-1).getEndTime();
		long lastTransitionDelayTime = startTimeEndEvent - beforeEnd.getTime();
		
		for(int i=0; i<flows.size(); i++){
			if(!(i+1 >= flows.size())){
			startTime = inq.get(i+1).getStartTime();
			endTime = inq.get(i).getEndTime();
			transitionDelay = startTime.getTime() - endTime.getTime();
			System.out.println("The transitionDelayTime is : " + transitionDelay + " with flow name: "+ flows.get(i) );
			transitionDelayTimes.put(flows.get(i), transitionDelay);
			}
		}
		// and put the last transition delay time in order to reach the endEvent
		transitionDelayTimes.put(flows.get(flows.size()-1),lastTransitionDelayTime);
//		System.out.println("The last transitionDelayTime before the endEvent is : " + lastTransitionDelayTime );
		double transWorflow = printTDelayMap(transitionDelayTimes, workflowNameMetric, client);
		
		return transWorflow;
	}
	
	public static void printMap(Map mp) {
		
	    Iterator it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	}
	
	

	private void printReliabilityMapDoubles(String workflowNameMetric, Map<String, Double> reliabilityMap, KairosDbClient client) throws Exception {
	
//		KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");

	    Iterator it = reliabilityMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        String metricName = workflowNameMetric+"_task_"+pair.getKey().toString();
	        metricName = metricName.replace("reliability", "");
	        System.out.println("Metric to be put equals with : " +metricName + " and value equal with : " + pair.getValue());
	        client.putAlreadyInstantiateMetric(metricName, new Date().getTime(), pair.getValue());
	        centralClient.putAlreadyInstantiateMetric(metricName, new Date().getTime(), pair.getValue());

	        Double value = Double.parseDouble(pair.getValue().toString());
	 //       it.remove(); // avoids a ConcurrentModificationException
	    }
		
	}
	
	private void printReliabilityMapLongs(String workflowNameMetric, Map<String, Double> reliabilityMap, KairosDbClient client) throws Exception {
		
//		KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");

	    Iterator it = reliabilityMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        String metricName = workflowNameMetric+"_task_"+pair.getKey().toString();
	        metricName = metricName.replace("reliability", "");
	        System.out.println("Metric going to be put equalss with  : " + metricName + " and the value equals with : " + pair.getValue());
	        client.putAlreadyInstantiateMetric(metricName, new Date().getTime(), pair.getValue());
	        centralClient.putAlreadyInstantiateMetric(metricName, new Date().getTime(), pair.getValue());

	        Double value = Double.parseDouble(pair.getValue().toString());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
		
	}
	
	// this method was static before
	private double printTaskDelayMap(String workflowNameMetric, Map<String, Double> delayValuesMap, KairosDbClient client) throws Exception {

//		KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");

		final String prefixMetricNames[] = {
		          "UserPage","PermAdminPage","ObjectPage","OuUserPage","RolePage","UserDetailPage",
		          "PermPage","ObjectAdminPage","RoleAdminPage","OuPermPage",
		          "SdDynamicPage","SdStaticPage"
			 };
		
		double workflowDelayTime =0;
		
	    Iterator it = delayValuesMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        String modifiedString = pair.getKey().toString();
//	        for(int i=0; i<prefixMetricNames.length; i++){
//	        	if(modifiedString.contains(prefixMetricNames[i]))
//	        			modifiedString = prefixMetricNames[i];
//	        }
	        System.out.println("The metric to be put is : " + workflowNameMetric+"_"+modifiedString+" and with value : " +pair.getValue() );
	        client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+modifiedString, new Date().getTime(), pair.getValue());
	        centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+modifiedString, new Date().getTime(), pair.getValue());
	        Double value = Double.parseDouble(pair.getValue().toString());
	        workflowDelayTime = workflowDelayTime + value;
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    return workflowDelayTime;	
	}
	
	
	// this method was static before
	public double printTDelayMap(Map mp, String workflowNameMetric, KairosDbClient client) throws Exception {
	
//		KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");

		double workflowTransitionDelayTime =0;
		
	    Iterator it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
//	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        System.out.println("Metric to be put is : " +workflowNameMetric+"_"+pair.getKey().toString()+"_TransitionDelayTime with value : " + pair.getValue());
	        client.putAlreadyInstantiateMetric(workflowNameMetric+"_"+pair.getKey().toString()+"_TransitionDelayTime", new Date().getTime(), pair.getValue());
	        centralClient.putAlreadyInstantiateMetric(workflowNameMetric+"_"+pair.getKey().toString()+"_TransitionDelayTime", new Date().getTime(), pair.getValue());
	        Double value = Double.parseDouble(pair.getValue().toString());
	        workflowTransitionDelayTime = workflowTransitionDelayTime + value;
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    return workflowTransitionDelayTime;
	}
	
	
	// this method was static before
	public double printDurMap(Map mp, String workflowNameMetric, KairosDbClient client) throws Exception {
		
//		KairosDbClient centralClient = new KairosDbClient("http://147.52.82.63:8088/");

		 final String prefixMetricNames[] = {
		          "UserPage","PermAdminPage","ObjectPage","OuUserPage","RolePage","UserDetailPage",
		          "PermPage","ObjectAdminPage","RoleAdminPage","OuPermPage",
		          "SdDynamicPage","SdStaticPage"
			 };
		
		double workflowExecutionTime =0;
		
	    Iterator it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        //String needs some modification replaces whitesoaces and gices a specific prefix metric
	        String modifiedName = pair.getKey().toString().replaceAll("\\s+","").replace("SearchingFunctionallityin", "");// this value is taken in situation where the task is not a service one
	        for(int i =0; i < prefixMetricNames.length; i++)
	        {
	        	if( modifiedName.contains(prefixMetricNames[i]))
	        		modifiedName = prefixMetricNames[i];
	        }
	        System.out.println("Metric to be put is : " +workflowNameMetric +"_task_"+ modifiedName + "_exec and with vale : " + pair.getValue());
	        client.putAlreadyInstantiateMetric(workflowNameMetric +"_task_"+ modifiedName + "_exec", new Date().getTime(), pair.getValue());
	        centralClient.putAlreadyInstantiateMetric(workflowNameMetric +"_task_"+ modifiedName + "_exec", new Date().getTime(), pair.getValue());

	        Double value = Double.parseDouble(pair.getValue().toString());
	        workflowExecutionTime = workflowExecutionTime + value;
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    return workflowExecutionTime;
	}
	
	
	public void printSubscriberValues() throws InterruptedException{
		
//		Subscriber sub = new Subscriber();
		Subscriber sub = new Subscriber(this.publisherUrl);

		sub.getSubscriberSecurityMetrics();
		Map<String,Double> securityValuesMap = sub.getSecurityMetrics();
		System.out.println("The securityMap has length equal to : " + securityValuesMap.size());
		printMap(securityValuesMap);
		
		
		sub.getSubscriberMtbfMetrics();
		Map<String,Double> mtbfValuesMap = sub.getMtbfMetrics();
		System.out.println("The mtbf has length equal to : " + mtbfValuesMap.size());
		printMap(mtbfValuesMap);
		
		
		sub.getSubscriberTotalUpTimeMetrics();
		Map<String,Double> totalUpTimeValuesMap = sub.getTotalUpTimesMetrics();
		System.out.println("The TotalUpTime has length equal to : " + totalUpTimeValuesMap.size());
		printMap(totalUpTimeValuesMap);

		
		sub.getSubscriberTotalDownTimeMetrics();
		Map<String,Double> totalDownTimeValuesMap = sub.getTotalDownTimesMetrics();
		System.out.println("The TotalDownTime has length equal to : " + totalDownTimeValuesMap.size());
		printMap(totalDownTimeValuesMap);
		
		
		sub.getSubscriberTotalBreakDownMetrics();
		Map<String,Double> breakDownMap = sub.getBreakDownMetrics();
		System.out.println("The BreakDown has length equal to : " + breakDownMap.size());
		printMap(breakDownMap);
		
	}
	
}
 