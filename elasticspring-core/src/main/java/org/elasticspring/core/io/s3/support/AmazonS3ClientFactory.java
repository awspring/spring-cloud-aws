/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3.support;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.elasticspring.core.region.Region;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that returns the corresponding {@link AmazonS3Client} based
 * on the {@link Region}. The {@link AmazonS3Client} are cached so that
 * at most one instance is create per {@link Region}.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class AmazonS3ClientFactory {

	private static final String SERVICE_NAME = "s3-";
	private final AWSCredentialsProvider credentials;
	private final ConcurrentHashMap<Region, AmazonS3Client> clientsForRegion = new ConcurrentHashMap<Region, AmazonS3Client>();

	public AmazonS3ClientFactory(AWSCredentialsProvider credentials) {
		this.credentials = credentials;
	}

	/**
	 * Method that returns the corresponding {@link AmazonS3} client based
	 * on the {@link Region}.
	 * @param region the {@link Region} that the client must access.
	 * @return the correspinding {@link AmazonS3} client.
	 */
	public AmazonS3 getClientForRegion(Region region) {
		AmazonS3Client cachedAmazonS3Client = this.clientsForRegion.get(region);
		if (cachedAmazonS3Client != null) {
			return cachedAmazonS3Client;
		} else {
			AmazonS3Client amazonS3Client = new AmazonS3Client(this.credentials.getCredentials());
			amazonS3Client.setEndpoint(SERVICE_NAME + region.getRegionDomain());
			return this.clientsForRegion.putIfAbsent(region, amazonS3Client);
		}
	}
}