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
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import javax.crypto.SecretKey;
import java.security.KeyPair;

public class AmazonS3FactoryBean extends AbstractFactoryBean<AmazonS3> {

	private final AWSCredentialsProvider credentials;
	private boolean anonymous;
	private KeyPair keyPair;
	private SecretKey secretKey;

	@RuntimeUse
	public AmazonS3FactoryBean() {
		// For anonymous clients
		this.credentials = null;
	}

	@RuntimeUse
	public AmazonS3FactoryBean(AWSCredentialsProvider credentials) {
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

	@Override
	public Class<?> getObjectType() {
		return AmazonS3.class;
	}

	@Override
	protected AmazonS3 createInstance() throws Exception {
		if (isEncryptionClient()) {
			return createAmazonS3EncryptionClient();
		} else {
			return new AmazonS3Client(this.credentials.getCredentials());
		}
	}

	private AmazonS3 createAmazonS3EncryptionClient() {
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

		return amazonS3EncryptionClient;
	}

	private boolean isEncryptionClient() {
		return this.anonymous || this.keyPair != null || this.secretKey != null;
	}

	@Override
	protected void destroyInstance(AmazonS3 instance) throws Exception {
		if (instance instanceof AmazonWebServiceClient) {
			((AmazonWebServiceClient) instance).shutdown();
		}
	}
}
