/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class SimpleStorageResource extends AbstractResource implements WritableResource {

	private final String bucketName;
	private final String objectName;
	private final AmazonS3 amazonS3;
	private ObjectMetadata objectMetadata;

	SimpleStorageResource(String bucketName, String objectName, AmazonS3 amazonS3) {
		this.bucketName = bucketName;
		this.objectName = objectName;
		this.amazonS3 = amazonS3;
		try {
			this.objectMetadata = amazonS3.getObjectMetadata(bucketName, objectName);
		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == 404) {
				this.objectMetadata = null;
			} else {
				throw e;
			}
		}
	}

	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder("Amazon s3 resource [bucket='");
		builder.append(this.bucketName);
		builder.append("' and object='");
		builder.append(this.objectName);
		builder.append("']");
		return builder.toString();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.amazonS3.getObject(this.bucketName, this.objectName).getObjectContent();
	}

	@Override
	public long lastModified() throws IOException {
		assertThatResourceExists();
		return this.objectMetadata.getLastModified().getTime();
	}

	@Override
	public long contentLength() throws IOException {
		assertThatResourceExists();
		return this.objectMetadata.getContentLength();
	}

	@Override
	public boolean exists() {
		return this.objectMetadata != null;
	}

	@Override
	public String getFilename() throws IllegalStateException {
		return this.objectName;
	}

	private void assertThatResourceExists() throws FileNotFoundException {
		if (this.objectMetadata == null) {
			throw new FileNotFoundException(new StringBuilder().
					append("Resource with bucket='").
					append(this.bucketName).
					append("' and objectName='").
					append(this.objectName).
					append("' not found!").
					toString());
		}
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new DecoratingOutputStream();
	}


	private class DecoratingOutputStream extends OutputStream {

		/*
			The minimum size for a multi part is 5 MB, hence the buffer size of 5 MB
		 */
		private static final int BUFFER_SIZE = 1024 * 1024 * 5;

		private final Object monitor = new Object();

		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
		private final List<PartETag> eTags = new ArrayList<PartETag>();
		private InitiateMultipartUploadResult initiateMultipartUploadResult;

		private int partNumber = 1;

		@Override
		public void write(int b) throws IOException {
			synchronized (this.monitor) {
				if (this.outputStream.size() == BUFFER_SIZE) {
					if (this.initiateMultipartUploadResult == null) {
						this.initiateMultipartUploadResult = SimpleStorageResource.this.amazonS3.initiateMultipartUpload(
								new InitiateMultipartUploadRequest(SimpleStorageResource.this.bucketName, SimpleStorageResource.this.objectName));
					}

					UploadPartResult uploadPartResult = SimpleStorageResource.this.amazonS3.uploadPart(new UploadPartRequest().withBucketName(this.initiateMultipartUploadResult.getBucketName()).
							withKey(this.initiateMultipartUploadResult.getKey()).
							withUploadId(this.initiateMultipartUploadResult.getUploadId()).
							withInputStream(new ByteArrayInputStream(this.outputStream.toByteArray())).
							withPartNumber(this.partNumber).
							withPartSize(this.outputStream.size()));
					this.eTags.add(uploadPartResult.getPartETag());
					this.partNumber++;
					this.outputStream.reset();
				}
				this.outputStream.write(b);
			}
		}

		@Override
		public void close() throws IOException {
			synchronized (this.monitor) {
				if (this.initiateMultipartUploadResult != null) {
					UploadPartResult uploadPartResult = SimpleStorageResource.this.amazonS3.uploadPart(new UploadPartRequest().withBucketName(this.initiateMultipartUploadResult.getBucketName()).
							withKey(this.initiateMultipartUploadResult.getKey()).
							withUploadId(this.initiateMultipartUploadResult.getUploadId()).
							withInputStream(new ByteArrayInputStream(this.outputStream.toByteArray())).
							withLastPart(true).
							withPartSize(this.outputStream.size()).
							withPartNumber(this.partNumber));
					this.eTags.add(uploadPartResult.getPartETag());
					SimpleStorageResource.this.amazonS3.completeMultipartUpload(new CompleteMultipartUploadRequest(this.initiateMultipartUploadResult.getBucketName(),
							this.initiateMultipartUploadResult.getKey(), this.initiateMultipartUploadResult.getUploadId(), this.eTags));
				} else {

					ObjectMetadata objectMetadata = new ObjectMetadata();
					objectMetadata.setContentLength(this.outputStream.size());

					SimpleStorageResource.this.amazonS3.putObject(SimpleStorageResource.this.bucketName, SimpleStorageResource.this.objectName,
							new ByteArrayInputStream(this.outputStream.toByteArray()), objectMetadata);
				}
				this.outputStream.close();
			}
		}
	}
}