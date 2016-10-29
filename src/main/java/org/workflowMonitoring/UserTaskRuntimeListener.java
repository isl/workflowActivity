package org.workflowMonitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.pvm.delegate.ExecutionListenerExecution;
 
 
public class UserTaskRuntimeListener implements ExecutionListener {
 
	@Override
	public void notify(DelegateExecution de) throws Exception {
		System.out.println(((ExecutionListenerExecution)de).getEventSource().getId());
		try{
				de.getEngineServices().getRepositoryService()
				.createProcessDefinitionQuery().singleResult();
		}
		catch(Exception e){
			long startTimeEndEvent = new java.util.Date().getTime();
			// call to DB with the below infor
			// 1)workflowId , 2)taskId, 3)timestamp
		}
	}
	
}
 