package org.workflowMonitoring;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class ServiceAvailabilityReqeustingTask implements JavaDelegate{

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		// Test the availability of the website and set the gateway variables the same way

		String host = (String) execution.getVariable("serviceIp");
		String serviceName = (String) execution.getVariable("prefixService");
		String port = (String) execution.getVariable("port");
		
		URL url = new URL ("http://"+host+":"+port+"/"+serviceName);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("HEAD");
		try{
			int code = connection.getResponseCode();
		
			if(code == 200){
			execution.setVariable("isReachable", true);
			}
		}
		catch (ConnectException e){
			execution.setVariable("isReachable", false);
		}
		
	}

}
