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

import org.springframework.cloud.aws.cloudmap.annotations.CloudMapRegistry;

/**
 * Unit testcase for {@link CloudMapRegistryAnnotationScanner}
 *
 * @author Hari Ohm Prasath
 * @since 2.3.2
 */
@CloudMapRegistry(nameSpace = CloudMapTestUtils.NAMESPACE, service = CloudMapTestUtils.SERVICE,
		description = "DESCRIPTION")
public class AwsCloudMapAnnotationScannerTest {

//	private final AWSServiceDiscovery serviceDiscovery = mock(AWSServiceDiscovery.class);
//
//	private final CloudMapUtils cloudMapUtils = mock(CloudMapUtils.class);
//
//	@Test
//	public void scanAndRegisterTest() {
//		final ListNamespacesResult result = CloudMapTestUtils.getListNamespacesResult();
//		final ListServicesResult listServicesResult = CloudMapTestUtils.getListServicesResult();
//		final RegisterInstanceResult registerInstanceRequest = CloudMapTestUtils.getRegisterInstanceResult();
//		final GetOperationResult operationResult = CloudMapTestUtils.getOperationResult();
//		final CloudMapRegistryService registryService = new CloudMapRegistryService(serviceDiscovery,
//				CloudMapTestUtils.getProperties());
//
//		when(cloudMapUtils.getRegistrationAttributes()).thenReturn(CloudMapTestUtils.getAttributesMap());
//		when(serviceDiscovery.listNamespaces(any(ListNamespacesRequest.class))).thenReturn(result);
//		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResult);
//		when(serviceDiscovery.registerInstance((any(RegisterInstanceRequest.class))))
//				.thenReturn(registerInstanceRequest);
//		when(serviceDiscovery.getOperation((any(GetOperationRequest.class)))).thenReturn(operationResult);
//
//		when(serviceDiscovery.listServices(any(ListServicesRequest.class))).thenReturn(listServicesResult);
//		assertThat(registryService.registerInstance()).hasToString(CloudMapTestUtils.OPERATION_ID);
//
//		CloudMapRegistryAnnotationScanner scanner = new CloudMapRegistryAnnotationScanner(serviceDiscovery,
//				"org.springframework.cloud.aws.cloudmap");
//		scanner.scanAndRegister();
//	}

}
