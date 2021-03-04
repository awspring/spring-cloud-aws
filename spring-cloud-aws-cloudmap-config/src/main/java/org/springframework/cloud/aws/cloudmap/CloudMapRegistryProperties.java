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

public class CloudMapRegistryProperties {

	private String serviceNameSpace;

	private String service;

	private String description;

	private String healthCheckProtocol;

	private Integer healthCheckThreshold;

	private String healthCheckResourcePath;

	private int port;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHealthCheckProtocol() {
		return healthCheckProtocol;
	}

	public void setHealthCheckProtocol(String healthCheckProtocol) {
		this.healthCheckProtocol = healthCheckProtocol;
	}

	public Integer getHealthCheckThreshold() {
		return healthCheckThreshold;
	}

	public void setHealthCheckThreshold(Integer healthCheckThreshold) {
		this.healthCheckThreshold = healthCheckThreshold;
	}

	public String getHealthCheckResourcePath() {
		return healthCheckResourcePath;
	}

	public void setHealthCheckResourcePath(String healthCheckResourcePath) {
		this.healthCheckResourcePath = healthCheckResourcePath;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
