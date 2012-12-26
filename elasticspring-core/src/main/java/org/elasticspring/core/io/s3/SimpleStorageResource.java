 /*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
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
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class SimpleStorageResource extends AbstractResource implements WritableResource {

	private static final int DEFAULT_CORE_POOL_SIZE = 10;
	private static final int DEFAULT_QUEUE_SIZE = 10;
	public static final String THREAD_NAME_PREFIX = "s3UploadThread";

	private final String bucketName;
	private final String objectName;
	private final AmazonS3 amazonS3;
	private final AsyncTaskExecutor taskExecutor;

	private ObjectMetadata objectMetadata;

	public SimpleStorageResource(String bucketName, String objectName, AmazonS3 amazonS3) {
		this.bucketName = bucketName;
		this.objectName = objectName;
		this.amazonS3 = amazonS3;

		initAmazonS3();
		this.taskExecutor = getDefaultTaskExecutor();
	}

	private AsyncTaskExecutor getDefaultTaskExecutor() {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setCorePoolSize(DEFAULT_CORE_POOL_SIZE);
		threadPoolTaskExecutor.setMaxPoolSize(DEFAULT_CORE_POOL_SIZE);
		threadPoolTaskExecutor.setQueueCapacity(DEFAULT_QUEUE_SIZE);
		threadPoolTaskExecutor.setThreadNamePrefix(THREAD_NAME_PREFIX);
		threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		threadPoolTaskExecutor.initialize();
		return threadPoolTaskExecutor;
	}

	private void initAmazonS3() {
		// TODO find a cleaner way to set the endpoint
		this.amazonS3.setEndpoint("s3-" + this.amazonS3.getBucketLocation(bucketName) + ".amazonaws.com");

		try {
			this.objectMetadata = this.amazonS3.getObjectMetadata(bucketName, objectName);
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

		private ByteArrayOutputStream currentOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);

		private Object multiPartUploadResultMonitor = new Object();
		private Object writeCloseOutputStreamMonitor = new Object();

		private final List<PartETag> eTags = new ArrayList<PartETag>();
		private final List<Future<Void>> multiPartUploadFutures = new ArrayList<Future<Void>>();
		private volatile InitiateMultipartUploadResult multiPartUploadResult;

		private AtomicInteger partNumberCounter = new AtomicInteger(1);

		private DecoratingOutputStream() {
		}

		@Override
		public void write(int b) throws IOException {
			synchronized (this.writeCloseOutputStreamMonitor) {
				if (this.currentOutputStream.size() == BUFFER_SIZE) {
					verifyThatMultiPartUploadResultIsSet();
					Future<Void> future = SimpleStorageResource.this.taskExecutor.submit(
							uploadMultiPart(DecoratingOutputStream.this.currentOutputStream, DecoratingOutputStream.this.partNumberCounter.getAndIncrement()));
					addMultiPartUploadFuture(future);

					this.currentOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
				}
				this.currentOutputStream.write(b);
			}
		}

		private Callable<Void> uploadMultiPart(final ByteArrayOutputStream outputStream, final int partNumber) throws IOException {
			return new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					UploadPartResult uploadPartResult = SimpleStorageResource.this.amazonS3.uploadPart(new UploadPartRequest().withBucketName(DecoratingOutputStream.this.multiPartUploadResult.getBucketName()).
							withKey(DecoratingOutputStream.this.multiPartUploadResult.getKey()).
							withUploadId(DecoratingOutputStream.this.multiPartUploadResult.getUploadId()).
							withInputStream(new ByteArrayInputStream(outputStream.toByteArray())).
							withPartNumber(partNumber).
							withPartSize(outputStream.size()));
					addETags(uploadPartResult);

					outputStream.close();
					return null;
				}
			};
		}

		private Callable<Void> uploadLastMultiPart(final ByteArrayOutputStream outputStream, final int partNumber) throws IOException {
			final Callable<Void> callable = new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					UploadPartResult uploadPartResult = SimpleStorageResource.this.amazonS3.uploadPart(
							new UploadPartRequest().withBucketName(DecoratingOutputStream.this.multiPartUploadResult.getBucketName()).
									withKey(DecoratingOutputStream.this.multiPartUploadResult.getKey()).
									withUploadId(DecoratingOutputStream.this.multiPartUploadResult.getUploadId()).
									withInputStream(new ByteArrayInputStream(outputStream.toByteArray())).
									withLastPart(true).
									withPartSize(outputStream.size()).
									withPartNumber(partNumber));
					addETags(uploadPartResult);

					outputStream.close();
					return null;
				}
			};
			return callable;
		}

		@Override
		public void close() throws IOException {
			if (this.multiPartUploadResult != null) {
				closeMultiPartUpload();
			} else {
				closeSimpleUpload();
			}
		}

		private void closeSimpleUpload() throws IOException {
			synchronized (this.writeCloseOutputStreamMonitor) {
				ObjectMetadata objectMetadata = new ObjectMetadata();
				objectMetadata.setContentLength(this.currentOutputStream.size());

				SimpleStorageResource.this.amazonS3.putObject(SimpleStorageResource.this.bucketName, SimpleStorageResource.this.objectName,
						new ByteArrayInputStream(this.currentOutputStream.toByteArray()), objectMetadata);
				this.currentOutputStream.close();
			}
		}

		private void closeMultiPartUpload() throws IOException {
			synchronized (this.writeCloseOutputStreamMonitor) {
				Future<Void> future = SimpleStorageResource.this.taskExecutor.submit(uploadLastMultiPart(this.currentOutputStream, this.partNumberCounter.get()));
				addMultiPartUploadFuture(future);

				try {
					waitUntilAllMultiPartsAreUploaded();
					SimpleStorageResource.this.amazonS3.completeMultipartUpload(new CompleteMultipartUploadRequest(this.multiPartUploadResult.getBucketName(),
							this.multiPartUploadResult.getKey(), this.multiPartUploadResult.getUploadId(), this.eTags));
				} catch (ExecutionException e) {
					abortMultiPartUpload();
					throw new IOException("Multi part upload failed ", e);
				} catch (InterruptedException e) {
					abortMultiPartUpload();
					Thread.currentThread().interrupt();
				}
			}
		}

		private void addETags(UploadPartResult uploadPartResult) {
			synchronized (this.eTags) {
				this.eTags.add(uploadPartResult.getPartETag());
			}
		}

		private void verifyThatMultiPartUploadResultIsSet() {
			if (this.multiPartUploadResult == null) {
				synchronized (this.multiPartUploadResultMonitor) {
					if (this.multiPartUploadResult == null) {
						this.multiPartUploadResult = SimpleStorageResource.this.amazonS3.initiateMultipartUpload(
								new InitiateMultipartUploadRequest(SimpleStorageResource.this.bucketName, SimpleStorageResource.this.objectName));
					}
				}
			}
		}

		private void addMultiPartUploadFuture(Future<Void> future) {
			synchronized (this.multiPartUploadFutures) {
				this.multiPartUploadFutures.add(future);
			}
		}

		private void abortMultiPartUpload() {
			SimpleStorageResource.this.amazonS3.abortMultipartUpload(new AbortMultipartUploadRequest(this.multiPartUploadResult.getBucketName(),
					this.multiPartUploadResult.getKey(), this.multiPartUploadResult.getUploadId()));
		}

		private void waitUntilAllMultiPartsAreUploaded() throws ExecutionException, InterruptedException {
			synchronized (this.multiPartUploadFutures) {
				for (Future<Void> multiPartUploadFuture : this.multiPartUploadFutures) {
					multiPartUploadFuture.get();
				}
			}
		}
	}
}