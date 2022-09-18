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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.awspring.cloud.autoconfigure.cloudmap.properties.discovery.CloudMapDiscoveryProperties;
import io.awspring.cloud.autoconfigure.cloudmap.properties.registration.CloudMapRegistryProperties;
import io.awspring.cloud.autoconfigure.cloudmap.properties.registration.ServiceRegistration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Subnet;
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

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit testcase for {@link ServiceRegistration}
 *
 * @author Hari Ohm Prasath
 * @since 3.0
 */
public class CloudMapRegisterServiceTest {

	private static final String ECS = "ECS";
	private static final String EKS = "EKS";
	private final ServiceDiscoveryClient serviceDiscovery = mock(ServiceDiscoveryClient.class);
	private final Ec2Client ec2Client = mock(Ec2Client.class);

	private final CloudMapUtils cloudMapUtils = CloudMapUtils.getInstance();

	private final RestTemplate restTemplate = mock(RestTemplate.class);

	private final Environment environment = mock(Environment.class);

	public CloudMapRegisterServiceTest() {
		cloudMapUtils.setRestTemplate(restTemplate);
	}

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

		when(restTemplate.getForEntity(CloudMapUtils.EC2_METADATA_URL + "/task", String.class))
				.thenReturn(ResponseEntity.ok(CloudMapTestConstants.ECS_REPONSE_JSON));
		when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(
				DescribeSubnetsResponse.builder().subnets(Subnet.builder().vpcId("vpc-id").build()).build());

		assertThat(cloudMapUtils.registerInstance(serviceDiscovery, ec2Client, getProperties(), environment, ECS)).isNotEmpty();
	}

	@Test
	public void cloudMapRegisterInstanceEcsWithNoNameSpace() {
		final ListNamespacesResponse listNamespacesResponse = getListNamespacesResponse();
		final ListServicesResponse listServicesResponse = getListServicesResponse();
		final RegisterInstanceResponse registerInstanceRequest = getRegisterInstanceResponse();
		final GetOperationResponse operationResponse = getOperationResponse();
		final CreatePrivateDnsNamespaceResponse createPrivateDnsNamespaceResponse = getCreatePrivateDnsNamespaceResponse();
		cloudMapUtils.setRestTemplate(restTemplate);

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

		when(restTemplate.getForEntity(String.format("%s/local-ipv4", CloudMapUtils.EC2_METADATA_URL), String.class))
				.thenReturn(ResponseEntity.ok("10.1.1.1"));
		when(restTemplate.getForEntity(String.format("%s/network/interfaces/macs", CloudMapUtils.EC2_METADATA_URL),
				String.class)).thenReturn(ResponseEntity.ok("MacId/Id"));
		when(restTemplate.getForEntity(
				String.format("%s/network/interfaces/macs/%s/vpc-id", CloudMapUtils.EC2_METADATA_URL, "MacId"),
				String.class)).thenReturn(ResponseEntity.ok("vpcId"));
		when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(
				DescribeSubnetsResponse.builder().subnets(Subnet.builder().vpcId("vpc-id").build()).build());

		assertThat(cloudMapUtils.registerInstance(serviceDiscovery, ec2Client, getProperties(), environment, ECS)).isNotEmpty();
	}

	@Test
	public void cloudMapRegisterInstanceEksWithNoNameSpace() {
		final ListNamespacesResponse listNamespacesResponse = getListNamespacesResponse();
		final ListServicesResponse listServicesResponse = getListServicesResponse();
		final RegisterInstanceResponse registerInstanceRequest = getRegisterInstanceResponse();
		final GetOperationResponse operationResponse = getOperationResponse();
		final CreatePrivateDnsNamespaceResponse createPrivateDnsNamespaceResponse = getCreatePrivateDnsNamespaceResponse();
		cloudMapUtils.setRestTemplate(restTemplate);

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

		when(restTemplate.getForEntity(CloudMapUtils.EC2_METADATA_URL + "/task", String.class))
				.thenReturn(ResponseEntity.ok(CloudMapTestConstants.ECS_REPONSE_JSON));
		when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(
				DescribeSubnetsResponse.builder().subnets(Subnet.builder().vpcId("vpc-id").build()).build());

		assertThat(cloudMapUtils.registerInstance(serviceDiscovery, ec2Client, getProperties(), environment, EKS)).isNotEmpty();
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

		when(restTemplate.getForEntity(CloudMapUtils.EC2_METADATA_URL + "/task", String.class))
				.thenReturn(ResponseEntity.ok(CloudMapTestConstants.ECS_REPONSE_JSON));
		when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(
				DescribeSubnetsResponse.builder().subnets(Subnet.builder().vpcId("vpc-id").build()).build());

		assertThat(cloudMapUtils.registerInstance(serviceDiscovery, ec2Client, getProperties(), environment, ECS)).isNotEmpty();
	}

	@Test
	public void listService() {
		CloudMapDiscoveryProperties cloudMapDiscoveryProperties = new CloudMapDiscoveryProperties();
		cloudMapDiscoveryProperties.setService(CloudMapTestUtils.SERVICE);
		cloudMapDiscoveryProperties.setNameSpace(CloudMapTestUtils.NAMESPACE);
		final ListNamespacesResponse listNamespacesResponse = getListNamespacesResponse();
		final ListServicesResponse listServicesResponse = getListServicesResponse();

		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class))).thenReturn(listNamespacesResponse);
		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResponse);

		cloudMapUtils.listServices(serviceDiscovery, Collections.singletonList(cloudMapDiscoveryProperties));
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

	private CloudMapRegistryProperties getProperties() {
		CloudMapRegistryProperties properties = new CloudMapRegistryProperties();
		properties.setService(CloudMapTestUtils.SERVICE);
		properties.setNameSpace(CloudMapTestUtils.NAMESPACE);
		properties.setDescription("DESCRIPTION");
		return properties;
	}

}
