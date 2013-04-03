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

package org.elasticspring.core.io.s3.support;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import org.elasticspring.core.region.ServiceEndpoint;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that returns the corresponding {@link AmazonS3Client} based
 * on the {@link org.elasticspring.core.io.s3.S3ServiceEndpoint}. The {@link AmazonS3Client} are cached so that
 * at most one instance is create per {@link org.elasticspring.core.io.s3.S3ServiceEndpoint}.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class AmazonS3ClientFactory {

	private final AWSCredentialsProvider credentials;
	private final ConcurrentHashMap<ServiceEndpoint, AmazonS3Client> clientsForRegion = new ConcurrentHashMap<ServiceEndpoint, AmazonS3Client>();
	private boolean anonymous;
	private KeyPair keyPair;
	private SecretKey secretKey;

	/**
	 * Used for anonymous client.
	 */
	public AmazonS3ClientFactory() {
		this.credentials = null;
	}

	public AmazonS3ClientFactory(AWSCredentialsProvider credentials) {
		this.credentials = credentials;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	public void setKeyPair(KeyPair keyPair) {
		this.keyPair = keyPair;
	}

	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * Method that returns the corresponding {@link AmazonS3} client based
	 * on the {@link org.elasticspring.core.region.ServiceEndpoint}.
	 *
	 * @param s3ServiceEndpoint
	 * 		the {@link org.elasticspring.core.region.ServiceEndpoint} that the client must access.
	 * @return the corresponding {@link AmazonS3} client.
	 */
	public AmazonS3 getClientForServiceEndpoint(ServiceEndpoint s3ServiceEndpoint) {
		AmazonS3Client cachedAmazonS3Client = this.clientsForRegion.get(s3ServiceEndpoint);
		if (cachedAmazonS3Client != null) {
			return cachedAmazonS3Client;
		} else {
			if (isEncryptionClient()) {
				return createAmazonS3EncryptionClient(s3ServiceEndpoint);
			} else {
				AmazonS3Client amazonS3Client = new AmazonS3Client(this.credentials.getCredentials());
				amazonS3Client.setEndpoint(s3ServiceEndpoint.getEndpoint());
				AmazonS3Client previousValue = this.clientsForRegion.putIfAbsent(s3ServiceEndpoint, amazonS3Client);
				return previousValue == null ? amazonS3Client : previousValue;
			}
		}
	}

	private AmazonS3 createAmazonS3EncryptionClient(ServiceEndpoint s3ServiceEndpoint) {
		EncryptionMaterials encryptionMaterials = null;
		if (this.keyPair != null) {
			encryptionMaterials = new EncryptionMaterials(this.keyPair);
		}

		if (this.secretKey != null) {
			encryptionMaterials = new EncryptionMaterials(this.secretKey);
		}

		AmazonS3EncryptionClient amazonS3EncryptionClient;
		if (this.anonymous) {
			amazonS3EncryptionClient = new AmazonS3EncryptionClient(encryptionMaterials);
		} else {
			amazonS3EncryptionClient = new AmazonS3EncryptionClient(this.credentials.getCredentials(), encryptionMaterials);
		}

		AmazonS3Client previousValue = this.clientsForRegion.putIfAbsent(s3ServiceEndpoint, amazonS3EncryptionClient);
		return previousValue == null ? amazonS3EncryptionClient : previousValue;
	}

	private boolean isEncryptionClient() {
		return this.anonymous || this.keyPair != null || this.secretKey != null;
	}
}