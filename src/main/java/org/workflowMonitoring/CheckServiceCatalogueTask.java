package org.workflowMonitoring;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;

import org.activiti.engine.delegate.JavaDelegate;

import org.activiti.engine.impl.jobexecutor.DefaultJobExecutor;

public class CheckServiceCatalogueTask implements JavaDelegate {

	DefaultJobExecutor jobExecutor = new DefaultJobExecutor(); 
	Map<String,Map<Long,Long>> delayTaskTime = new HashMap<String,Map<Long,Long>>();
	
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		
//		Thread thread1 = new Thread(){
//			public void run(){
//				System.out.println("Thread one is running");
//				
//			}
//		};
//		
//		Thread thread2 = new Thread(){
//			public void run(){
//				System.out.println("Thread two is running");
//			}
//		};
//		
//		thread1.start();
//		thread2.start();
//		
//		thread1.join();
//		thread2.join();
//		System.out.println("We have passed the threads");
		
		// suppose that we have implemented an adaptive jobExecutor and we have the appropriate delay milliseconds in order to put them to the process context
		Map<Long,Long> delays = new HashMap<Long,Long>();
		delays.put(jobExecutor.getWaitTimeInMillis(), (long) jobExecutor.getLockTimeInMillis());
		delayTaskTime.put(execution.getEventName(), delays);
		execution.setVariable(execution.getEventName()+ getClass().getName() , delayTaskTime);
		
		// below is a list of supposed application/services available to use in the workflow
		ServiceCatalogueGeneral sc = new ServiceCatalogueGeneral();
		boolean isIncluded =false;
		String services[] = {"fortress-web", "e-shop", "ldap"};
		for(int i=0; i<services.length; i++)
		{
			if (services[i].equals((String) execution.getVariable("serviceName")))
			{	
				sc.setServiceName((String) execution.getVariable("serviceName"));
				execution.setVariable("isInCatalogue", true);
				isIncluded = true;
			}
		}
		
		// if the service is not included in the catalogue we give the false variables to ServiceCatalogueGeneral
		if(isIncluded == false){
			execution.setVariable("isInCatalogue", false);
			sc.setServiceName((String)execution.getVariable("serviceName"));
		}
		
		execution.setVariable("serviceCatalogueGeneral", sc);
	}

	private void jobExecutions() throws InterruptedException {
		// TODO Auto-generated method stub
		System.out.println("To waitTime einai iso me : " + jobExecutor.getWaitTimeInMillis());
		System.out.println("To queueTime einai iso me : " + jobExecutor.getLockTimeInMillis());
		
	}


	
}
