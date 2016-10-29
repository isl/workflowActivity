package org.workflowMonitoring;

import java.io.Serializable;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class ServiceCatalogueGeneral implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private String serviceName;

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

	
}
