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
	public static final String PREFIX = "aws";

	@Nullable
	private final String roleArn;

	@Nullable
	private final Path webIdentityTokenFile;

	@Nullable
	private final Boolean isAsyncCredentialsUpdate;

	@Nullable
	private final String roleSessionName;

	public StsProperties(@Nullable String roleArn, @Nullable Path webIdentityTokenFile, @Nullable Boolean isAsyncCredentialsUpdate, @Nullable String roleSessionName) {
		this.roleArn = roleArn;
		this.webIdentityTokenFile = webIdentityTokenFile;

		this.isAsyncCredentialsUpdate = isAsyncCredentialsUpdate;
		this.roleSessionName = roleSessionName;
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
			logger.debug("Role ARN not set. To configure use " + PREFIX + ".roleArn");
			return false;
		}
		if (Objects.isNull(webIdentityTokenFile)) {
			logger.debug("Web identity token not set. To configure use " + PREFIX + ".webIdentityTokenFile");
			return false;
		}
		if (!Files.exists(webIdentityTokenFile)) {
			logger.debug("Web identity token not found. To configure use " + PREFIX + ".webIdentityTokenFile");
			return false;
		}
		return true;
	}
}
