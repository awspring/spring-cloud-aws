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

import java.util.Map;

public class AwsCloudMapDiscoveryProperties {

	private String serviceNameSpace;

	private String service;

	private Map<String, String> filterAttributes;

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

	@Override
	public String toString() {
		String data = "AwsCloudMapDiscoveryProperties{" + "serviceNameSpace=" + serviceNameSpace + ", service="
				+ service;
		if (filterAttributes != null) {
			data += filterAttributes.keySet().stream().map(f -> "key = " + f + ":" + filterAttributes.get(f))
					.reduce((a, b) -> a + "," + b).get();
		}
		data += "}";
		return data;
	}

}
