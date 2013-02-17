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
import com.amazonaws.services.s3.AmazonS3;
import org.elasticspring.core.io.s3.S3ServiceEndpoint;
import org.elasticspring.core.io.s3.support.AmazonS3ClientFactory;
import org.elasticspring.core.io.s3.support.EndpointRoutingS3Client;
import org.elasticspring.core.region.RegionProvider;
import org.elasticspring.core.region.ServiceEndpoint;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class AmazonS3FactoryBean extends AbstractFactoryBean<AmazonS3> {

	private final AmazonS3ClientFactory amazonS3ClientFactory;
	private final ServiceEndpoint serviceEndpoint;

	@RuntimeUse
	public AmazonS3FactoryBean(AmazonS3ClientFactory amazonS3ClientFactory, RegionProvider regionProvider) {
		this.amazonS3ClientFactory = amazonS3ClientFactory;
		this.serviceEndpoint = S3ServiceEndpoint.fromRegion(regionProvider.getRegion());
	}

	@Override
	public Class<?> getObjectType() {
		return AmazonS3.class;
	}

	@Override
	protected AmazonS3 createInstance() throws Exception {
		return new EndpointRoutingS3Client(this.amazonS3ClientFactory, this.serviceEndpoint);
	}

	@Override
	protected void destroyInstance(AmazonS3 instance) throws Exception {
		if (instance instanceof AmazonWebServiceClient) {
			((AmazonWebServiceClient) instance).shutdown();
		}
	}
}
