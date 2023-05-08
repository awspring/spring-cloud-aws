/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3.properties;

import org.springframework.lang.Nullable;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Properties related to AWS S3 {@link S3TransferManager}.
 *
 * @author Anton Perez
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class S3TransferManagerProperties {

	/**
	 * Specifies whether to follow symbolic links when traversing the file tree in `S3TransferManager#uploadDirectory`
	 * operation.
	 */
	@Nullable
	private Boolean followSymbolicLinks;

	/**
	 * Specifies the maximum number of levels of directories to visit in `S3TransferManager#uploadDirectory` operation.
	 */
	@Nullable
	private Integer maxDepth;

	@Nullable
	public Boolean getFollowSymbolicLinks() {
		return this.followSymbolicLinks;
	}

	public void setFollowSymbolicLinks(@Nullable Boolean followSymbolicLinks) {
		this.followSymbolicLinks = followSymbolicLinks;
	}

	@Nullable
	public Integer getMaxDepth() {
		return this.maxDepth;
	}

	public void setMaxDepth(@Nullable Integer maxDepth) {
		this.maxDepth = maxDepth;
	}

}
