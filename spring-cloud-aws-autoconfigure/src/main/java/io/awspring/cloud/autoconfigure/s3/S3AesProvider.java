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
package io.awspring.cloud.autoconfigure.s3;

import javax.crypto.SecretKey;

/**
 * Interface for providing {@link SecretKey} when configuring {@link software.amazon.encryption.s3.S3EncryptionClient}.
 * Required when encrypting files server side with AES. Secret Key should be stored in secure storage, for example AWS
 * Secrets Manager.
 * @author Matej Nedic
 * @since 3.3.0
 */
public interface S3AesProvider {

	/**
	 * Provides SecretKey that will be used to configure {@link software.amazon.encryption.s3.S3EncryptionClient}.
	 * Advised to fetch and return SecretKey in this method from Secured Storage.
	 * @return KeyPair that will be used for encryption/decryption.
	 */
	SecretKey generateSecretKey();
}
