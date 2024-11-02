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

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditional for creating {@link software.amazon.encryption.s3.S3EncryptionClient}. Will only create
 * S3EncryptionClient if one of following is true.
 * @author Matej Nedic
 * @since 3.3.0
 */
public class S3EncryptionConditional extends AnyNestedCondition {
	public S3EncryptionConditional() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnBean(S3RsaProvider.class)
	static class RSAProviderCondition {
	}

	@ConditionalOnBean(S3AesProvider.class)
	static class AESProviderCondition {
	}

	@ConditionalOnProperty(name = "spring.cloud.aws.s3.encryption.keyId")
	static class KmsKeyProperty {
	}
}
