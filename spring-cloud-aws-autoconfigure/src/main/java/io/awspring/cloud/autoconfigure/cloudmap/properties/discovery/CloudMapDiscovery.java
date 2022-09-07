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
package io.awspring.cloud.autoconfigure.cloudmap.properties.discovery;

import java.util.List;

/**
 * Pojo class to capture all the discovery parameters.
 *
 * @author Hari Ohm Prasath
 * @since 3.0
 */
public class CloudMapDiscovery {

	private List<CloudMapDiscoveryProperties> discoveryList;

	public List<CloudMapDiscoveryProperties> getDiscoveryList() {
		return this.discoveryList;
	}

	public void setDiscoveryList(List<CloudMapDiscoveryProperties> discoveryList) {
		this.discoveryList = discoveryList;
	}

	@Override
	public String toString() {
		return "AwsCloudMapDiscovery{" + "discoveryList=" + discoveryList.toString() + '}';
	}

}
