/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.core.env.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
abstract class AbstractAmazonEC2PropertySource extends PropertySource<AmazonEC2> {

	private InstanceIdProvider instanceIdProvider;
	private volatile Map<String, String> propertyValues;

	public AbstractAmazonEC2PropertySource(String name, AmazonEC2 amazonEC2Client) {
		super(name, amazonEC2Client);
		this.instanceIdProvider = new AmazonEC2InstanceIdProvider();
	}

	public void setInstanceIdProvider(InstanceIdProvider instanceIdProvider) {
		this.instanceIdProvider = instanceIdProvider;
	}

	protected InstanceIdProvider getInstanceIdProvider() {
		return this.instanceIdProvider;
	}

	@Override
	public Object getProperty(String name) {
		if (this.propertyValues == null) {
			String currentInstanceId;
			try {
				currentInstanceId = this.instanceIdProvider.getCurrentInstanceId();
			} catch (IOException e) {
				throw new RuntimeException("Error retrieving instance id", e);
			}
			this.propertyValues = createValueMap(currentInstanceId);
		}
		return this.propertyValues.get(name);
	}

	protected abstract Map<String, String> createValueMap(String instanceId);
}
