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
package io.awspring.cloud.autoconfigure.cloudmap.integration;

import java.time.Duration;
import java.util.List;

import io.awspring.cloud.autoconfigure.cloudmap.AwsCloudMapStoreClientCustomizer;
import io.awspring.cloud.autoconfigure.cloudmap.CloudMapUtils;
import io.awspring.cloud.autoconfigure.cloudmap.discovery.CloudMapDiscoveryClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static io.awspring.cloud.autoconfigure.cloudmap.CloudMapUtils.EKS;

/**
 * Integration test for Eks based Cloud Map registration.
 *
 * @author Hari Ohm Prasath
 * @since 3.0
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class EksCloudMapIntegrationTest {

	// Local stack container with Cloud Map and Ec2 services enabled
	@Container
	private static final LocalStackContainer localStackContainer = new LocalStackContainer(
		DockerImageName.parse("localstack/localstack:1.1.0"))
		.withServices(LocalStackContainer.EnabledService.named("servicediscovery"), LocalStackContainer.Service.EC2)
		.withEnv(IntegrationTestUtil.LOCAL_STACK_API, IntegrationTestUtil.LOCAL_STACK_API_KEY)
		.withReuse(true);

	/**
	 * Create all the pre-requisites for the test.
	 */
	@BeforeAll
	static void beforeAll() {
		final String vpcId = IntegrationTestUtil.createVpc(localStackContainer);
		IntegrationTestUtil.createSubnet(localStackContainer, vpcId);
		IntegrationTestUtil.createCloudMapResources(localStackContainer, IntegrationTestUtil.TEST_DEFAULT_NAMESPACE
			, IntegrationTestUtil.TEST_DEFAULT_SERVICE, vpcId);
	}

	/**
	 * Register a service with no specification provided in application.properties.
	 */
	@Test
	void registerEksContainerWithCloudMapWithNoSpecification() {
		SpringApplication application = new SpringApplication(EksApp.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfiguration());

		List<String> properties = IntegrationTestUtil.getCloudMapSpringBootProperties(localStackContainer);
		properties.add("--spring.cloud.aws.cloudmap.deploymentPlatform=" + EKS);

		try (ConfigurableApplicationContext context = application.run(properties.toArray(new String[0]))) {
			final ServiceDiscoveryClient serviceDiscoveryClient = context.getBean(ServiceDiscoveryClient.class);
			final CloudMapUtils cloudMapUtils = CloudMapUtils.getInstance();
			Assertions.assertNotNull(cloudMapUtils.getNameSpaceId(serviceDiscoveryClient, CloudMapUtils.DEFAULT_NAMESPACE));
			final String serviceId = cloudMapUtils.getServiceId(serviceDiscoveryClient, CloudMapUtils.DEFAULT_NAMESPACE,
				CloudMapUtils.DEFAULT_SERVICE);
			Assertions.assertNotNull(serviceId);
			Assertions.assertEquals(1, IntegrationTestUtil.getCloudMapRegisteredInstances(localStackContainer, serviceId));
		}
	}

	/**
	 * Register a service with defined specifications like namespace, service, etc
	 */
	@Test
	void registerEksContainerWithCloudMapWithDefinedSpecification() {
		SpringApplication application = new SpringApplication(EcsCloudMapIntegrationTest.EcsApp.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new EcsCloudMapIntegrationTest.AwsConfigurerClientConfiguration());

		final String nameSpace = "a.namespace";
		final String service = "a.service";

		List<String> properties = IntegrationTestUtil.getCloudMapSpringBootProperties(localStackContainer);
		properties.add("--spring.cloud.aws.cloudmap.registry.nameSpace=" + nameSpace);
		properties.add("--spring.cloud.aws.cloudmap.registry.service=" + service);
		properties.add("--spring.cloud.aws.cloudmap.registry.description=Name space description");
		properties.add("--spring.cloud.aws.cloudmap.deploymentPlatform=" + EKS);

		try (ConfigurableApplicationContext context = application.run(properties.toArray(new String[0]))) {
			final ServiceDiscoveryClient serviceDiscoveryClient = context.getBean(ServiceDiscoveryClient.class);
			final CloudMapUtils cloudMapUtils = CloudMapUtils.getInstance();
			Assertions.assertNotNull(cloudMapUtils.getNameSpaceId(serviceDiscoveryClient, nameSpace));
			final String serviceId = cloudMapUtils.getServiceId(serviceDiscoveryClient, nameSpace, service);
			Assertions.assertNotNull(serviceId);
			Assertions.assertEquals(1, IntegrationTestUtil.getCloudMapRegisteredInstances(localStackContainer, serviceId));
		}
	}

	/**
	 * Discover the service by pre-creating the namepsace and service.
	 */
	@Test
	void discoverEksCloudMapInstancesWithNoSpecification() {
		SpringApplication application = new SpringApplication(EcsCloudMapIntegrationTest.EcsApp.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new EcsCloudMapIntegrationTest.AwsConfigurerClientConfiguration());

		List<String> properties = IntegrationTestUtil.getCloudMapSpringBootProperties(localStackContainer);
		properties.add("--spring.cloud.aws.cloudmap.discovery.discoveryList[0].nameSpace=" + IntegrationTestUtil.TEST_DEFAULT_NAMESPACE);
		properties.add("--spring.cloud.aws.cloudmap.discovery.discoveryList[0].service=" + IntegrationTestUtil.TEST_DEFAULT_SERVICE);
		properties.add("--spring.cloud.aws.cloudmap.deploymentPlatform=" + EKS);

		try (ConfigurableApplicationContext context = application.run(properties.toArray(new String[0]))) {
			CloudMapDiscoveryClient discoveryClient = context.getBean(CloudMapDiscoveryClient.class);
			final List<String> services = discoveryClient.getServices();
			Assertions.assertNotNull(services);
			Assertions.assertEquals(1, services.size());
			Assertions.assertEquals(String.format("%s@%s", IntegrationTestUtil.TEST_DEFAULT_NAMESPACE,
				IntegrationTestUtil.TEST_DEFAULT_SERVICE), services.get(0));
		}
	}

	@SpringBootApplication
	static class EksApp {
		static {
			System.setProperty(CloudMapUtils.EC2_METADATA, IntegrationTestUtil.META_DATA_MOCK_RESPONSE);
		}
	}

	static class AwsConfigurerClientConfiguration implements BootstrapRegistryInitializer {
		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(AwsCloudMapStoreClientCustomizer.class,
				context -> new AwsCloudMapStoreClientCustomizer() {

					@Override
					public ClientOverrideConfiguration overrideConfiguration() {
						return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(2828))
							.build();
					}

					@Override
					public SdkHttpClient httpClient() {
						return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
					}
				});
		}
	}
}
