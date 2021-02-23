/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.cloudmap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.servicediscovery.model.GetOperationResult;
import com.amazonaws.services.servicediscovery.model.ListNamespacesResult;
import com.amazonaws.services.servicediscovery.model.ListServicesResult;
import com.amazonaws.services.servicediscovery.model.NamespaceSummary;
import com.amazonaws.services.servicediscovery.model.Operation;
import com.amazonaws.services.servicediscovery.model.RegisterInstanceResult;
import com.amazonaws.services.servicediscovery.model.ServiceSummary;

public class CloudMapTestUtils {

	public static final String NAMESPACE = "NAMESPACE";

	public static final String SERVICE = "SERVICE";

	public static final String OPERATION_ID = "OPERATION_ID";

	public static GetOperationResult getOperationResult() {
		GetOperationResult operationResult = new GetOperationResult();
		operationResult.setOperation(new Operation().withStatus("SUCCESS"));
		return operationResult;
	}

	public static RegisterInstanceResult getRegisterInstanceResult() {
		RegisterInstanceResult registerInstanceRequest = new RegisterInstanceResult();
		registerInstanceRequest.setOperationId(OPERATION_ID);
		return registerInstanceRequest;
	}

	public static ListServicesResult getListServicesResult() {
		ServiceSummary serviceSummary = new ServiceSummary();
		serviceSummary.setId(SERVICE);
		serviceSummary.setName(SERVICE);
		ListServicesResult listServicesResult = new ListServicesResult();
		listServicesResult.setServices(Collections.singletonList(serviceSummary));
		return listServicesResult;
	}

	public static ListNamespacesResult getListNamespacesResult() {
		NamespaceSummary summary = new NamespaceSummary();
		summary.setId(NAMESPACE);
		summary.setName(NAMESPACE);
		ListNamespacesResult result = new ListNamespacesResult();
		result.setNamespaces(Collections.singleton(summary));
		return result;
	}

	public static Map<String, String> getAttributesMap() {
		Map<String, String> attributeMap = new HashMap<>();
		attributeMap.put(CloudMapUtils.IPV_4_ADDRESS, "10.1.1.23");
		return attributeMap;
	}

	public static AwsCloudMapRegistryProperties getProperties() {
		AwsCloudMapRegistryProperties properties = new AwsCloudMapRegistryProperties();
		properties.setService(SERVICE);
		properties.setServiceNameSpace(NAMESPACE);
		properties.setDescription("DESCRIPTION");
		properties.setPort(80);
		properties.setHealthCheckThreshold(80);
		properties.setHealthCheckResourcePath("PATH");
		properties.setHealthCheckProtocol("HTTPS");
		return properties;
	}

}
