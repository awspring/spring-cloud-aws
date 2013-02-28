/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.rds;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import org.elasticspring.core.region.Region;
import org.elasticspring.core.region.RegionProvider;
import org.elasticspring.core.region.ServiceEndpoint;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.jdbc.rds.config.endpoint.AmazonRdsServiceEndpoint;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} implementation for the {@link AmazonRDS} service. This factory
 * bean encapsulates the creation and destruction of the AmazonRDS service.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsClientFactoryBean extends AbstractFactoryBean<AmazonRDS> {

	private final AWSCredentialsProvider awsCredentialsProvider;

	private RegionProvider regionProvider;

	/**
	 * Constructor that retrieves the mandatory AWSCredentialsProvider instance in order to create the service. The
	 * AWSCredentialsProvider is typically configured through ElasticSpring credentials element which may use static or
	 * dynamic credentials to actually create the object.
	 *
	 * @param awsCredentialsProvider
	 * 		- The credentials provider - must not be null
	 */
	public AmazonRdsClientFactoryBean(AWSCredentialsProvider awsCredentialsProvider) {
		Assert.notNull(awsCredentialsProvider, "awsCredentialsProvider must not be null.");
		this.awsCredentialsProvider = awsCredentialsProvider;
	}

	/**
	 * Sets the optional {@link Region} to be used for this AmazonRDSClient
	 *
	 * @param region
	 * 		- a valid {@link Region} identifying the database host
	 */
	@RuntimeUse
	public void setRegionProvider(RegionProvider region) {
		this.regionProvider = region;
	}

	@Override
	public Class<?> getObjectType() {
		return AmazonRDS.class;
	}

	@Override
	protected AmazonRDS createInstance() throws Exception {
		AmazonRDSClient amazonRDSClient = new AmazonRDSClient(this.awsCredentialsProvider);
		if (this.regionProvider != null) {
			ServiceEndpoint serviceEndpoint = new AmazonRdsServiceEndpoint(this.regionProvider.getRegion());
			amazonRDSClient.setEndpoint(serviceEndpoint.getEndpoint());
		}
		return amazonRDSClient;
	}

	/**
	 * Issues a shutdown call on the underlying implementation which will free up all (cached) connections by the client.
	 *
	 * @param instance
	 * 		- the instance of the AmazonRDS client, created by the {@link #createInstance()} method
	 */
	@Override
	protected void destroyInstance(AmazonRDS instance) throws Exception {
		instance.shutdown();
	}
}