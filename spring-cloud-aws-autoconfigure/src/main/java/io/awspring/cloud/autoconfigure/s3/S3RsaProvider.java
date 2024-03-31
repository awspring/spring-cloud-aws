package io.awspring.cloud.autoconfigure.s3;

import java.security.KeyPair;

public interface S3RsaProvider {

	KeyPair generateKeyPair();
}
