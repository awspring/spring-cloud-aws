package io.awspring.cloud.autoconfigure.s3.properties;

public class S3PluginProperties {

	/**
	 * If set to false if Access Grants does not find/return permissions, S3Client won't try to determine if policies
	 * grant access If set to true fallback policies S3/IAM will be evaluated.
	 */
	private boolean enableFallback;

	public boolean getEnableFallback() {
		return enableFallback;
	}

	public void setEnableFallback(boolean enableFallback) {
		this.enableFallback = enableFallback;
	}
}
