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

package org.elasticspring.context.config;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} implementation that creates an {@link AmazonS3} client.
 * The lifecycle of the created bean is managed and {@link com.amazonaws.AmazonWebServiceClient#shutdown()} is
 * called before the bean is destroyed.
 *
 * @author Alain Sahli
 * @since 1.0
 */
public class AmazonS3FactoryBean extends AbstractFactoryBean<AmazonS3> {

	private final AWSCredentialsProvider credentials;

	/**
	 * Constructor that retrieves the mandatory {@link AWSCredentialsProvider} instance in order to create the service. The
	 * {@link AWSCredentialsProvider} is typically configured through ElasticSpring credentials element which may use static or
	 * dynamic credentials to actually create the object.
	 *
	 * @param credentialsProvider must not be null
	 */
	@RuntimeUse
	public AmazonS3FactoryBean(AWSCredentialsProvider credentialsProvider) {
		Assert.notNull(credentialsProvider);
		this.credentials = credentialsProvider;
	}

	@Override
	public Class<?> getObjectType() {
		return AmazonS3.class;
	}

	@Override
	protected AmazonS3 createInstance() throws Exception {
		return new AmazonS3Client(this.credentials.getCredentials());
	}

	@Override
	protected void destroyInstance(AmazonS3 instance) throws Exception {
		if (instance instanceof AmazonWebServiceClient) {
			((AmazonWebServiceClient) instance).shutdown();
		}
	}
}
