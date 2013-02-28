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

package org.elasticspring.core.io.s3.encryption;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.security.KeyPair;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class KeyPairFactoryBeanTest {

	@Test
	public void testInstantiationWithKeyPairResource() throws Exception {
		ClassPathResource privateKey = new ClassPathResource("/org/elasticspring/core/io/s3/encryption/private-key.pem");
		ClassPathResource publicKey = new ClassPathResource("/org/elasticspring/core/io/s3/encryption/public-key.pem");
		KeyPairFactoryBean keyPairFactoryBean = new KeyPairFactoryBean(privateKey, publicKey);
		keyPairFactoryBean.afterPropertiesSet();

		KeyPair keyPair = keyPairFactoryBean.createInstance();
		Assert.assertNotNull(keyPair);
		Assert.assertNotNull(keyPair.getPrivate());
		Assert.assertNotNull(keyPair.getPublic());
	}

	@Test
	public void multipleInstantiationWithSameResourcesReturnsSameKeyPair() throws Exception {
		ClassPathResource privateKey = new ClassPathResource("/org/elasticspring/core/io/s3/encryption/private-key.pem");
		ClassPathResource publicKey = new ClassPathResource("/org/elasticspring/core/io/s3/encryption/public-key.pem");

		KeyPairFactoryBean keyPairFactoryBean1 = new KeyPairFactoryBean(privateKey, publicKey);
		keyPairFactoryBean1.afterPropertiesSet();
		KeyPair keyPair1 = keyPairFactoryBean1.createInstance();

		KeyPairFactoryBean keyPairFactoryBean2 = new KeyPairFactoryBean(privateKey, publicKey);
		keyPairFactoryBean2.afterPropertiesSet();
		KeyPair keyPair2 = keyPairFactoryBean2.createInstance();

		Assert.assertEquals(keyPair1.getPublic(), keyPair2.getPublic());
		Assert.assertEquals(keyPair1.getPrivate(), keyPair2.getPrivate());
	}

	@Test(expected = Exception.class)
	public void testInstantiationWithInvalidKeyPairResource() throws Exception {
		ClassPathResource privateKey = new ClassPathResource("/org/elasticspring/core/io/s3/encryption/invalid-private-key.pem");
		ClassPathResource publicKey = new ClassPathResource("/org/elasticspring/core/io/s3/encryption/invalid-public-key.pem");
		KeyPairFactoryBean keyPairFactoryBean = new KeyPairFactoryBean(privateKey, publicKey);
		keyPairFactoryBean.afterPropertiesSet();

		keyPairFactoryBean.createInstance();
	}
}
