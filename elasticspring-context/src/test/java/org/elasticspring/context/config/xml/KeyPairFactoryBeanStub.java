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

import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class KeyPairFactoryBeanStub extends AbstractFactoryBean<KeyPair> {


	@Override
	public Class<?> getObjectType() {
		return KeyPair.class;
	}

	@Override
	protected KeyPair createInstance() throws Exception {
		return KeyPairGenerator.getInstance("RSA").generateKeyPair();
	}
}
