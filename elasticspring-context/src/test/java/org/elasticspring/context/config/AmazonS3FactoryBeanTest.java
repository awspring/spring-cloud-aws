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
import com.amazonaws.services.s3.AmazonS3Client;
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
	public void testBeanCreation() throws Exception {
		AWSCredentialsProvider awsCredentialsProviderMock = Mockito.mock(AWSCredentialsProvider.class);
		AmazonS3FactoryBean amazonS3FactoryBean = new AmazonS3FactoryBean(awsCredentialsProviderMock);
		AmazonS3 instance = amazonS3FactoryBean.createInstance();

		Assert.assertTrue(AmazonS3Client.class.isInstance(instance));
	}

}
