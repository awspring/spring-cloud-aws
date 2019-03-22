/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.core.io.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.BinaryUtils;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;

/**
 * {@link org.springframework.core.io.Resource} implementation for
 * {@code com.amazonaws.services.s3.model.S3Object} handles. Implements the extended
 * {@link WritableResource} interface.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageResource extends AbstractResource implements WritableResource {

	private final String bucketName;

	private final String objectName;

	private final String versionId;

	private final AmazonS3 amazonS3;

	private final TaskExecutor taskExecutor;

	private volatile ObjectMetadata objectMetadata;

	public SimpleStorageResource(AmazonS3 amazonS3, String bucketName, String objectName,
			TaskExecutor taskExecutor) {
		this(amazonS3, bucketName, objectName, taskExecutor, null);
	}

	public SimpleStorageResource(AmazonS3 amazonS3, String bucketName, String objectName,
			TaskExecutor taskExecutor, String versionId) {
		this.amazonS3 = AmazonS3ProxyFactory.createProxy(amazonS3);
		this.bucketName = bucketName;
		this.objectName = objectName;
		this.taskExecutor = taskExecutor;
		this.versionId = versionId;
	}

	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder("Amazon s3 resource [bucket='");
		builder.append(this.bucketName);
		builder.append("' and object='");
		builder.append(this.objectName);
		if (this.versionId != null) {
			builder.append("' and versionId='");
			builder.append(this.versionId);
		}
		builder.append("']");
		return builder.toString();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		GetObjectRequest getObjectRequest = new GetObjectRequest(this.bucketName,
				this.objectName);
		if (this.versionId != null) {
			getObjectRequest.setVersionId(this.versionId);
		}
		return this.amazonS3.getObject(getObjectRequest).getObjectContent();
	}

	@Override
	public boolean exists() {
		return getObjectMetadata() != null;
	}

	@Override
	public long contentLength() throws IOException {
		return getRequiredObjectMetadata().getContentLength();
	}

	@Override
	public long lastModified() throws IOException {
		return getRequiredObjectMetadata().getLastModified().getTime();
	}

	@Override
	public String getFilename() throws IllegalStateException {
		return this.objectName;
	}

	@Override
	public URL getURL() throws IOException {
		Region region = this.amazonS3.getRegion().toAWSRegion();
		return new URL("https", region.getServiceEndpoint(AmazonS3Client.S3_SERVICE_NAME),
				"/" + this.bucketName + "/" + this.objectName);
	}

	@Override
	public File getFile() throws IOException {
		throw new UnsupportedOperationException(
				"Amazon S3 resource can not be resolved to java.io.File objects.Use "
						+ "getInputStream() to retrieve the contents of the object!");
	}

	private ObjectMetadata getRequiredObjectMetadata() throws FileNotFoundException {
		ObjectMetadata metadata = getObjectMetadata();
		if (metadata == null) {
			StringBuilder builder = new StringBuilder().append("Resource with bucket='")
					.append(this.bucketName).append("' and objectName='")
					.append(this.objectName);
			if (this.versionId != null) {
				builder.append("' and versionId='");
				builder.append(this.versionId);
			}
			builder.append("' not found!");

			throw new FileNotFoundException(builder.toString());
		}
		return metadata;
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new SimpleStorageOutputStream();
	}

	@Override
	public SimpleStorageResource createRelative(String relativePath) throws IOException {
		String relativeKey = this.objectName + "/" + relativePath;
		return new SimpleStorageResource(this.amazonS3, this.bucketName, relativeKey,
				this.taskExecutor);
	}

	private ObjectMetadata getObjectMetadata() {
		if (this.objectMetadata == null) {
			try {
				GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
						this.bucketName, this.objectName);
				if (this.versionId != null) {
					metadataRequest.setVersionId(this.versionId);
				}
				this.objectMetadata = this.amazonS3.getObjectMetadata(metadataRequest);
			}
			catch (AmazonS3Exception e) {
				// Catch 404 (object not found) and 301 (bucket not found, moved
				// permanently)
				if (e.getStatusCode() == 404 || e.getStatusCode() == 301) {
					this.objectMetadata = null;
				}
				else {
					throw e;
				}
			}
		}
		return this.objectMetadata;
	}

	private class SimpleStorageOutputStream extends OutputStream {

		// The minimum size for a multi part is 5 MB, hence the buffer size of 5 MB
		private static final int BUFFER_SIZE = 1024 * 1024 * 5;

		private final Object monitor = new Object();

		private final CompletionService<UploadPartResult> completionService;

		@SuppressWarnings("FieldMayBeFinal")
		private ByteArrayOutputStream currentOutputStream = new ByteArrayOutputStream(
				BUFFER_SIZE);

		private int partNumberCounter = 1;

		private InitiateMultipartUploadResult multiPartUploadResult;

		SimpleStorageOutputStream() {
			this.completionService = new ExecutorCompletionService<>(
					new ExecutorServiceAdapter(SimpleStorageResource.this.taskExecutor));
		}

		@Override
		public void write(int b) throws IOException {
			synchronized (this.monitor) {
				if (this.currentOutputStream.size() == BUFFER_SIZE) {
					initiateMultiPartIfNeeded();
					this.completionService.submit(new UploadPartResultCallable(
							SimpleStorageResource.this.amazonS3,
							this.currentOutputStream.toByteArray(),
							this.currentOutputStream.size(),
							SimpleStorageResource.this.bucketName,
							SimpleStorageResource.this.objectName,
							this.multiPartUploadResult.getUploadId(),
							this.partNumberCounter++, false));
					this.currentOutputStream.reset();
				}
				this.currentOutputStream.write(b);
			}
		}

		@Override
		public void close() throws IOException {
			synchronized (this.monitor) {
				if (this.currentOutputStream == null) {
					return;
				}

				if (isMultiPartUpload()) {
					finishMultiPartUpload();
				}
				else {
					finishSimpleUpload();
				}
			}
		}

		private boolean isMultiPartUpload() {
			return this.multiPartUploadResult != null;
		}

		private void finishSimpleUpload() {
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(this.currentOutputStream.size());

			byte[] content = this.currentOutputStream.toByteArray();
			try {
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");
				String md5Digest = BinaryUtils.toBase64(messageDigest.digest(content));
				objectMetadata.setContentMD5(md5Digest);
			}
			catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(
						"MessageDigest could not be initialized because it uses an unknown algorithm",
						e);
			}

			SimpleStorageResource.this.amazonS3.putObject(
					SimpleStorageResource.this.bucketName,
					SimpleStorageResource.this.objectName,
					new ByteArrayInputStream(content), objectMetadata);

			// Release the memory early
			this.currentOutputStream = null;
		}

		private void finishMultiPartUpload() throws IOException {
			this.completionService.submit(
					new UploadPartResultCallable(SimpleStorageResource.this.amazonS3,
							this.currentOutputStream.toByteArray(),
							this.currentOutputStream.size(),
							SimpleStorageResource.this.bucketName,
							SimpleStorageResource.this.objectName,
							this.multiPartUploadResult.getUploadId(),
							this.partNumberCounter, true));
			try {
				List<PartETag> partETags = getMultiPartsUploadResults();
				SimpleStorageResource.this.amazonS3
						.completeMultipartUpload(new CompleteMultipartUploadRequest(
								this.multiPartUploadResult.getBucketName(),
								this.multiPartUploadResult.getKey(),
								this.multiPartUploadResult.getUploadId(), partETags));
			}
			catch (ExecutionException e) {
				abortMultiPartUpload();
				throw new IOException("Multi part upload failed ", e.getCause());
			}
			catch (InterruptedException e) {
				abortMultiPartUpload();
				Thread.currentThread().interrupt();
			}
			finally {
				this.currentOutputStream = null;
			}
		}

		private void initiateMultiPartIfNeeded() {
			if (this.multiPartUploadResult == null) {
				this.multiPartUploadResult = SimpleStorageResource.this.amazonS3
						.initiateMultipartUpload(new InitiateMultipartUploadRequest(
								SimpleStorageResource.this.bucketName,
								SimpleStorageResource.this.objectName));
			}
		}

		private void abortMultiPartUpload() {
			if (isMultiPartUpload()) {
				SimpleStorageResource.this.amazonS3
						.abortMultipartUpload(new AbortMultipartUploadRequest(
								this.multiPartUploadResult.getBucketName(),
								this.multiPartUploadResult.getKey(),
								this.multiPartUploadResult.getUploadId()));
			}
		}

		private List<PartETag> getMultiPartsUploadResults()
				throws ExecutionException, InterruptedException {
			List<PartETag> result = new ArrayList<>(this.partNumberCounter);
			for (int i = 0; i < this.partNumberCounter; i++) {
				Future<UploadPartResult> uploadPartResultFuture = this.completionService
						.take();
				result.add(uploadPartResultFuture.get().getPartETag());
			}
			return result;
		}

		private final class UploadPartResultCallable
				implements Callable<UploadPartResult> {

			private final AmazonS3 amazonS3;

			private final int contentLength;

			private final int partNumber;

			private final boolean last;

			private final String bucketName;

			private final String key;

			private final String uploadId;

			@SuppressWarnings("FieldMayBeFinal")
			private byte[] content;

			private UploadPartResultCallable(AmazonS3 amazon, byte[] content,
					int writtenDataSize, String bucketName, String key, String uploadId,
					int partNumber, boolean last) {
				this.amazonS3 = amazon;
				this.content = content;
				this.contentLength = writtenDataSize;
				this.partNumber = partNumber;
				this.last = last;
				this.bucketName = bucketName;
				this.key = key;
				this.uploadId = uploadId;
			}

			@Override
			public UploadPartResult call() throws Exception {
				try {
					return this.amazonS3.uploadPart(new UploadPartRequest()
							.withBucketName(this.bucketName).withKey(this.key)
							.withUploadId(this.uploadId)
							.withInputStream(new ByteArrayInputStream(this.content))
							.withPartNumber(this.partNumber).withLastPart(this.last)
							.withPartSize(this.contentLength));
				}
				finally {
					// Release the memory, as the callable may still live inside the
					// CompletionService which would cause
					// an exhaustive memory usage
					this.content = null;
				}
			}

		}

	}

}
