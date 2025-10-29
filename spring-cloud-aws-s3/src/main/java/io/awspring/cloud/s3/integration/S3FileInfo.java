/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.s3.integration;

import java.util.Date;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * An Amazon S3 {@link org.springframework.integration.file.remote.FileInfo} implementation.
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class S3FileInfo extends AbstractFileInfo<S3Object> {

	private final S3Object s3Object;

	public S3FileInfo(S3Object s3Object) {
		Assert.notNull(s3Object, "s3Object must not be null");
		this.s3Object = s3Object;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isLink() {
		return false;
	}

	@Override
	public long getSize() {
		return this.s3Object.size();
	}

	@Override
	public long getModified() {
		return this.s3Object.lastModified().getEpochSecond();
	}

	@Override
	public String getFilename() {
		return this.s3Object.key();
	}

	/**
	 * A permissions representation string. Throws {@link UnsupportedOperationException} to avoid extra
	 * {@link software.amazon.awssdk.services.s3.S3Client#getObjectAcl} REST call. The target application may choose to
	 * do that according to its logic.
	 * @return the permissions representation string.
	 */
	@Override
	public String getPermissions() {
		throw new UnsupportedOperationException("Use [AmazonS3.getObjectAcl()] to obtain permissions.");
	}

	@Override
	public S3Object getFileInfo() {
		return this.s3Object;
	}

	@Override
	public String toString() {
		return "FileInfo [isDirectory=" + isDirectory() + ", isLink=" + isLink() + ", Size=" + getSize()
				+ ", ModifiedTime=" + new Date(getModified()) + ", Filename=" + getFilename() + ", RemoteDirectory="
				+ getRemoteDirectory() + "]";
	}

}
