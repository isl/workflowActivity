package org.workflowMonitoring;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * Created by dmetallidis on 9/8/16.
 */
public class Subscriber {


	private static Map<String,Double> startEndEntityDelayMetrics;
	private static Map<String,Double> securityMetrics;
	private static Map<String,Double> mtbfMetrics;
	private static Map<String,Double> totalUpTimes;
	private static Map<String,Double> totalDownTimes;
	private static Map<String,Double> breakDowns;

	private String url;
	
	public Subscriber(String url){
		this.url = url;
	}
	
	public Subscriber(){
		this.url = "tcp://localhost:5563/";
	}
	

	// getter for the start and End Delay Metrics
	public Map<String,Double> getStartEndDelayMetrics(){
		return startEndEntityDelayMetrics;
	}
	
	// getter for the security Metrics
	public Map<String,Double> getSecurityMetrics(){
		return securityMetrics;
	}
	
	// getter for the mtbf Metrics
	public Map<String,Double> getMtbfMetrics(){
		return mtbfMetrics;
	}
	
	// getter for the totalUpTimes metrics for the tasks
	public Map<String,Double> getTotalUpTimesMetrics(){
		return totalUpTimes;
	}
	
	// getter for the totalDownTimes metrics for the tasks
	public Map<String,Double> getTotalDownTimesMetrics(){
		return totalDownTimes;
	}
	
	// getter for the totalDownTimes metrics for the tasks
	public Map<String,Double> getBreakDownMetrics(){
		return breakDowns;
	}
	
	  public static final String rawStartAndEndEntityDelayMetricNames[] = {
          "userDelay_latency_milliseconds", "userAnswerDelay_latency_milliseconds",
          "permAdminDelay_latency_milliseconds", "permAdminAnswerDelay_latency_milliseconds",
          "objectDelay_latency_milliseconds", "objectAnswerDelay_latency_milliseconds",
          "ouUserDelay_latency_milliseconds", "ouUserAnswerDelay_latency_milliseconds",
          "roleDelay_latency_milliseconds", "roleAnswerDelay_latency_milliseconds",
          "userDetailDelay_latency_milliseconds", "userDetailAnswerDelay_latency_milliseconds",
          "permDelay_latency_milliseconds", "permAnswerDelay_latency_milliseconds",
          "objectAdminDelay_latency_milliseconds", "objectAdminAnswerDelay_latency_milliseconds",
          "roleAdminDelay_latency_milliseconds", "roleAdminAnswerDelay_latency_milliseconds",
          "ouPermDelay_latency_milliseconds", "ouPermAnswerDelay_latency_milliseconds",
          "sdDynamicDelay_latency_milliseconds", "sdDynamicAnswerDelay_latency_milliseconds",
          "sdStaticDelay_latency_milliseconds", "sdStaticAnswerDelay_latency_milliseconds"
  };

  public static final String rawSecurityMetrics[] = {
          "failedAuthentications",
          "totalAttemptsOfAuthentication"};

  public static final String prefixesReliabilityOfServices_mtbf [] = {"reliabilityUserPage_mtbf", "reliabilityPermAdminPage_mtbf", "reliabilityObjectPage_mtbf",
          "reliabilityOuUserPage_mtbf", "reliabilityRolePage_mtbf", "reliabilityUserDetailPage_mtbf", 
          "reliabilityPermPage_mtbf", "reliabilityObjectAdminPage_mtbf", "reliabilityRoleAdminPage_mtbf"
          , "reliabilityOuPermPage_mtbf", "reliabilitySdDynamicPage_mtbf", "reliabilitySdStaticPage_mtbf"};

  public static final String prefixesReliabilityOfServices_totalUpTime [] = {"reliabilityUserPagetotalUpTime",
      "reliabilityPermAdminPagetotalUpTime", "reliabilityObjectPagetotalUpTime",
      "reliabilityOuUserPagetotalUpTime", "reliabilityRolePagetotalUpTime", "reliabilityUserDetailPagetotalUpTime",
      "reliabilityPermPagetotalUpTime", "reliabilityObjectAdminPagetotalUpTime", "reliabilityRoleAdminPagetotalUpTime"
      , "reliabilityOuPermPagetotalUpTime", "reliabilitySdDynamicPagetotalUpTime", "reliabilitySdStaticPagetotalUpTime"};

  public static final String prefixesReliabilityOfServices_totalDownTime [] = {"reliabilityUserPagetotalDownTime",
      "reliabilityPermAdminPagetotalDownTime", "reliabilityObjectPagetotalDownTime",
      "reliabilityOuUserPagetotalDownTime", "reliabilityRolePagetotalDownTime", "reliabilityUserDetailPagetotalDownTime",
      "reliabilityPermPagetotalDownTime", "reliabilityObjectAdminPagetotalDownTime", "reliabilityRoleAdminPagetotalDownTime"
      , "reliabilityOuPermPagetotalDownTime", "reliabilitySdDynamicPagetotalDownTime", "reliabilitySdStaticPagetotalDownTime"};
  
  public static final String prefixesReliabilityOfServices_nBreakDowns [] = {"reliabilityUserPagenBreakDowns",
      "reliabilityPermAdminPagenBreakDowns", "reliabilityObjectPagenBreakDowns",
      "reliabilityOuUserPagenBreakDowns", "reliabilityRolePagenBreakDowns", "reliabilityUserDetailPagenBreakDowns",
      "reliabilityPermPagenBreakDowns", "reliabilityObjectAdminPagenBreakDowns", "reliabilityRoleAdminPagenBreakDowns"
      , "reliabilityOuPermPagenBreakDowns", "reliabilitySdDynamicPagenBreakDowns", "reliabilitySdStaticPagenBreakDowns"};


    public static void main (String[] args) throws Exception {

        // Prepare our context and subscriber
        Context context = ZMQ.context(1);
        Socket subscriber = context.socket(ZMQ.SUB);

        subscriber.connect("tcp://192.168.254.134:5563");
        subscriber.subscribe("workflowMonitoringExecution14".getBytes());
       
        
//		KairosDbClient client = new KairosDbClient("http://147.52.82.63:8088");
//
//		client.putAlreadyInstantiateMetric("exampleMetric3", new Date().getTime(), 934.3);
//		System.out.println("I have put the metrics inside the TSBD");
		
        while (!Thread.currentThread ().isInterrupted ()) {
            // Read envelope with address
            String address = subscriber.recvStr ();
            // Read message contents
            String contents = subscriber.recvStr ();
            System.out.println(address + " : " + contents);
        }
        subscriber.close ();
        context.term ();
    }
    
    public void getSubscriberDelayMetrics() throws InterruptedException{
    	
    	  // Prepare our context and subscriber
        Context context = ZMQ.context(1);
        final Socket subscriber = context.socket(ZMQ.SUB);

        
//        subscriber.connect("tcp://localhost:5563");
       subscriber.connect(this.url);

        
        //example subscriber
        subscriber.subscribe("A".getBytes());
        for (int i=0; i<rawStartAndEndEntityDelayMetricNames.length; i++)
        	subscriber.subscribe(rawStartAndEndEntityDelayMetricNames[i].getBytes());
        startEndEntityDelayMetrics = new HashMap<String,Double>();
        
        
      System.out.println("StartEndDelay Subscriber is going to run");

      // time of subscription in order to take the metrics
      long t = System.currentTimeMillis();
      long end = t + 40000;
      while (!Thread.currentThread ().isInterrupted () && System.currentTimeMillis() < end) {
        	        	// Read envelope with address
        	            String address = subscriber.recvStr ();
//        	            // Read message contents
        	            String contents = subscriber.recvStr ();
        	            System.out.println(address + " : " + contents);  
        	            startEndEntityDelayMetrics.put(address, Double.parseDouble(contents));
        	            try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
			}
      }
       
      subscriber.close ();
      context.term ();
      System.out.println("I have gotten the according delay metrics");
    }
    
    public void getSubscriberSecurityMetrics() throws InterruptedException{
    	
    	Context context = ZMQ.context(1);
        final Socket subscriber = context.socket(ZMQ.SUB);

//        subscriber.connect("tcp://localhost:5563");
        subscriber.connect(this.url);

        for (int i=0; i<rawSecurityMetrics.length; i++)
        	subscriber.subscribe(rawSecurityMetrics[i].getBytes());
       
       securityMetrics = new HashMap<String,Double>();
         
       System.out.println("Security Subscriber is going to run");

       // time of subscription in order to take the metrics
       long t = System.currentTimeMillis();
       long end = t + 40000;
       while (!Thread.currentThread ().isInterrupted () && System.currentTimeMillis() < end) {
         	        	// Read envelope with address
         	            String address = subscriber.recvStr ();
         	            // Read message contents
         	            String contents = subscriber.recvStr ();
         	            System.out.println(address + " : " + contents);  
         	           securityMetrics.put(address, Double.parseDouble(contents));
         	            try {
 							Thread.sleep(3000);
 						} catch (InterruptedException e) {
 							// TODO Auto-generated catch block
 							e.printStackTrace();
 			}
       }
        
       subscriber.close ();
       context.term ();
       System.out.println("I have gotten the according security metrics");
    
    }
    
    public void getSubscriberMtbfMetrics() throws InterruptedException{
    	
    	Context context = ZMQ.context(1);
        final Socket subscriber = context.socket(ZMQ.SUB);

//      subscriber.connect("tcp://localhost:5563");
        subscriber.connect(this.url);

        for (int i=0; i<prefixesReliabilityOfServices_mtbf.length; i++)
        	subscriber.subscribe(prefixesReliabilityOfServices_mtbf[i].getBytes());
       
        mtbfMetrics = new HashMap<String,Double>();
         
       System.out.println("Mtbf Subscriber is going to run");

       // time of subscription in order to take the metrics
       long t = System.currentTimeMillis();
       long end = t + 40000;
       while (!Thread.currentThread ().isInterrupted () && System.currentTimeMillis() < end) {
         	        	// Read envelope with address
         	            String address = subscriber.recvStr ();
         	            // Read message contents
         	            String contents = subscriber.recvStr ();
         	            System.out.println(address + " : " + contents);  
         	           mtbfMetrics.put(address, Double.parseDouble(contents));
         	            try {
 							Thread.sleep(3000);
 						} catch (InterruptedException e) {
 							// TODO Auto-generated catch block
 							e.printStackTrace();
 			}
       }
       subscriber.close ();
       context.term ();
       System.out.println("I have gotten the according mtbf metrics");
    }
    
    public void getSubscriberTotalUpTimeMetrics() throws InterruptedException{
    	
    	Context context = ZMQ.context(1);
        final Socket subscriber = context.socket(ZMQ.SUB);

//        subscriber.connect("tcp://localhost:5563");
        subscriber.connect(this.url);

        for (int i=0; i<prefixesReliabilityOfServices_totalUpTime.length; i++)
        	subscriber.subscribe(prefixesReliabilityOfServices_totalUpTime[i].getBytes());
       
        totalUpTimes = new HashMap<String,Double>();
         
       System.out.println("TotalUpTimes Subscriber is going to run");

       // time of subscription in order to take the metrics
       long t = System.currentTimeMillis();
       long end = t + 40000;
       while (!Thread.currentThread ().isInterrupted () && System.currentTimeMillis() < end) {
         	        	// Read envelope with address
         	            String address = subscriber.recvStr ();
         	            // Read message contents
         	            String contents = subscriber.recvStr ();
         	            System.out.println(address + " : " + contents);  
         	           totalUpTimes.put(address, Double.parseDouble(contents));
         	            try {
 							Thread.sleep(3000);
 						} catch (InterruptedException e) {
 							// TODO Auto-generated catch block
 							e.printStackTrace();
 			}
       }
       subscriber.close ();
       context.term ();
       System.out.println("I have gotten the according totalUpTimes metrics");
    
    }

    public void getSubscriberTotalDownTimeMetrics() throws InterruptedException{
    	
    	Context context = ZMQ.context(1);
        final Socket subscriber = context.socket(ZMQ.SUB);

//        subscriber.connect("tcp://localhost:5563");
        subscriber.connect(this.url);

        for (int i=0; i<prefixesReliabilityOfServices_totalDownTime.length; i++)
        	subscriber.subscribe(prefixesReliabilityOfServices_totalDownTime[i].getBytes());
       
       totalDownTimes = new HashMap<String,Double>();
         
       System.out.println("TotalDownTimes Subscriber is going to run");

       // time of subscription in order to take the metrics
       long t = System.currentTimeMillis();
       long end = t + 40000;
       while (!Thread.currentThread ().isInterrupted () && System.currentTimeMillis() < end) {
         	        	// Read envelope with address
         	            String address = subscriber.recvStr ();
         	            // Read message contents
         	            String contents = subscriber.recvStr ();
         	            System.out.println(address + " : " + contents);  
         	           totalDownTimes.put(address, Double.parseDouble(contents));
         	            try {
 							Thread.sleep(3000);
 						} catch (InterruptedException e) {
 							// TODO Auto-generated catch block
 							e.printStackTrace();
 			}
       }
       subscriber.close ();
       context.term ();
       System.out.println("I have gotten the according totalDownTimes metrics");
    
    }
    
    public void getSubscriberTotalBreakDownMetrics() throws InterruptedException{
    	
       Context context = ZMQ.context(1);
       final Socket subscriber = context.socket(ZMQ.SUB);

//       subscriber.connect("tcp://localhost:5563");
       subscriber.connect(this.url);

       for (int i=0; i<prefixesReliabilityOfServices_nBreakDowns.length; i++)
        	subscriber.subscribe(prefixesReliabilityOfServices_nBreakDowns[i].getBytes());
       
       breakDowns = new HashMap<String,Double>();
         
       System.out.println("BreakDown Subscriber is going to run");

       // time of subscription in order to take the metrics
       long t = System.currentTimeMillis();
       long end = t + 40000;
       while (!Thread.currentThread ().isInterrupted () && System.currentTimeMillis() < end) {
         	        	// Read envelope with address
         	            String address = subscriber.recvStr ();
         	            // Read message contents
         	            String contents = subscriber.recvStr ();
         	            System.out.println(address + " : " + contents);  
         	            breakDowns.put(address, Double.parseDouble(contents));
         	            try {
 							Thread.sleep(3000);
 						} catch (InterruptedException e) {
 							// TODO Auto-generated catch block
 							e.printStackTrace();
 			}
       }
       subscriber.close ();
       context.term ();
       System.out.println("I have gotten the according breakDown metrics");
    }
    
}
