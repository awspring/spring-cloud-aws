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

package org.elasticspring.context.config.xml.support;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import org.springframework.util.Assert;

/**
* @author Agim Emruli
* Test stub used by {@link org.elasticspring.context.config.xml.support.AmazonWebserviceClientConfigurationUtilsTest}
*/
class AmazonTestWebserviceClient extends AmazonWebServiceClient {

	private Region region;

	AmazonTestWebserviceClient(AWSCredentialsProvider awsCredentialsProvider) {
		super(new ClientConfiguration());
		Assert.notNull(awsCredentialsProvider);
	}

	public Region getRegion() {
		return this.region;
	}

	@Override
	public void setRegion(Region region) {
		this.region = region;
	}
}
