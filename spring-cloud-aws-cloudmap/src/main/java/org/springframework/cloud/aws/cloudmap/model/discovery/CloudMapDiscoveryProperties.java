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

package org.springframework.cloud.aws.cloudmap.model.discovery;

import java.util.Map;

/**
 * POJO class to capture cloudmap discovery attributes.
 *
 * @author Hari Ohm Prasath
 * @since 2.3.2
 */
public class CloudMapDiscoveryProperties {

	private String nameSpace;

	private String service;

	private Map<String, String> filterAttributes;

	public String getNameSpace() {
		return this.nameSpace;
	}

	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}

	public String getService() {
		return this.service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Map<String, String> getFilterAttributes() {
		return this.filterAttributes;
	}

	public void setFilterAttributes(Map<String, String> filterAttributes) {
		this.filterAttributes = filterAttributes;
	}

	@Override
	public String toString() {
		String data = "AwsCloudMapDiscoveryProperties{" + "serviceNameSpace=" + nameSpace + ", service=" + service;
		if (filterAttributes != null) {
			data += filterAttributes.keySet().stream().map(f -> "key = " + f + ":" + filterAttributes.get(f))
					.reduce((a, b) -> a + "," + b).get();
		}
		data += "}";
		return data;
	}

}
