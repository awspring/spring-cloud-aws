package io.awspring.cloud.autoconfigure.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Properties related to AWS Sts Credentials.
 * It the properties are not configured, it will default to the EKS values from:
 * <a href="https://docs.aws.amazon.com/eks/latest/userguide/pod-configuration.html">
 *
 * @author Eduan Bekker
 * @since 3.0.0
 */
@ConfigurationProperties(prefix = StsProperties.PREFIX)
public class StsProperties {
	private static final Logger logger = LoggerFactory.getLogger(StsProperties.class);

	/**
	 * The prefix used for AWS STS related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.sts";

	/**
	 * The default environment variable name in EKS.
	 */
	public static final String AWS_ROLE_ARN_ENV_VARIABLE = "AWS_ROLE_ARN";

	/**
	 * The default environment variable name in EKS.
	 */
	public static final String AWS_WEB_IDENTITY_TOKEN_FILE_ENV_VARIABLE = "AWS_WEB_IDENTITY_TOKEN_FILE";

	@Nullable
	private String roleArn;

	@Nullable
	private Path webIdentityTokenFile;

	@Nullable
	private final Boolean isAsyncCredentialsUpdate;

	@Nullable
	private final String roleSessionName;

	public StsProperties(@Nullable String roleArn, @Nullable Path webIdentityTokenFile, @Nullable Boolean isAsyncCredentialsUpdate, @Nullable String roleSessionName) {
		this.isAsyncCredentialsUpdate = isAsyncCredentialsUpdate;
		this.roleSessionName = roleSessionName;

		this.roleArn = roleArn;
		// If the spring.cloud.aws.sts.role-arn isn't configured, fall back to default in EKS
		if (this.roleArn == null) {
			this.roleArn = System.getenv(AWS_ROLE_ARN_ENV_VARIABLE);
		}

		// If the spring.cloud.aws.sts.aws-web-identity-token-file isn't configured, fall back to default in EKS
		this.webIdentityTokenFile = webIdentityTokenFile;
		if (this.webIdentityTokenFile == null) {
			String tokenFileLocation = System.getenv(AWS_WEB_IDENTITY_TOKEN_FILE_ENV_VARIABLE);
			if (StringUtils.hasText(tokenFileLocation)) {
				this.webIdentityTokenFile = Path.of(System.getenv(AWS_WEB_IDENTITY_TOKEN_FILE_ENV_VARIABLE));
			}
		}
	}

	@Nullable
	public Boolean isAsyncCredentialsUpdate() {
		return isAsyncCredentialsUpdate;
	}

	@Nullable
	public String getRoleSessionName() {
		return roleSessionName;
	}

	@Nullable
	public String getRoleArn() {
		return roleArn;
	}

	@Nullable
	public Path getWebIdentityTokenFile() {
		return webIdentityTokenFile;
	}

	public boolean isValid() {
		if (!StringUtils.hasText(roleArn)) {
			logger.debug("Role ARN not set. To configure use " + PREFIX + ".role-arn or " + AWS_ROLE_ARN_ENV_VARIABLE + " env variable");
			return false;
		}
		if (Objects.isNull(webIdentityTokenFile) || !Files.exists(webIdentityTokenFile)) {
			logger.debug("Web identity token not set or not found To configure use " + PREFIX + ".web-identity-token or " + AWS_WEB_IDENTITY_TOKEN_FILE_ENV_VARIABLE + " env variable");
			return false;
		}
		return true;
	}
}
