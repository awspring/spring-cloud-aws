/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.autoconfigure.s3.provider;

import io.awspring.cloud.autoconfigure.s3.S3AesProvider;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class MyAesProvider implements S3AesProvider {
	@Override
	public SecretKey generateSecretKey() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			return keyGenerator.generateKey();
		}
		catch (Exception e) {
			return null;
		}
	}
}
