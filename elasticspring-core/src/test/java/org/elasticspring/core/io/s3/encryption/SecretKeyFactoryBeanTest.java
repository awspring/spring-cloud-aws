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

import javax.crypto.SecretKey;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SecretKeyFactoryBeanTest {

	@Test
	public void testInstantiationWithPasswordAndSalt() throws Exception {
		SecretKeyFactoryBean secretKeyFactoryBean = new SecretKeyFactoryBean("myVerySecurePassword", "mySaltySalt");
		SecretKey secretKey = secretKeyFactoryBean.createInstance();

		Assert.assertNotNull(secretKey);
		Assert.assertNotNull(secretKey.getEncoded());
	}

	@Test
	public void testInstantiationWithEmptyStrings() throws Exception {
		SecretKeyFactoryBean secretKeyFactoryBean = new SecretKeyFactoryBean("myVerySecurePassword", "mySaltySalt");
		SecretKey secretKey = secretKeyFactoryBean.createInstance();

		Assert.assertNotNull(secretKey);
		Assert.assertNotNull(secretKey.getEncoded());
	}

	@Test
	public void samePasswordAndSaltReturnsSameSecretKey() throws Exception {
		SecretKeyFactoryBean secretKeyFactoryBean1 = new SecretKeyFactoryBean("myVerySecurePassword", "mySaltySalt");
		SecretKey secretKey1 = secretKeyFactoryBean1.createInstance();

		SecretKeyFactoryBean secretKeyFactoryBean2 = new SecretKeyFactoryBean("myVerySecurePassword", "mySaltySalt");
		SecretKey secretKey2 = secretKeyFactoryBean2.createInstance();

		Assert.assertEquals(secretKey1, secretKey2);
	}
}
