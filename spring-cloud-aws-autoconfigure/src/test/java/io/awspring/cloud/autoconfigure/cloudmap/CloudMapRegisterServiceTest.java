/*
 * Copyright 2013-2021 the original author or authors.
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
package io.awspring.cloud.autoconfigure.cloudmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.awspring.cloud.autoconfigure.cloudmap.properties.registration.CloudMapRegistryProperties;
import io.awspring.cloud.autoconfigure.cloudmap.properties.registration.ServiceRegistration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient;
import software.amazon.awssdk.services.servicediscovery.model.CreatePrivateDnsNamespaceRequest;
import software.amazon.awssdk.services.servicediscovery.model.CreatePrivateDnsNamespaceResponse;
import software.amazon.awssdk.services.servicediscovery.model.CreateServiceRequest;
import software.amazon.awssdk.services.servicediscovery.model.CreateServiceResponse;
import software.amazon.awssdk.services.servicediscovery.model.DeregisterInstanceRequest;
import software.amazon.awssdk.services.servicediscovery.model.DeregisterInstanceResponse;
import software.amazon.awssdk.services.servicediscovery.model.GetOperationRequest;
import software.amazon.awssdk.services.servicediscovery.model.GetOperationResponse;
import software.amazon.awssdk.services.servicediscovery.model.ListNamespacesRequest;
import software.amazon.awssdk.services.servicediscovery.model.ListNamespacesResponse;
import software.amazon.awssdk.services.servicediscovery.model.ListServicesRequest;
import software.amazon.awssdk.services.servicediscovery.model.ListServicesResponse;
import software.amazon.awssdk.services.servicediscovery.model.NamespaceSummary;
import software.amazon.awssdk.services.servicediscovery.model.Operation;
import software.amazon.awssdk.services.servicediscovery.model.OperationStatus;
import software.amazon.awssdk.services.servicediscovery.model.RegisterInstanceRequest;
import software.amazon.awssdk.services.servicediscovery.model.RegisterInstanceResponse;
import software.amazon.awssdk.services.servicediscovery.model.Service;
import software.amazon.awssdk.services.servicediscovery.model.ServiceSummary;

/**
 * Unit testcase for {@link ServiceRegistration}
 *
 * @author Hari Ohm Prasath
 * @since 3.0
 */
public class CloudMapRegisterServiceTest {

	private final ServiceDiscoveryClient serviceDiscovery = mock(ServiceDiscoveryClient.class);

	private final CloudMapUtils cloudMapUtils = CloudMapUtils.getInstance();

	private final Environment environment = mock(Environment.class);

	@Test
	public void cloudMapRegisterInstancesNameSpaceAndServiceExists() {
		final ListNamespacesResponse response = getListNamespacesResponse();
		final ListServicesResponse listServicesResponse = getListServicesResponse();
		final RegisterInstanceResponse registerInstanceRequest = getRegisterInstanceResponse();
		final GetOperationResponse operationResponse = getOperationResponse();

		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class))).thenReturn(response);
		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResponse);
		when(serviceDiscovery.registerInstance((any(RegisterInstanceRequest.class))))
				.thenReturn(registerInstanceRequest);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResponse);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResponse);
		assertThat(cloudMapUtils.registerInstance(serviceDiscovery, getProperties(), environment)).isNotEmpty();
	}

	@Test
	public void cloudMapRegisterInstanceWithNoNameSpace() {
		final ListNamespacesResponse listNamespacesResponse = getListNamespacesResponse();
		final ListServicesResponse listServicesResponse = getListServicesResponse();
		final RegisterInstanceResponse registerInstanceRequest = getRegisterInstanceResponse();
		final GetOperationResponse operationResponse = getOperationResponse();
		final CreatePrivateDnsNamespaceResponse createPrivateDnsNamespaceResponse = getCreatePrivateDnsNamespaceResponse();

		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class))).thenReturn(
				ListNamespacesResponse.builder().namespaces(Collections.emptyList()).build(), listNamespacesResponse);
		when(serviceDiscovery.createPrivateDnsNamespace(any(CreatePrivateDnsNamespaceRequest.class)))
				.thenReturn(createPrivateDnsNamespaceResponse);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResponse);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResponse);
		when(serviceDiscovery.registerInstance((any(RegisterInstanceRequest.class))))
				.thenReturn(registerInstanceRequest);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResponse);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResponse);
		assertThat(cloudMapUtils.registerInstance(serviceDiscovery, getProperties(), environment)).isNotEmpty();
	}

	@Test
	public void cloudMapRegisterInstancesWithNoService() {
		final ListNamespacesResponse listNamespacesResponse = getListNamespacesResponse();
		final ListServicesResponse listServicesResponse = getListServicesResponse();
		final RegisterInstanceResponse registerInstanceResponse = getRegisterInstanceResponse();
		final GetOperationResponse operationResponse = getOperationResponse();
		final CreateServiceResponse createServiceResponse = CreateServiceResponse.builder()
				.service(Service.builder().name(CloudMapTestUtils.SERVICE).id(CloudMapTestUtils.SERVICE).build())
				.build();

		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class))).thenReturn(listNamespacesResponse);

		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(
				ListServicesResponse.builder().services(Collections.emptyList()).build(), listServicesResponse);
		when(serviceDiscovery.createService(any(CreateServiceRequest.class))).thenReturn(createServiceResponse);

		when(serviceDiscovery.registerInstance((any(RegisterInstanceRequest.class))))
				.thenReturn(registerInstanceResponse);
		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResponse);

		assertThat(cloudMapUtils.registerInstance(serviceDiscovery, getProperties(), environment)).isNotEmpty();
	}

	@Test
	public void deRegisterInstances() {
		try {
			final Map<String, String> attributeMap = new HashMap<>();
			attributeMap.put("SERVICE_INSTANCE_ID", "SERVICE_INSTANCE_ID");
			attributeMap.put("SERVICE_ID", "SERVICE_ID");

			DeregisterInstanceResponse deregisterInstanceResponse = DeregisterInstanceResponse.builder()
					.operationId(CloudMapTestUtils.OPERATION_ID).build();
			when(serviceDiscovery.deregisterInstance(any(DeregisterInstanceRequest.class)))
					.thenReturn(deregisterInstanceResponse);

			final GetOperationResponse operationResponse = getOperationResponse();
			when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResponse,
					operationResponse);
			cloudMapUtils.deregisterInstance(serviceDiscovery, attributeMap);
		}
		catch (Exception e) {
			Assertions.fail();
		}
	}

	private CreatePrivateDnsNamespaceResponse getCreatePrivateDnsNamespaceResponse() {
		return CreatePrivateDnsNamespaceResponse.builder().operationId(CloudMapTestUtils.OPERATION_ID).build();
	}

	private GetOperationResponse getOperationResponse() {
		return GetOperationResponse.builder().operation(Operation.builder().status(OperationStatus.SUCCESS).build())
				.build();
	}

	private RegisterInstanceResponse getRegisterInstanceResponse() {
		return RegisterInstanceResponse.builder().operationId(CloudMapTestUtils.OPERATION_ID).build();
	}

	private ListServicesResponse getListServicesResponse() {
		ServiceSummary serviceSummary = ServiceSummary.builder().id(CloudMapTestUtils.SERVICE)
				.name(CloudMapTestUtils.SERVICE).build();
		return ListServicesResponse.builder().services(Collections.singletonList(serviceSummary)).build();
	}

	private ListNamespacesResponse getListNamespacesResponse() {
		NamespaceSummary summary = NamespaceSummary.builder().id(CloudMapTestUtils.NAMESPACE)
				.name(CloudMapTestUtils.NAMESPACE).build();
		return ListNamespacesResponse.builder().namespaces(Collections.singleton(summary)).build();
	}

	private Map<String, String> getAttributesMap() {
		Map<String, String> attributeMap = new HashMap<>();
		attributeMap.put(cloudMapUtils.IPV_4_ADDRESS, "10.1.1.23");
		return attributeMap;
	}

	private CloudMapRegistryProperties getProperties() {
		CloudMapRegistryProperties properties = new CloudMapRegistryProperties();
		properties.setService(CloudMapTestUtils.SERVICE);
		properties.setNameSpace(CloudMapTestUtils.NAMESPACE);
		properties.setDescription("DESCRIPTION");
		return properties;
	}

}
