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

package org.springframework.cloud.aws.cloudmap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.CreatePrivateDnsNamespaceRequest;
import com.amazonaws.services.servicediscovery.model.CreateServiceRequest;
import com.amazonaws.services.servicediscovery.model.DeregisterInstanceRequest;
import com.amazonaws.services.servicediscovery.model.DnsConfig;
import com.amazonaws.services.servicediscovery.model.DnsRecord;
import com.amazonaws.services.servicediscovery.model.DuplicateRequestException;
import com.amazonaws.services.servicediscovery.model.GetOperationRequest;
import com.amazonaws.services.servicediscovery.model.HealthCheckConfig;
import com.amazonaws.services.servicediscovery.model.HealthCheckType;
import com.amazonaws.services.servicediscovery.model.InvalidInputException;
import com.amazonaws.services.servicediscovery.model.ListNamespacesRequest;
import com.amazonaws.services.servicediscovery.model.ListServicesRequest;
import com.amazonaws.services.servicediscovery.model.NamespaceAlreadyExistsException;
import com.amazonaws.services.servicediscovery.model.NamespaceFilter;
import com.amazonaws.services.servicediscovery.model.NamespaceSummary;
import com.amazonaws.services.servicediscovery.model.Operation;
import com.amazonaws.services.servicediscovery.model.RecordType;
import com.amazonaws.services.servicediscovery.model.RegisterInstanceRequest;
import com.amazonaws.services.servicediscovery.model.ResourceLimitExceededException;
import com.amazonaws.services.servicediscovery.model.ServiceAlreadyExistsException;
import com.amazonaws.services.servicediscovery.model.ServiceFilter;
import com.amazonaws.services.servicediscovery.model.ServiceSummary;
import com.amazonaws.util.StringUtils;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class CloudMapRegistryService implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

	private static final String SUBMITTED = "SUBMITTED";

	private static final String PENDING = "PENDING";

	private static final int MAX_POLL = 30;

	private static final String AWS_INSTANCE_IPV_4 = "AWS_INSTANCE_IPV4";

	private static final String AWS_INSTANCE_PORT = "AWS_INSTANCE_PORT";

	private static final String REGION = "REGION";

	private static final Logger log = LoggerFactory.getLogger(CloudMapRegistryService.class);

	private final AWSServiceDiscovery serviceDiscovery;

	private final AwsCloudMapRegistryProperties properties;

	private String serviceInstanceId;

	private String serviceId;

	private final CloudMapUtils cloudMapUtils = new CloudMapUtils();

	private volatile Connector connector;

	public CloudMapRegistryService(AWSServiceDiscovery serviceDiscovery, AwsCloudMapRegistryProperties properties) {
		this.serviceDiscovery = serviceDiscovery;
		this.properties = properties;
	}

	public String registerInstances() {
		if (properties != null && !StringUtils.isNullOrEmpty(properties.getServiceNameSpace())
				&& !StringUtils.isNullOrEmpty(properties.getService())) {

			final String nameSpace = properties.getServiceNameSpace();
			final String service = properties.getService();

			this.serviceInstanceId = UUID.randomUUID().toString();

			Map<String, String> registrationDetails = cloudMapUtils.getRegistrationAttributes();
			String nameSpaceId = getNameSpaceId(properties.getServiceNameSpace());
			try {
				if (nameSpaceId == null) {
					log.debug("Namespace " + nameSpace + "not available so creating");
					nameSpaceId = createNameSpace(properties, registrationDetails.get(CloudMapUtils.VPC_ID));
				}

				String serviceId = getServiceId(service, nameSpaceId);
				if (serviceId == null) {
					log.debug("Service " + service + " doesnt exist so creating new one");
					serviceId = createService(nameSpaceId);
				}
				this.serviceId = serviceId;

				Map<String, String> attributes = new HashMap<>();
				attributes.put(AWS_INSTANCE_IPV_4, registrationDetails.get(CloudMapUtils.IPV_4_ADDRESS));
				attributes.put(AWS_INSTANCE_PORT, String.valueOf(properties.getPort()));
				attributes.put(REGION, System.getenv("AWS_REGION"));

				final String operationId = serviceDiscovery.registerInstance(new RegisterInstanceRequest()
						.withInstanceId(serviceInstanceId).withServiceId(serviceId).withAttributes(attributes))
						.getOperationId();
				log.debug("Register instance initiated, polling for completion {}", operationId);

				pollForCompletion(operationId);

				return operationId;
			}
			catch (CreateNameSpaceException e) {
				log.error("Error while creating namespace {} - {}", nameSpace, e.getMessage());
			}
			catch (InterruptedException e) {
				log.error("Error while polling for status update {} with error {}", nameSpace, e.getMessage());
			}
			catch (CreateServiceException e) {
				log.error("Error while creating service {} with {} - {}", service, nameSpace, e.getMessage());
			}
			catch (MaxRetryExceededException e) {
				log.error("Maximum number of retry exceeded for registering instance with {} for {}", nameSpace,
						service, e);
			}
			catch (Exception e) {
				log.error("Internal error {}", e.getMessage(), e);
			}
		}
		else {
			log.info("Service registration skipped");
		}

		return null;
	}

	private String createNameSpace(AwsCloudMapRegistryProperties properties, String vpcId)
			throws CreateNameSpaceException {
		final String nameSpace = properties.getServiceNameSpace();
		try {
			final String operationId = serviceDiscovery.createPrivateDnsNamespace(new CreatePrivateDnsNamespaceRequest()
					.withName(nameSpace).withVpc(vpcId).withDescription(properties.getDescription())).getOperationId();

			pollForCompletion(operationId);

			return getNameSpaceId(nameSpace);
		}
		catch (NamespaceAlreadyExistsException | InvalidInputException | ResourceLimitExceededException
				| DuplicateRequestException e) {
			log.error("Error while registering with cloudmap {} with error {}", nameSpace, e.getMessage(), e);
			throw new CreateNameSpaceException(e);
		}
		catch (InterruptedException e) {
			log.error("Error while polling for status update {} with error {}", nameSpace, e.getMessage(), e);
			throw new CreateNameSpaceException(e);
		}
		catch (MaxRetryExceededException e) {
			log.error("Maximum number of retry exceeded for namespace {}", nameSpace, e);
			throw new CreateNameSpaceException(e);
		}
		catch (Exception e) {
			log.error("Internal error {}", e.getMessage(), e);
			throw new CreateNameSpaceException(e);
		}
	}

	private String createService(String nameSpaceId) throws CreateServiceException {
		final String nameSpace = properties.getServiceNameSpace();
		final String service = properties.getService();

		try {
			CreateServiceRequest serviceRequest = new CreateServiceRequest().withName(service)
					.withNamespaceId(nameSpaceId).withDnsConfig(
							new DnsConfig().withDnsRecords(new DnsRecord().withType(RecordType.A).withTTL(300L)));

			if (!StringUtils.isNullOrEmpty(properties.getHealthCheckResourcePath())) {
				HealthCheckConfig healthCheckConfig = new HealthCheckConfig();
				healthCheckConfig.setType(String.valueOf(("https").equalsIgnoreCase(properties.getHealthCheckProtocol())
						? HealthCheckType.HTTPS : HealthCheckType.HTTP));
				healthCheckConfig.setResourcePath(properties.getHealthCheckResourcePath());
				healthCheckConfig.setFailureThreshold(
						(properties.getHealthCheckThreshold() == null) ? 5 : properties.getHealthCheckThreshold());
				serviceRequest.withHealthCheckConfig(healthCheckConfig);
			}

			final String serviceId = serviceDiscovery.createService(serviceRequest).getService().getId();
			log.info("Service ID create {} for {} with namespace {}", serviceId, service, nameSpace);
			return serviceId;
		}
		catch (InvalidInputException | ResourceLimitExceededException | ServiceAlreadyExistsException e) {
			log.error("Error while creating service {} with namespace {}", service, nameSpace);
			throw new CreateServiceException(e);
		}
		catch (Exception e) {
			log.error("Internal error {}", e.getMessage(), e);
			throw new CreateServiceException(e);
		}
	}

	private String getServiceId(String serviceName, String nameSpaceId) {
		ServiceFilter filter = new ServiceFilter();
		filter.setName("NAMESPACE_ID");
		filter.setValues(Collections.singletonList(nameSpaceId));
		Optional<ServiceSummary> serviceSummary = serviceDiscovery
				.listServices(new ListServicesRequest().withFilters(filter)).getServices().stream()
				.filter(s -> serviceName.equals(s.getName())).findFirst();
		return serviceSummary.map(ServiceSummary::getId).orElse(null);
	}

	private String getNameSpaceId(String nameSpace) {
		ListNamespacesRequest request = new ListNamespacesRequest();
		NamespaceFilter filter = new NamespaceFilter();
		filter.setName("TYPE");
		filter.setCondition("EQ");
		filter.setValues(Collections.singletonList("DNS_PRIVATE"));
		request.setFilters(Collections.singletonList(filter));
		Optional<NamespaceSummary> nameSpaceSummary = serviceDiscovery.listNamespaces(request).getNamespaces().stream()
				.filter(n -> nameSpace.equalsIgnoreCase(n.getName())).findFirst();
		return nameSpaceSummary.map(NamespaceSummary::getId).orElse(null);
	}

	private void pollForCompletion(String operationId) throws InterruptedException, MaxRetryExceededException {
		Operation operation = serviceDiscovery.getOperation(new GetOperationRequest().withOperationId(operationId))
				.getOperation();
		int counter = 0;
		while ((SUBMITTED.equalsIgnoreCase(operation.getStatus()) || PENDING.equalsIgnoreCase(operation.getStatus()))
				&& counter < MAX_POLL) {
			operation = serviceDiscovery.getOperation(new GetOperationRequest().withOperationId(operationId))
					.getOperation();
			Thread.sleep(2000);
			counter++;
		}

		if (counter > MAX_POLL) {
			throw new MaxRetryExceededException("Maximum of retry exceeded for " + operationId);
		}
	}

	@Override
	public void customize(Connector connector) {
		this.connector = connector;
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		this.deregisterInstance();
	}

	public void deregisterInstance() {
		if (!StringUtils.isNullOrEmpty(serviceInstanceId) && !StringUtils.isNullOrEmpty(serviceId)) {
			try {
				log.info("Initiating de-registration process {} - {}", serviceInstanceId, serviceId);

				String operationId = serviceDiscovery.deregisterInstance(
						new DeregisterInstanceRequest().withInstanceId(serviceInstanceId).withServiceId(serviceId))
						.getOperationId();

				pollForCompletion(operationId);
			}
			catch (InterruptedException e) {
				log.error("Error while polling for status while de-registering instance {}", e.getMessage(), e);
			}
			catch (MaxRetryExceededException e) {
				log.error("Maximum number of retry exceeded {}", e.getMessage(), e);
			}
			catch (Exception e) {
				log.error("Internal error {}", e.getMessage(), e);
			}
		}
	}

	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	static class CreateNameSpaceException extends RuntimeException {

		CreateNameSpaceException(Throwable cause) {
			super(cause);
		}

	}

	static class CreateServiceException extends RuntimeException {

		CreateServiceException(Throwable cause) {
			super(cause);
		}

	}

	static class MaxRetryExceededException extends RuntimeException {

		MaxRetryExceededException(String message) {
			super(message);
		}

	}

}
