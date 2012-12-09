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
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class AmazonEC2UserTagPropertySource extends AbstractAmazonEC2PropertySource {

	public AmazonEC2UserTagPropertySource(String name, AmazonEC2 amazonEC2) {
		super(name, amazonEC2);
	}

	@Override
	protected Map<String, String> createValueMap(String instanceId) {
		LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
		DescribeInstancesResult describeInstancesResult = this.source.describeInstances(
				new DescribeInstancesRequest().withInstanceIds(instanceId));

		for (Reservation reservation : describeInstancesResult.getReservations()) {
			for (Instance instance : reservation.getInstances()) {
				if (instance.getInstanceId().equals(instanceId)) {
					for (Tag tag : instance.getTags()) {
						properties.put(tag.getKey(), tag.getValue());
					}
					break;
				}
			}
		}
		return properties;
	}
}
