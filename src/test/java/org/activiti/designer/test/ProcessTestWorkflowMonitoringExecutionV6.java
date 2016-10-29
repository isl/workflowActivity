package org.activiti.designer.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.FileInputStream;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Task;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricFormProperty;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Rule;
import org.junit.Test;

public class ProcessTestWorkflowMonitoringExecutionV6 {

	@Rule 
	public ActivitiRule activitiRule = new ActivitiRule("activiti.cfg-mem.xml");

	@Test
	@Deployment(resources={"WorkflowMonitoringExecutionV5.bpmn"})
	public void startProcess() throws Exception {
		// apo edw pernei to PROCESS me id startFormTest
		ProcessDefinition definition = activitiRule.getRepositoryService()
		.createProcessDefinitionQuery().processDefinitionKey("workflowMonitoringExecution").singleResult();
		assertNotNull(definition);
				
		
		// kalei to form service gia na parei ta formvariables apo to start
		FormService formService = activitiRule.getFormService();
		
		
		// PRWTA GIA TO PRWTO FORM
		List<FormProperty> formList = formService.getStartFormData(definition.getId()).getFormProperties();
		assertEquals(1, formList.size());
		// ta kanei put se map
		Map<String, String> formProperties = new HashMap<String, String>();
		formProperties.put("serviceName", "fortress-web");
		// ta kanei submit kai thewroyme oti exei hdh teleiwsei kai to workflow edw
		formService.submitStartFormData(definition.getId(), formProperties);
		

		
//		long taskDefinition = activitiRule.getTaskService()
//				.createTaskQuery().taskDefinitionKey("userTask1").count();
//		System.out.println("taskDefinition einai iso me : " + taskDefinition);
		// META GIA TO DEUTERO FORM
		// ME TON PARAKATW TROPO KANOYME RETRIEVE TO TASK ANALOGWS ME TO KEY TOY KAI KANOUME PUT
		// TIS EKASTOTE METAVLITES
		TaskEntity tdefinition =   (TaskEntity) activitiRule.getTaskService()
				.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
		assertNotNull(tdefinition);
		List<FormProperty> formTaskList = formService.getTaskFormData(tdefinition.getId()).getFormProperties();
		assertEquals(5, formTaskList.size());
		Map<String, String> formTaskProperties = new HashMap<String, String>();
		formTaskProperties.put("serviceIp", "localhost");
		formTaskProperties.put("prefixService", "fortress-web");
		formTaskProperties.put("port", "8080");
		formTaskProperties.put("username", "test");
		//formTaskProperties.put("password", "password");
		formService.submitTaskFormData(tdefinition.getId(), formTaskProperties);
		
		
		
		// kai apo edw kai katw kanoyme retrieve gia na paroyme ta variables poy mas niazoyn
		List<HistoricDetail> historyVariables = activitiRule.getHistoryService()
        	.createHistoricDetailQuery()
        	.formProperties()
        	.list();

		assertNotNull(historyVariables);
		assertEquals(6, historyVariables.size());
		HistoricFormProperty formProperty = (HistoricFormProperty) historyVariables.get(0);
		assertEquals("port", formProperty.getPropertyId());
		
//		JobExecutor jobExecutor = ((ProcessEngineImpl) activitiRule.getProcessEngine()).getProcessEngineConfiguration().getJobExecutor();
//		System.out.println("To jobExecutor time einai iso me : " + jobExecutor.getWaitTimeInMillis());
		 
	}
}