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

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SecretKeyFactoryBean extends AbstractFactoryBean<SecretKey> {

	private final String password;
	private final String salt;

	/**
	 *
	 * @param password the password used for the AES key generation - must not be null
	 * @param salt the salt used for the AES key generation - must not be null
	 */
	public SecretKeyFactoryBean(String password, String salt) {
		Assert.notNull(password, "password must not be null");
		Assert.notNull(salt, "salt must not be null");
		this.password = password;
		this.salt = salt;
	}

	@Override
	public Class<?> getObjectType() {
		return SecretKey.class;
	}

	@Override
	protected SecretKey createInstance() throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		KeySpec spec = new PBEKeySpec(this.password.toCharArray(), this.salt.getBytes("UTF-8"), 65536, 256);
		SecretKey tmp = factory.generateSecret(spec);
		return new SecretKeySpec(tmp.getEncoded(), "AES");
	}
}
