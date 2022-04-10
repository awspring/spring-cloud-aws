/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.aws.cloudmap.registration;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.cloud.aws.cloudmap.CloudMapUtils;
import org.springframework.cloud.aws.cloudmap.model.registration.CloudMapRegistryProperties;
import org.springframework.cloud.client.serviceregistry.Registration;

public class ServiceRegistration implements Registration {

	private final CloudMapRegistryProperties properties;

	private final Map<String, String> registrationDetails;

	private final CloudMapUtils UTILS = CloudMapUtils.INSTANCE.getInstance();

	public ServiceRegistration(CloudMapRegistryProperties properties) {
		registrationDetails = UTILS.getRegistrationAttributes();
		this.properties = properties;
	}

	@Override
	public String getInstanceId() {
		return UUID.randomUUID().toString();
	}

	@Override
	public String getScheme() {
		return Registration.super.getScheme();
	}

	@Override
	public String getServiceId() {
		return UTILS.generateServiceId(properties.getNameSpace(), properties.getService());
	}

	@Override
	public String getHost() {
		return registrationDetails.get(UTILS.IPV_4_ADDRESS);
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public URI getUri() {
		return null;
	}

	@Override
	public Map<String, String> getMetadata() {
		return registrationDetails;
	}

}
