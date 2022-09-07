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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.cloudmap.discovery.CloudMapServiceInstance;
import io.awspring.cloud.autoconfigure.cloudmap.properties.discovery.CloudMapDiscoveryProperties;
import io.awspring.cloud.autoconfigure.cloudmap.properties.registration.CloudMapRegistryProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.cloudmap.exceptions.CreateNameSpaceException;
import org.springframework.cloud.aws.cloudmap.exceptions.CreateServiceException;
import org.springframework.cloud.aws.cloudmap.exceptions.MaxRetryExceededException;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient;
import software.amazon.awssdk.services.servicediscovery.model.CreatePrivateDnsNamespaceRequest;
import software.amazon.awssdk.services.servicediscovery.model.CreateServiceRequest;
import software.amazon.awssdk.services.servicediscovery.model.DeregisterInstanceRequest;
import software.amazon.awssdk.services.servicediscovery.model.DiscoverInstancesRequest;
import software.amazon.awssdk.services.servicediscovery.model.DnsConfig;
import software.amazon.awssdk.services.servicediscovery.model.DnsRecord;
import software.amazon.awssdk.services.servicediscovery.model.DuplicateRequestException;
import software.amazon.awssdk.services.servicediscovery.model.GetOperationRequest;
import software.amazon.awssdk.services.servicediscovery.model.HttpInstanceSummary;
import software.amazon.awssdk.services.servicediscovery.model.InvalidInputException;
import software.amazon.awssdk.services.servicediscovery.model.ListNamespacesRequest;
import software.amazon.awssdk.services.servicediscovery.model.ListNamespacesResponse;
import software.amazon.awssdk.services.servicediscovery.model.ListServicesRequest;
import software.amazon.awssdk.services.servicediscovery.model.ListServicesResponse;
import software.amazon.awssdk.services.servicediscovery.model.NamespaceAlreadyExistsException;
import software.amazon.awssdk.services.servicediscovery.model.NamespaceSummary;
import software.amazon.awssdk.services.servicediscovery.model.Operation;
import software.amazon.awssdk.services.servicediscovery.model.RecordType;
import software.amazon.awssdk.services.servicediscovery.model.RegisterInstanceRequest;
import software.amazon.awssdk.services.servicediscovery.model.ResourceLimitExceededException;
import software.amazon.awssdk.services.servicediscovery.model.ServiceAlreadyExistsException;
import software.amazon.awssdk.services.servicediscovery.model.ServiceFilter;
import software.amazon.awssdk.services.servicediscovery.model.ServiceSummary;

/**
 * Uses Fargate Metadata URL to retrieve IPv4 address and VPC ID to register instances to cloudmap.
 *
 * @author Hari Ohm Prasath
 * @since 3.0
 */
public class CloudMapUtils {

	/*
	 * Singleton instance
	 */
	private static CloudMapUtils cloudMapUtils = null;

	public static final String EC2_METADATA_URL = "http://169.254.169.254/latest/meta-data";
	/*
	 * AWS VPC ID
	 */
	private static final String VPC_ID = "VPC_ID";
	/*
	 * Local IP address
	 */
	private static final String AWS_INSTANCE_IPV_4 = "AWS_INSTANCE_IPV4";
	/*
	 * AWS Region
	 */
	private static final String REGION = "REGION";
	/*
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(CloudMapUtils.class);
	/*
	 * Namespace status - SUBMITTED
	 */
	private static final String SUBMITTED = "SUBMITTED";
	/*
	 * Namespace status - PENDING
	 */
	private static final String PENDING = "PENDING";
	/*
	 * Maximum number of polling before returning error
	 */
	private static final int MAX_POLL = 30;
	/*
	 * Deployment platform environment variable
	 */
	public final String DEPLOYMENT_PLATFORM = "DEPLOYMENT_PLATFORM";

	/*
	 * Deployment platform type ECS
	 */
	public final String EKS = "EKS";

	/*
	 * Metadata URL
	 */
	public final String ECS_CONTAINER_METADATA_URI_V_4 = "ECS_CONTAINER_METADATA_URI_V4";

	/*
	 * Request attributes - NamespaceID
	 */
	public final String NAMESPACE_ID = "NAMESPACE_ID";

	/*
	 * Request attributes - ServiceID
	 */
	public final String SERVICE_ID = "SERVICE_ID";

	/*
	 * Request attributes - ServiceInstanceID
	 */
	public final String SERVICE_INSTANCE_ID = "SERVICE_INSTANCE_ID";

	/*
	 * Request attributes - IP address
	 */
	public final String IPV_4_ADDRESS = "IPV4_ADDRESS";

	final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private RestTemplate restTemplate;

	private Ec2Client ec2Client;

	/**
	 * Uses metadata URL to fetch all the required details around IP address and VpcID to register instances to cloudmap
	 * service. If Deployment platform is not passed in then we consider it as classic EC2 or ECS based deployment
	 * platform
	 * @return map containing ip address and vpcid
	 */
	public Map<String, String> getRegistrationAttributes() {
		String deploymentPlatform = System.getenv(DEPLOYMENT_PLATFORM) == null ? System.getProperty(DEPLOYMENT_PLATFORM)
				: System.getenv(DEPLOYMENT_PLATFORM);
		LOGGER.info("Deployment platform passed in {} ", deploymentPlatform);
		if (StringUtils.hasText(deploymentPlatform) && EKS.equalsIgnoreCase(deploymentPlatform.trim()))
			return getEksRegistrationAttributes();
		return getEcsRegistrationAttributes();
	}

	/**
	 * Get Cloudmap namespaceID based on name
	 * @param serviceDiscovery AWS Service discovery
	 * @param nameSpace cloudmap namespace
	 * @return namespaceID
	 */
	public String getNameSpaceId(final ServiceDiscoveryClient serviceDiscovery, final String nameSpace) {
		String token;
		do {
			ListNamespacesResponse nameSpaceResponse = serviceDiscovery
					.listNamespaces(ListNamespacesRequest.builder().build());
			token = nameSpaceResponse.nextToken();

			List<NamespaceSummary> namespaceSummaries = nameSpaceResponse.namespaces();
			if (namespaceSummaries != null) {
				Optional<String> namespaceId = namespaceSummaries.stream().filter(n -> n.name().equals(nameSpace))
						.map(NamespaceSummary::id).findFirst();
				if (namespaceId.isPresent())
					return namespaceId.get();
			}
			else
				LOGGER.warn("Namespace {} not available", nameSpace);
		}
		while (StringUtils.hasText(token));

		return null;
	}

	/**
	 * List services based on namespace and filter them based on name
	 * @param serviceDiscovery AWS service discovery
	 * @param discoveryProperties discovery properties (includes namespace and service name)
	 * @return list of cloudmap services
	 */
	public List<String> listServices(final ServiceDiscoveryClient serviceDiscovery,
			List<CloudMapDiscoveryProperties> discoveryProperties) {
		final List<String> serviceList = new ArrayList<>();

		if (discoveryProperties != null && !discoveryProperties.isEmpty()) {
			for (CloudMapDiscoveryProperties d : discoveryProperties) {
				final String serviceName = d.getService();
				final String nameSpace = d.getNameSpace();
				String token = null;

				do {
					// Get namespaceID
					final String nameSpaceId = getNameSpaceId(serviceDiscovery, nameSpace);
					if (StringUtils.hasText(nameSpaceId)) {
						// Filter cloudmap services
						final ServiceFilter serviceFilter = ServiceFilter.builder().name(NAMESPACE_ID).condition("EQ")
								.values(nameSpaceId).build();
						final ListServicesRequest servicesRequest = ListServicesRequest.builder().filters(serviceFilter)
								.build();
						final ListServicesResponse response = serviceDiscovery.listServices(servicesRequest);
						if (StringUtils.hasText(response.nextToken()))
							token = response.nextToken();

						if (StringUtils.hasText(serviceName)) {
							serviceList.addAll(response.services().stream().filter(r -> r.name().equals(d.getService()))
									.map(r -> generateServiceId(nameSpace, r.name())).collect(Collectors.toList()));
							if (serviceList.size() == discoveryProperties.size())
								return serviceList;
						}
						else
							serviceList.addAll(response.services().stream()
									.map(r -> generateServiceId(nameSpace, r.name())).collect(Collectors.toList()));
					}
					else
						LOGGER.warn("Namespace is empty");
				}
				while (StringUtils.hasText(token));
			}
		}

		return serviceList;
	}

	/**
	 * List cloudmap instances based on service name and namespace
	 * @param serviceDiscovery AWS Service discovery
	 * @param namespace cloudmap namespace
	 * @param serviceName cloudmap service name
	 * @return list of http instances
	 */
	public List<HttpInstanceSummary> listInstances(final ServiceDiscoveryClient serviceDiscovery,
			final String namespace, String serviceName) {
		final DiscoverInstancesRequest dRequest = DiscoverInstancesRequest.builder().namespaceName(namespace)
				.serviceName(serviceName).build();

		return serviceDiscovery.discoverInstances(dRequest).instances();
	}

	/**
	 * Get service instance from http instance summary
	 * @param instanceSummary HTTP instance summary - Cloudmap object
	 * @return Service instance - Spring object
	 */
	public ServiceInstance getServiceInstance(HttpInstanceSummary instanceSummary) {
		return new CloudMapServiceInstance(instanceSummary);
	}

	/**
	 * Register with cloudmap, the method takes care of the following: 1. Create namespace, if not exists 2. Create
	 * service, if not exists 3. Register the instance with the created namespace and service
	 * @param serviceDiscovery AWS Service discovery service
	 * @param properties Cloud map registry properties
	 * @param environment Spring environment
	 * @return map of registration properties
	 */
	public Map<String, String> registerInstance(final ServiceDiscoveryClient serviceDiscovery,
			final CloudMapRegistryProperties properties, final Environment environment) {
		if (properties != null && StringUtils.hasText(properties.getNameSpace())
				&& StringUtils.hasText(properties.getService())) {

			String nameSpace = properties.getNameSpace();
			if (!StringUtils.hasText(nameSpace))
				nameSpace = "default-namespace";

			String service = properties.getService();
			if (!StringUtils.hasText(service))
				service = environment.getProperty("spring.application.name");

			if (!StringUtils.hasText(service))
				service = "default-service";

			final String serviceInstanceId = UUID.randomUUID().toString();

			LOGGER.info("Registration details namespace {} - service {} - serviceInstance {}", nameSpace, service,
					serviceInstanceId);
			Map<String, String> registrationDetails = getRegistrationAttributes();
			String nameSpaceId = getNameSpaceId(serviceDiscovery, properties.getNameSpace());
			try {
				// Create namespace if not exists
				if (!StringUtils.hasText(nameSpaceId)) {
					LOGGER.debug("Namespace " + nameSpace + "not available so creating");
					nameSpaceId = createNameSpace(serviceDiscovery, properties, registrationDetails.get(VPC_ID));
				}

				// Create service if not exists
				String serviceId = getServiceId(serviceDiscovery, nameSpaceId, service);
				if (!StringUtils.hasText(serviceId)) {
					LOGGER.debug("Service " + service + " doesnt exist so creating new one");
					serviceId = createService(serviceDiscovery, nameSpaceId, service);
				}

				Map<String, String> attributes = new HashMap<>();
				attributes.put(AWS_INSTANCE_IPV_4, registrationDetails.get(IPV_4_ADDRESS));
				attributes.put(REGION, System.getenv("AWS_REGION"));
				attributes.put(NAMESPACE_ID, nameSpaceId);
				attributes.put(SERVICE_ID, serviceId);
				attributes.put(SERVICE_INSTANCE_ID, serviceInstanceId);

				// Register instance
				final String operationId = serviceDiscovery.registerInstance(RegisterInstanceRequest.builder()
						.instanceId(serviceInstanceId).serviceId(serviceId).attributes(attributes).build())
						.operationId();
				LOGGER.debug("Register instance initiated, polling for completion {}", operationId);

				// Poll for completion
				pollForCompletion(serviceDiscovery, operationId);

				return attributes;
			}
			catch (InvalidInputException e) {
				LOGGER.error("Invalid input passed into the service {} - {}", nameSpaceId, e.getMessage(), e);
			}
			catch (CreateNameSpaceException e) {
				LOGGER.error("Error while creating namespace {} - {}", nameSpace, e.getMessage());
			}
			catch (InterruptedException e) {
				LOGGER.error("Error while polling for status update {} with error {}", nameSpace, e.getMessage());
			}
			catch (CreateServiceException e) {
				LOGGER.error("Error while creating service {} with {} - {}", service, nameSpace, e.getMessage());
			}
			catch (MaxRetryExceededException e) {
				LOGGER.error("Maximum number of retry exceeded for registering instance with {} for {}", nameSpace,
						service, e);
			}
		}
		else {
			LOGGER.info("Service registration skipped");
		}

		return null;
	}

	/**
	 * Create Cloudmap namespace.
	 * @param serviceDiscovery AWS Service discovery
	 * @param properties cloudmap properties
	 * @param vpcId VPC ID
	 * @return NamespaceID
	 * @throws CreateNameSpaceException thrown in case of runtime exception
	 */
	private String createNameSpace(ServiceDiscoveryClient serviceDiscovery, CloudMapRegistryProperties properties,
			String vpcId) throws CreateNameSpaceException {
		final String nameSpace = properties.getNameSpace();
		try {
			// Create namespace
			final String operationId = serviceDiscovery.createPrivateDnsNamespace(CreatePrivateDnsNamespaceRequest
					.builder().name(nameSpace).vpc(vpcId).description(properties.getDescription()).build())
					.operationId();
			LOGGER.info("Creating namespace {} with operationId {}", nameSpace, operationId);

			// Wait till completion
			pollForCompletion(serviceDiscovery, operationId);

			return getNameSpaceId(serviceDiscovery, nameSpace);
		}
		catch (NamespaceAlreadyExistsException e) {
			LOGGER.warn("Namespace {} already exists", nameSpace);
			return getNameSpaceId(serviceDiscovery, nameSpace);
		}
		catch (InvalidInputException | ResourceLimitExceededException | DuplicateRequestException e) {
			LOGGER.error("Error while registering with cloudmap {} with error {}", nameSpace, e.getMessage(), e);
			throw new CreateNameSpaceException(e);
		}
		catch (InterruptedException e) {
			LOGGER.error("Error while polling for status update {} with error {}", nameSpace, e.getMessage(), e);
			throw new CreateNameSpaceException(e);
		}
		catch (MaxRetryExceededException e) {
			LOGGER.error("Maximum number of retry exceeded for namespace {}", nameSpace, e);
			throw new CreateNameSpaceException(e);
		}
	}

	/**
	 * Create service.
	 * @param serviceDiscovery AWS Service Discovery
	 * @param nameSpaceId CloudMap Namespace ID
	 * @param service Service name
	 * @return Service ID
	 * @throws CreateServiceException thrown in case of runtime exception
	 */
	private String createService(ServiceDiscoveryClient serviceDiscovery, String nameSpaceId, String service)
			throws CreateServiceException {
		try {
			CreateServiceRequest serviceRequest = CreateServiceRequest.builder().name(service).namespaceId(nameSpaceId)
					.dnsConfig(DnsConfig.builder().dnsRecords(DnsRecord.builder().type(RecordType.A).ttl(300L).build())
							.build())
					.build();

			final String serviceId = serviceDiscovery.createService(serviceRequest).service().id();
			LOGGER.info("Service ID create {} for {} with namespace {}", serviceId, service, nameSpaceId);
			return serviceId;
		}
		catch (ServiceAlreadyExistsException e) {
			LOGGER.warn("Service {} already exists", service);
			return getServiceId(serviceDiscovery, service, nameSpaceId);
		}
		catch (InvalidInputException | ResourceLimitExceededException e) {
			LOGGER.error("Error while creating service {} with namespace {}", service, nameSpaceId);
			throw new CreateServiceException(e);
		}
	}

	public String generateServiceId(final String namespace, final String serviceName) {
		return String.format("%s@%s", namespace, serviceName);
	}

	/**
	 * Automatically deregister the instance when the container is stopped.
	 * @param serviceDiscovery AWS Service Discovery Service
	 * @param attributeMap Service discovery attributes
	 */
	public void deregisterInstance(final ServiceDiscoveryClient serviceDiscovery,
			final Map<String, String> attributeMap) {
		try {
			final String serviceInstanceId = attributeMap.get(SERVICE_INSTANCE_ID);
			final String serviceId = attributeMap.get(SERVICE_ID);
			LOGGER.info("Initiating de-registration process {} - {}", serviceInstanceId, serviceId);

			// Deregister instance
			String operationId = serviceDiscovery.deregisterInstance(
					DeregisterInstanceRequest.builder().instanceId(serviceInstanceId).serviceId(serviceId).build())
					.operationId();

			// Wait till completion
			pollForCompletion(serviceDiscovery, operationId);
		}
		catch (InterruptedException e) {
			LOGGER.error("Error while polling for status while de-registering instance {}", e.getMessage(), e);
		}
		catch (MaxRetryExceededException e) {
			LOGGER.error("Maximum number of retry exceeded {}", e.getMessage(), e);
		}
	}

	public static CloudMapUtils getInstance() {
		if (cloudMapUtils == null) {
			cloudMapUtils = new CloudMapUtils();
		}
		return cloudMapUtils;
	}

	/**
	 * Get service ID based on service name and namespace ID.
	 * @param serviceDiscovery AWS Service discovery
	 * @param nameSpaceId Namespace ID
	 * @param serviceName name of the cloudmap service
	 * @return Cloudmap service ID
	 */
	private String getServiceId(ServiceDiscoveryClient serviceDiscovery, String nameSpaceId, String serviceName) {
		ServiceFilter filter = ServiceFilter.builder().name(NAMESPACE_ID).values(Collections.singletonList(nameSpaceId))
				.build();
		Optional<ServiceSummary> serviceSummary = serviceDiscovery
				.listServices(ListServicesRequest.builder().filters(filter).build()).services().stream()
				.filter(s -> serviceName.equals(s.name())).findFirst();
		return serviceSummary.map(ServiceSummary::id).orElse(null);
	}

	/**
	 * Poll for completion.
	 * @param serviceDiscovery AWS Service discovery
	 * @param operationId cloudmap operationID
	 * @throws InterruptedException thrown in case of thread.sleep() exception
	 * @throws MaxRetryExceededException thrown if maximum polling duration has exceeded
	 */
	private void pollForCompletion(ServiceDiscoveryClient serviceDiscovery, String operationId)
			throws InterruptedException, MaxRetryExceededException {
		Operation operation = serviceDiscovery
				.getOperation(GetOperationRequest.builder().operationId(operationId).build()).operation();
		int counter = 0;
		LOGGER.info("Operation ID {} will be polled", operationId);
		while ((SUBMITTED.equalsIgnoreCase(operation.statusAsString())
				|| PENDING.equalsIgnoreCase(operation.statusAsString())) && counter < MAX_POLL) {
			operation = serviceDiscovery.getOperation(GetOperationRequest.builder().operationId(operationId).build())
					.operation();
			Thread.sleep(2000);
			counter++;
		}

		if (counter > MAX_POLL) {
			throw new MaxRetryExceededException("Maximum of retry exceeded for " + operationId);
		}
	}

	/**
	 * Get CloudMap attributes for EKS platform
	 * @return map of cloud map attributes with Ipaddress and vpcid
	 */
	private Map<String, String> getEksRegistrationAttributes() {
		try {
			String ipAddress = getUrlResponse(String.format("%s/local-ipv4", EC2_METADATA_URL));
			final String macId = getUrlResponse(String.format("%s/network/interfaces/macs", EC2_METADATA_URL));
			if (StringUtils.hasText(macId) && macId.contains("/")) {
				final String macAddress = macId.split("/")[0];
				final String vpcUrl = String.format("%s/network/interfaces/macs/%s/vpc-id", EC2_METADATA_URL,
						macAddress);
				final String vpcId = getUrlResponse(vpcUrl);
				LOGGER.info("Metadata details IP Address {}, macAddress {} - VPCId {}", ipAddress, macAddress, vpcId);
				return getCloudMapAttributes(ipAddress, vpcId);
			}
		}
		catch (Exception e) {
			LOGGER.error("Error while getting registration details {}", e.getMessage(), e);
		}
		return new HashMap<>();
	}

	/**
	 * Get CloudMap attributes for ECS platform
	 * @return map of cloud map attributes with Ipaddress and vpcid
	 */
	private Map<String, String> getEcsRegistrationAttributes() {
		try {
			String metaDataUrl = System.getenv(ECS_CONTAINER_METADATA_URI_V_4);
			if (!StringUtils.hasText(metaDataUrl))
				metaDataUrl = EC2_METADATA_URL;
			final String responseBody = getUrlResponse(metaDataUrl + "/task");
			JsonNode root = JSON_MAPPER.readTree(responseBody);
			JsonNode jsonNode = root.get("Containers").get(0).get("Networks").get(0);
			final String ipv4Address = jsonNode.get("IPv4Addresses").get(0).asText();
			final String cidrBlock = jsonNode.get("IPv4SubnetCIDRBlock").asText();
			final String vpcId = getEc2Client()
					.describeSubnets(DescribeSubnetsRequest.builder()
							.filters(Filter.builder().name("cidr-block").values(cidrBlock).build()).build())
					.subnets().get(0).vpcId();
			LOGGER.info("IPv4Address {} - VPC ID {}", ipv4Address, vpcId);
			return getCloudMapAttributes(ipv4Address, vpcId);
		}
		catch (Exception e) {
			LOGGER.error("Error while fetching network details - {}", e.getMessage(), e);
		}
		return new HashMap<>();
	}

	/**
	 * Helper method to fetch contents of URL as string
	 * @param url URL to fetch from
	 * @return response as string
	 */
	private String getUrlResponse(String url) {
		return getRestTemplate().getForEntity(url, String.class).getBody();
	}

	/**
	 * Returns hash map of cloudmap attributes
	 * @param ipv4Address IP Address of the instance
	 * @param vpcId VPC ID in which the instance is hosted
	 * @return hash map of cloudmap attributes
	 */
	private Map<String, String> getCloudMapAttributes(String ipv4Address, String vpcId) {
		Map<String, String> attributes = new HashMap<>();
		attributes.put(IPV_4_ADDRESS, ipv4Address);
		attributes.put(VPC_ID, vpcId);
		return attributes;
	}

	/**
	 * Get Ec2 client
	 * @return ec2 client object
	 */
	Ec2Client getEc2Client() {
		if (ec2Client == null) {
			ec2Client = Ec2Client.builder().region(Region.of(System.getenv("AWS_REGION")))
					.credentialsProvider(DefaultCredentialsProvider.builder().build()).build();
		}
		return ec2Client;
	}

	/**
	 * Get Rest Template
	 * @return restTemplate
	 */
	RestTemplate getRestTemplate() {
		if (restTemplate == null) {
			restTemplate = new RestTemplate();
		}
		return restTemplate;
	}

	void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	void setEc2Client(Ec2Client ec2Client) {
		this.ec2Client = ec2Client;
	}
}
