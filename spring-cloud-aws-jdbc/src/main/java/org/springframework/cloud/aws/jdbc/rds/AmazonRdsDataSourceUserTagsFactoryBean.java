/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.jdbc.rds;

import java.util.LinkedHashMap;
import java.util.Map;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import com.amazonaws.services.rds.model.Tag;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.naming.AmazonResourceName;

/**
 * @author Agim Emruli
 */
public class AmazonRdsDataSourceUserTagsFactoryBean
		extends AbstractFactoryBean<Map<String, String>> {

	private final AmazonRDS amazonRds;

	private final String dbInstanceIdentifier;

	private final AmazonIdentityManagement identityManagement;

	private ResourceIdResolver resourceIdResolver;

	private Region region;

	public AmazonRdsDataSourceUserTagsFactoryBean(AmazonRDS amazonRds,
			String dbInstanceIdentifier, AmazonIdentityManagement identityManagement) {
		this.amazonRds = amazonRds;
		this.dbInstanceIdentifier = dbInstanceIdentifier;
		this.identityManagement = identityManagement;
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}

	@Override
	protected Map<String, String> createInstance() throws Exception {
		LinkedHashMap<String, String> userTags = new LinkedHashMap<>();
		ListTagsForResourceResult tagsForResource = this.amazonRds
				.listTagsForResource(new ListTagsForResourceRequest()
						.withResourceName(getDbInstanceResourceName()));
		for (Tag tag : tagsForResource.getTagList()) {
			userTags.put(tag.getKey(), tag.getValue());
		}
		return userTags;
	}

	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	private String getDbInstanceIdentifier() {
		return this.resourceIdResolver != null ? this.resourceIdResolver
				.resolveToPhysicalResourceId(this.dbInstanceIdentifier)
				: this.dbInstanceIdentifier;
	}

	private Region getRegion() {
		if (this.region != null) {
			return this.region;
		}
		return Region.getRegion(Regions.DEFAULT_REGION);
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	/**
	 * Unfortunately Amazon AWS mandates to use ARN notation to get the tags. Therefore we
	 * first need to get the account number through the IAM service and then construct the
	 * ARN out of the account no and region
	 * @return the arn string used to query the tags
	 */
	private String getDbInstanceResourceName() {
		String userArn = this.identityManagement.getUser().getUser().getArn();
		AmazonResourceName userResourceName = AmazonResourceName.fromString(userArn);
		AmazonResourceName dbResourceArn = new AmazonResourceName.Builder()
				.withService("rds").withRegion(getRegion())
				.withAccount(userResourceName.getAccount()).withResourceType("db")
				.withResourceName(getDbInstanceIdentifier())
				.withResourceTypeDelimiter(":").build();
		return dbResourceArn.toString();
	}

}
