package org.workflowMonitoring;


import java.util.Date;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class sPublisher {
	
	 public static void main (String[] args) throws Exception {
	        // Prepare our context and publisher
	        Context context = ZMQ.context(1);
	        Socket publisher = context.socket(ZMQ.PUB);

	        publisher.bind("tcp://192.168.254.132:5564");
	        while (!Thread.currentThread ().isInterrupted ()) {
//	            // Write two messages, each with an envelope and content
//	            publisher.sendMore ("userDelay_latency_milliseconds");
//	            publisher.send ("802");
//	            publisher.sendMore ("userAnswerDelay_latency_milliseconds");
//	            publisher.send("198");
//	            
//	            //security
//	            publisher.sendMore ("failed_login_auth_request_total");
//	            publisher.send("10");
//	            
//	            publisher.sendMore("success_login_auth_request_total");
//	            publisher.send("20");
//
//	            // mtbf
//	            publisher.sendMore ("reliabilityUserPage_mtbf");
//	            publisher.send("1.5");
//	            
//	            // totalUpTIme
//	            publisher.sendMore ("reliabilityUserPage_totalUpTime");
//	            publisher.send("4");
//	            
//	            // totalDownTime
//	            publisher.sendMore ("reliabilityUserPage_totalDownTime");
//	            publisher.send("3");
//
//	            // breakDowns
//	            publisher.sendMore ("reliabilityUserPage_nBreakDowns");
//	            publisher.send("2");
	            System.out.println("We are sedning the values");
	    	 	// we send the name of the workflow and then the start time of it
	            publisher.sendMore("workflowMonitoringExecution14");
	            publisher.send (String.valueOf(new Date().getTime()));

	        }
	        publisher.close ();
	        context.term ();
	    }

}
