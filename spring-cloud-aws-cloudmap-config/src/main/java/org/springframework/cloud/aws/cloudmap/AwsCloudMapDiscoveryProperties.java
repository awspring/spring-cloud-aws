/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.Map;

public class AwsCloudMapDiscoveryProperties {

	private String serviceNameSpace;

	private String service;

	private Map<String, String> filterAttributes;

	private boolean failFast;

	public String getServiceNameSpace() {
		return serviceNameSpace;
	}

	public void setServiceNameSpace(String serviceNameSpace) {
		this.serviceNameSpace = serviceNameSpace;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Map<String, String> getFilterAttributes() {
		return filterAttributes;
	}

	public void setFilterAttributes(Map<String, String> filterAttributes) {
		this.filterAttributes = filterAttributes;
	}

	public boolean isFailFast() {
		return failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

}
