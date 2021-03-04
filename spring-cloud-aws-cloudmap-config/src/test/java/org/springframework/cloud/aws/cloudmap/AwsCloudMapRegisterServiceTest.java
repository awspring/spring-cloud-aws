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

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.CreatePrivateDnsNamespaceRequest;
import com.amazonaws.services.servicediscovery.model.CreatePrivateDnsNamespaceResult;
import com.amazonaws.services.servicediscovery.model.CreateServiceRequest;
import com.amazonaws.services.servicediscovery.model.CreateServiceResult;
import com.amazonaws.services.servicediscovery.model.DeregisterInstanceRequest;
import com.amazonaws.services.servicediscovery.model.DeregisterInstanceResult;
import com.amazonaws.services.servicediscovery.model.GetOperationRequest;
import com.amazonaws.services.servicediscovery.model.GetOperationResult;
import com.amazonaws.services.servicediscovery.model.ListNamespacesRequest;
import com.amazonaws.services.servicediscovery.model.ListNamespacesResult;
import com.amazonaws.services.servicediscovery.model.ListServicesRequest;
import com.amazonaws.services.servicediscovery.model.ListServicesResult;
import com.amazonaws.services.servicediscovery.model.NamespaceSummary;
import com.amazonaws.services.servicediscovery.model.Operation;
import com.amazonaws.services.servicediscovery.model.RegisterInstanceRequest;
import com.amazonaws.services.servicediscovery.model.RegisterInstanceResult;
import com.amazonaws.services.servicediscovery.model.Service;
import com.amazonaws.services.servicediscovery.model.ServiceSummary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsCloudMapRegisterServiceTest {

	private final AWSServiceDiscovery serviceDiscovery = mock(AWSServiceDiscovery.class);

	private final CloudMapUtils cloudMapUtils = mock(CloudMapUtils.class);

	@Test
	public void cloudMapRegisterInstancesNameSpaceAndServiceExists() {
		final ListNamespacesResult result = getListNamespacesResult();
		final ListServicesResult listServicesResult = getListServicesResult();
		final RegisterInstanceResult registerInstanceRequest = getRegisterInstanceResult();
		final GetOperationResult operationResult = getOperationResult();
		final CloudMapRegistryService registryService = new CloudMapRegistryService(serviceDiscovery, getProperties());

		when(cloudMapUtils.getRegistrationAttributes()).thenReturn(getAttributesMap());
		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class))).thenReturn(result);
		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResult);
		when(serviceDiscovery.registerInstance((any(RegisterInstanceRequest.class))))
				.thenReturn(registerInstanceRequest);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResult);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResult);
		assertThat(registryService.registerInstances()).hasToString(CloudMapTestUtils.OPERATION_ID);
	}

	@Test
	public void cloudMapRegisterInstanceWithNoNameSpace() {
		final ListNamespacesResult namespacesResult = getListNamespacesResult();
		final ListServicesResult listServicesResult = getListServicesResult();
		final RegisterInstanceResult registerInstanceRequest = getRegisterInstanceResult();
		final GetOperationResult operationResult = getOperationResult();
		final CloudMapRegistryService registryService = new CloudMapRegistryService(serviceDiscovery, getProperties());
		final CreatePrivateDnsNamespaceResult nameSpaceResult = getCreatePrivateDnsNamespaceResult();

		when(cloudMapUtils.getRegistrationAttributes()).thenReturn(getAttributesMap());
		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class)))
				.thenReturn(new ListNamespacesResult().withNamespaces(new NamespaceSummary()), namespacesResult);
		when(serviceDiscovery.createPrivateDnsNamespace(any(CreatePrivateDnsNamespaceRequest.class)))
				.thenReturn(nameSpaceResult);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResult);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResult);
		when(serviceDiscovery.registerInstance((any(RegisterInstanceRequest.class))))
				.thenReturn(registerInstanceRequest);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResult);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResult);
		assertThat(registryService.registerInstances()).hasToString(CloudMapTestUtils.OPERATION_ID);
	}

	@Test
	public void cloudMapRegisterInstancesWithNoService() {
		final ListNamespacesResult result = getListNamespacesResult();
		final ListServicesResult listServicesResult = getListServicesResult();
		final RegisterInstanceResult registerInstanceRequest = getRegisterInstanceResult();
		final GetOperationResult operationResult = getOperationResult();
		final CreateServiceResult createServiceResult = new CreateServiceResult();
		createServiceResult
				.setService(new Service().withName(CloudMapTestUtils.SERVICE).withId(CloudMapTestUtils.SERVICE));
		final CloudMapRegistryService registryService = new CloudMapRegistryService(serviceDiscovery, getProperties());

		when(cloudMapUtils.getRegistrationAttributes()).thenReturn(getAttributesMap());
		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class))).thenReturn(result);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class)))
				.thenReturn(new ListServicesResult().withServices(new ServiceSummary()), listServicesResult);
		when(serviceDiscovery.createService(any(CreateServiceRequest.class))).thenReturn(createServiceResult);

		when(serviceDiscovery.registerInstance((any(RegisterInstanceRequest.class))))
				.thenReturn(registerInstanceRequest);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResult);

		assertThat(registryService.registerInstances()).hasToString(CloudMapTestUtils.OPERATION_ID);
	}

	@Test
	public void deRegisterInstances() {
		try {
			final CloudMapRegistryService registryService = new CloudMapRegistryService(serviceDiscovery,
					getProperties());
			registryService.setServiceInstanceId("SERVICE_INSTANCE_ID");
			registryService.setServiceId(CloudMapTestUtils.SERVICE);

			DeregisterInstanceResult result = new DeregisterInstanceResult();
			result.setOperationId(CloudMapTestUtils.OPERATION_ID);
			when(serviceDiscovery.deregisterInstance(any(DeregisterInstanceRequest.class))).thenReturn(result);

			final GetOperationResult waitingResult = getOperationResult();
			waitingResult.setOperation(new Operation().withStatus("PENDING"));

			final GetOperationResult successResult = getOperationResult();
			when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(waitingResult,
					successResult);
			registryService.deregisterInstance();
		}
		catch (Exception e) {
			Assertions.fail();
		}
	}

	private CreatePrivateDnsNamespaceResult getCreatePrivateDnsNamespaceResult() {
		CreatePrivateDnsNamespaceResult createPrivateDnsNamespaceResult = new CreatePrivateDnsNamespaceResult();
		createPrivateDnsNamespaceResult.setOperationId(CloudMapTestUtils.OPERATION_ID);
		return createPrivateDnsNamespaceResult;
	}

	private GetOperationResult getOperationResult() {
		GetOperationResult operationResult = new GetOperationResult();
		operationResult.setOperation(new Operation().withStatus("SUCCESS"));
		return operationResult;
	}

	private RegisterInstanceResult getRegisterInstanceResult() {
		RegisterInstanceResult registerInstanceRequest = new RegisterInstanceResult();
		registerInstanceRequest.setOperationId(CloudMapTestUtils.OPERATION_ID);
		return registerInstanceRequest;
	}

	private ListServicesResult getListServicesResult() {
		ServiceSummary serviceSummary = new ServiceSummary();
		serviceSummary.setId(CloudMapTestUtils.SERVICE);
		serviceSummary.setName(CloudMapTestUtils.SERVICE);
		ListServicesResult listServicesResult = new ListServicesResult();
		listServicesResult.setServices(Collections.singletonList(serviceSummary));
		return listServicesResult;
	}

	private ListNamespacesResult getListNamespacesResult() {
		NamespaceSummary summary = new NamespaceSummary();
		summary.setId(CloudMapTestUtils.NAMESPACE);
		summary.setName(CloudMapTestUtils.NAMESPACE);
		ListNamespacesResult result = new ListNamespacesResult();
		result.setNamespaces(Collections.singleton(summary));
		return result;
	}

	private Map<String, String> getAttributesMap() {
		Map<String, String> attributeMap = new HashMap<>();
		attributeMap.put(CloudMapUtils.IPV_4_ADDRESS, "10.1.1.23");
		return attributeMap;
	}

	private CloudMapRegistryProperties getProperties() {
		CloudMapRegistryProperties properties = new CloudMapRegistryProperties();
		properties.setService(CloudMapTestUtils.SERVICE);
		properties.setServiceNameSpace(CloudMapTestUtils.NAMESPACE);
		properties.setDescription("DESCRIPTION");
		properties.setPort(80);
		properties.setHealthCheckThreshold(80);
		properties.setHealthCheckResourcePath("PATH");
		properties.setHealthCheckProtocol("HTTPS");
		return properties;
	}

}
