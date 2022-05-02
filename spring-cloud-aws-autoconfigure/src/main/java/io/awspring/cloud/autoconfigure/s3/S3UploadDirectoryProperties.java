package io.awspring.cloud.autoconfigure.s3;

import org.springframework.lang.Nullable;

/**
 * Properties related to AWS S3 TransferManager uploadDirectory.
 *
 * @author Anton Perez
 */
public class S3UploadDirectoryProperties {
	@Nullable
	private Boolean recursive;
	@Nullable
	private Boolean followSymbolicLinks;
	@Nullable
	private Integer maxDepth;

	@Nullable
	public Boolean getRecursive() {
		return recursive;
	}

	public void setRecursive(@Nullable Boolean recursive) {
		this.recursive = recursive;
	}

	@Nullable
	public Boolean getFollowSymbolicLinks() {
		return followSymbolicLinks;
	}

	public void setFollowSymbolicLinks(@Nullable Boolean followSymbolicLinks) {
		this.followSymbolicLinks = followSymbolicLinks;
	}

	@Nullable
	public Integer getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(@Nullable Integer maxDepth) {
		this.maxDepth = maxDepth;
	}
}
