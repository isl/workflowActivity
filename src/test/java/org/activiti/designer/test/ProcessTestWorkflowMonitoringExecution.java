package org.activiti.designer.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.FileInputStream;

import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricFormProperty;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Rule;
import org.junit.Test;

public class ProcessTestWorkflowMonitoringExecution {

	@Rule 
	public ActivitiRule activitiRule = new ActivitiRule("activiti.cfg-mem.xml");

	@Test
	@Deployment(resources={"WorkflowMonitoringExecutionV0.bpmn"})
	public void startProcess() throws Exception {
		// apo edw pernei to process me id startFormTest
		ProcessDefinition definition = activitiRule.getRepositoryService()
		.createProcessDefinitionQuery().processDefinitionKey("workflowMonitoringExecution").singleResult();
		assertNotNull(definition);
		
		
		// kalei to form service gia na parei ta formvariables apo to start
		FormService formService = activitiRule.getFormService();
		List<FormProperty> formList = formService.getStartFormData(definition.getId()).getFormProperties();
		assertEquals(1, formList.size());
		
		// ta kanei put se map
		Map<String, String> formProperties = new HashMap<String, String>();
		formProperties.put("serviceName", "fortress-web");
		

		// ta kanei submit kai thewroyme oti exei hdh teleiwsei kai to workflow edw
		formService.submitStartFormData(definition.getId(), formProperties);
		
		// kai apo edw kai katw kanoyme retrieve gia na paroyme ta variables poy mas niazoyn
		List<HistoricDetail> historyVariables = activitiRule.getHistoryService()
        	.createHistoricDetailQuery()
        	.formProperties()
        	.list();

		assertNotNull(historyVariables);
		assertEquals(1, historyVariables.size());
		HistoricFormProperty formProperty = (HistoricFormProperty) historyVariables.get(0);
		assertEquals("serviceName", formProperty.getPropertyId());
	}
}