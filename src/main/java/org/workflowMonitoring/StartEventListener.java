package org.workflowMonitoring;

import java.util.Date;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class StartEventListener implements ExecutionListener{

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		
		 Context context = ZMQ.context(1);
	     Socket publisher = context.socket(ZMQ.PUB);

	     publisher.bind("tcp://192.168.254.132:5564");	
	     int k =0;
	     // it will run for 3 seconds
	     while (!Thread.currentThread ().isInterrupted ()) {
//	            System.out.println("We are sedning the values");
	    	 	// we send the name of the workflow and then the start time of it
	            publisher.sendMore ("workflowMonitoringExecution14");
	            publisher.send (String.valueOf(new Date().getTime()));
	            k++;
	            if(k == 100000)
	            	break;
	     }
	     
	     publisher.close ();
	     context.term ();
		
	}

}
