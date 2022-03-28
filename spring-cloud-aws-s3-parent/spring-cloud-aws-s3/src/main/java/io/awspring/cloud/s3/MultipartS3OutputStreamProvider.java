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

package io.awspring.cloud.s3;

import java.util.Map;

import edu.colorado.cires.cmg.s3out.AwsS3ClientMultipartUpload;
import edu.colorado.cires.cmg.s3out.S3ClientMultipartUpload;
import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class MultipartS3OutputStreamProvider implements S3OutputStreamProvider {

	@Nullable
	private final Integer partSizeMib;

	@Nullable
	private final Boolean autoComplete;

	private final S3ClientMultipartUpload s3ClientMultipartUpload;

	public MultipartS3OutputStreamProvider(S3Client s3Client) {
		this(null, null, s3Client);
	}

	public MultipartS3OutputStreamProvider(@Nullable Integer maxSizeMib, @Nullable Boolean autoComplete,
			S3Client s3Client) {
		this.partSizeMib = maxSizeMib;
		this.autoComplete = autoComplete;
		this.s3ClientMultipartUpload = AwsS3ClientMultipartUpload.builder().s3(s3Client).build();
	}

	@Override
	public S3OutputStream create(@NonNull String bucket, @NonNull String key, @Nullable Map<String, String> metadata) {
		edu.colorado.cires.cmg.s3out.S3OutputStream.Builder builder = edu.colorado.cires.cmg.s3out.S3OutputStream
				.builder().s3(s3ClientMultipartUpload).bucket(bucket).key(key);
		if (partSizeMib != null) {
			builder = builder.partSizeMib(partSizeMib);
		}
		if (autoComplete != null) {
			builder.autoComplete(autoComplete);
		}
		return new MultipartUploadS3OutputStream(builder.build());
	}

}
