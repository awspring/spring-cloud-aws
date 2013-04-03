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

package org.elasticspring.context.config.xml;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SecretKeyFactoryBeanStub extends AbstractFactoryBean<SecretKey> {

	@Override
	public Class<?> getObjectType() {
		return SecretKey.class;
	}

	@Override
	protected SecretKey createInstance() throws Exception {
		return new SecretKeySpec("DummyPassword".getBytes("UTF-8"), "AES");
	}
}
