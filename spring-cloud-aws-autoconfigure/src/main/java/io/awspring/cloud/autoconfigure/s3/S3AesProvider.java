package io.awspring.cloud.autoconfigure.s3;

import javax.crypto.SecretKey;
import java.security.KeyPair;

public interface S3AesProvider {

	SecretKey generateSecretKey();
}
