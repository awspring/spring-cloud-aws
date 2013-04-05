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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class AmazonS3FactoryBeanTest {

	@Test
	public void testInstantiationWithKeyPairRef() throws Exception {
		AmazonS3FactoryBean factory = getAmazonS3ClientWithCredentialsProviderFactory();

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		factory.setKeyPair(keyPair);

		AmazonS3 amazonS3Client = factory.createInstance();
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	@Test
	public void testInstantiationWithSecretKeyRef() throws Exception {
		AmazonS3FactoryBean factory = getAmazonS3ClientWithCredentialsProviderFactory();

		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey secretKey = keyGenerator.generateKey();
		factory.setSecretKey(secretKey);

		AmazonS3 amazonS3Client = factory.createInstance();
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	@Test
	public void testInstantiationWithAnonymousFlagAndSecretKeyRef() throws Exception {
		AmazonS3FactoryBean factory = new AmazonS3FactoryBean();

		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey secretKey = keyGenerator.generateKey();
		factory.setSecretKey(secretKey);
		factory.setAnonymous(true);

		AmazonS3 amazonS3Client = factory.createInstance();
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	@Test
	public void testInstantiationWithAnonymousFlagAndKeyPairRef() throws Exception {
		AmazonS3FactoryBean factory = new AmazonS3FactoryBean();

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		factory.setKeyPair(keyPair);
		factory.setAnonymous(true);

		AmazonS3 amazonS3Client = factory.createInstance();
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	private AmazonS3FactoryBean getAmazonS3ClientWithCredentialsProviderFactory() {
		AWSCredentialsProvider awsCredentialsProviderMock = Mockito.mock(AWSCredentialsProvider.class);
		return new AmazonS3FactoryBean(awsCredentialsProviderMock);
	}

}
