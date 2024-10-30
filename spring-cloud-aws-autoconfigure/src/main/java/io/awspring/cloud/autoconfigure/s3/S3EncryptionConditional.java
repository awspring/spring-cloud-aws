package io.awspring.cloud.autoconfigure.s3;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditional for creating {@link software.amazon.encryption.s3.S3EncryptionClient}.
 * Will only create S3EncryptionClient if one of following is true.
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
