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

// @checkstyle:off
package org.springframework.cloud.aws.cloudmap.discovery;

import com.amazonaws.services.servicediscovery.model.HttpInstanceSummary;
import org.springframework.cloud.aws.cloudmap.CloudMapUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Map;

public class CloudMapServiceInstance implements ServiceInstance {

	private final CloudMapUtils UTILS = CloudMapUtils.INSTANCE.getInstance();

	HttpInstanceSummary instanceSummary;

	public CloudMapServiceInstance(HttpInstanceSummary httpInstanceSummary) {
		this.instanceSummary = httpInstanceSummary;
	}

	@Override
	public String getInstanceId() {
		return instanceSummary.getInstanceId();
	}

	@Override
	public String getScheme() {
		return getUri().getScheme();
	}

	@Override
	public String getServiceId() {
		return UTILS.generateServiceId(instanceSummary.getNamespaceName(), instanceSummary.getServiceName());
	}

	@Override
	public String getHost() {
		return instanceSummary.getAttributes().get("AWS_INSTANCE_IPV4");
	}

	@Override
	public int getPort() {
		String port = instanceSummary.getAttributes().get("AWS_INSTANCE_PORT");
		return StringUtils.hasText(port) ? Integer.parseInt(port) : 0;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public URI getUri() {
		return URI.create(String.format("http://%s:%s", this.getHost(), this.getPort()));
	}

	@Override
	public Map<String, String> getMetadata() {
		return instanceSummary.getAttributes();
	}

}
