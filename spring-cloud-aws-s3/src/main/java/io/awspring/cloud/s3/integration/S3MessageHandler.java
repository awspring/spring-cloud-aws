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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.Transfer;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * The {@link AbstractReplyProducingMessageHandler} implementation for the Amazon S3 services.
 * <p>
 * The implementation is fully based on the {@link S3TransferManager} and support its {@code upload}, {@code download}
 * and {@code copy} operations which can be determined by the provided or evaluated via SpEL expression at runtime
 * {@link Command}.
 * <p>
 * This {@link AbstractReplyProducingMessageHandler} can behave as a "one-way" (by default) or "request-reply" component
 * according to the {@link #produceReply} constructor argument.
 * <p>
 * The "one-way" behavior is also blocking, which is achieved with the {@link Transfer#completionFuture()} invocation.
 * Consider to use an async upstream hand off if this blocking behavior isn't appropriate.
 * <p>
 * The "request-reply" behavior is async and the {@link Transfer} result from the {@link S3TransferManager} operation is
 * sent to the {@link #getOutputChannel()}, assuming the transfer progress observation in the downstream flow.
 * <p>
 * The {@link TransferListener} can be provided to track the transfer progress. Also, see a {@link Transfer} API
 * returned as a reply message from this handler.
 * <p>
 * For the upload operation the {@link BiConsumer} callback can be supplied to populate options on a
 * {@link PutObjectRequest.Builder} against request message.
 * <p>
 * For download operation the {@code payload} must be a {@link File} instance, representing a single file for downloaded
 * content or directory to download all files from the S3 virtual directory.
 * <p>
 * An S3 Object {@code key} for upload and download can be determined by the provided {@link #keyExpression} or the
 * {@link File#getName()} is used directly. The former has precedence.
 * <p>
 * For copy operation all {@link #keyExpression}, {@link #destinationBucketExpression} and
 * {@link #destinationKeyExpression} are required and must not evaluate to {@code null}.
 * <p>
 *
 * @author Artem Bilan
 * @author John Logan
 *
 * @since 4.0
 *
 * @see S3TransferManager
 */
public class S3MessageHandler extends AbstractReplyProducingMessageHandler {

	private final S3TransferManager transferManager;

	private final boolean produceReply;

	private final Expression bucketExpression;

	private EvaluationContext evaluationContext;

	private Expression keyExpression;

	private Expression destinationBucketExpression;

	private Expression destinationKeyExpression;

	private Expression commandExpression = new ValueExpression<>(Command.UPLOAD);

	private BiConsumer<PutObjectRequest.Builder, Message<?>> uploadMetadataProvider = (builder, message) -> {
	};

	private TransferListener transferListener;

	public S3MessageHandler(S3AsyncClient amazonS3, String bucket) {
		this(amazonS3, bucket, false);
	}

	public S3MessageHandler(S3AsyncClient amazonS3, Expression bucketExpression) {
		this(amazonS3, bucketExpression, false);
	}

	public S3MessageHandler(S3AsyncClient amazonS3, String bucket, boolean produceReply) {
		this(amazonS3, new LiteralExpression(bucket), produceReply);
		Assert.notNull(bucket, "'bucket' must not be null");
	}

	public S3MessageHandler(S3AsyncClient amazonS3, Expression bucketExpression, boolean produceReply) {
		this(S3TransferManager.builder().s3Client(amazonS3).build(), bucketExpression, produceReply);
		Assert.notNull(amazonS3, "'amazonS3' must not be null");
	}

	public S3MessageHandler(S3TransferManager transferManager, String bucket) {
		this(transferManager, bucket, false);
	}

	public S3MessageHandler(S3TransferManager transferManager, Expression bucketExpression) {
		this(transferManager, bucketExpression, false);
	}

	public S3MessageHandler(S3TransferManager transferManager, String bucket, boolean produceReply) {
		this(transferManager, new LiteralExpression(bucket), produceReply);
		Assert.notNull(bucket, "'bucket' must not be null");
	}

	public S3MessageHandler(S3TransferManager transferManager, Expression bucketExpression, boolean produceReply) {
		Assert.notNull(transferManager, "'transferManager' must not be null");
		Assert.notNull(bucketExpression, "'bucketExpression' must not be null");
		this.transferManager = transferManager;
		this.bucketExpression = bucketExpression;
		this.produceReply = produceReply;
	}

	/**
	 * The SpEL expression to evaluate S3 object key at runtime against {@code requestMessage}.
	 * @param keyExpression the SpEL expression for S3 key.
	 */
	public void setKeyExpression(Expression keyExpression) {
		this.keyExpression = keyExpression;
	}

	/**
	 * Specify a {@link Command} to perform against {@link S3TransferManager}.
	 * @param command The {@link Command} to use.
	 * @see Command
	 */
	public void setCommand(Command command) {
		Assert.notNull(command, "'command' must not be null");
		setCommandExpression(new ValueExpression<>(command));
	}

	/**
	 * The SpEL expression to evaluate the command to perform on {@link S3TransferManager}: {@code upload},
	 * {@code download} or {@code copy}.
	 * @param commandExpression the SpEL expression to evaluate the {@link S3TransferManager} operation.
	 * @see Command
	 */
	public void setCommandExpression(Expression commandExpression) {
		Assert.notNull(commandExpression, "'commandExpression' must not be null");
		this.commandExpression = commandExpression;
	}

	/**
	 * The SpEL expression to evaluate the target S3 bucket for copy operation.
	 * @param destinationBucketExpression the SpEL expression for destination bucket.
	 * @see S3TransferManager#copy(CopyRequest)
	 */
	public void setDestinationBucketExpression(Expression destinationBucketExpression) {
		this.destinationBucketExpression = destinationBucketExpression;
	}

	/**
	 * The SpEL expression to evaluate the target S3 key for copy operation.
	 * @param destinationKeyExpression the SpEL expression for destination key.
	 * @see S3TransferManager#copy(CopyRequest)
	 */
	public void setDestinationKeyExpression(Expression destinationKeyExpression) {
		this.destinationKeyExpression = destinationKeyExpression;
	}

	/**
	 * Specify an {@link BiConsumer} callback to populate the metadata for upload operation, e.g. {@code Content-MD5},
	 * {@code Content-Type} or any other required options.
	 * @param uploadMetadataProvider the {@link BiConsumer} to use for upload request option settings.
	 */
	public void setUploadMetadataProvider(BiConsumer<PutObjectRequest.Builder, Message<?>> uploadMetadataProvider) {
		Assert.notNull(uploadMetadataProvider, "'uploadMetadataProvider' must not be null");
		this.uploadMetadataProvider = uploadMetadataProvider;
	}

	public void setTransferListener(TransferListener transferListener) {
		this.transferListener = transferListener;
	}

	@Override
	public String getComponentType() {
		return "aws:s3-outbound-channel-adapter";
	}

	@Override
	protected void doInit() {
		Assert.notNull(this.bucketExpression, "The 'bucketExpression' must not be null");
		super.doInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Command command = this.commandExpression.getValue(this.evaluationContext, requestMessage, Command.class);
		Assert.state(command != null, () -> "'commandExpression' [" + this.commandExpression.getExpressionString()
				+ "] cannot evaluate to null.");

		Transfer transfer = switch (command) {
		case UPLOAD -> upload(requestMessage);
		case DOWNLOAD -> download(requestMessage);
		case COPY -> copy(requestMessage);
		};

		if (this.produceReply) {
			return transfer;
		}
		else {
			try {
				transfer.completionFuture().join();
			}
			catch (CompletionException ex) {
				throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(requestMessage,
						() -> "Failed to transfer file", ex.getCause());
			}
			return null;
		}
	}

	private Transfer upload(Message<?> requestMessage) {
		Object payload = requestMessage.getPayload();
		String bucketName = obtainBucket(requestMessage);

		String key = null;
		if (this.keyExpression != null) {
			key = this.keyExpression.getValue(this.evaluationContext, requestMessage, String.class);
		}
		else if (payload instanceof File fileToUpload) {
			key = fileToUpload.getName();
		}

		if (payload instanceof File fileToUpload && fileToUpload.isDirectory()) {
			UploadDirectoryRequest.Builder uploadDirectoryRequest = UploadDirectoryRequest.builder().bucket(bucketName)
					.source(fileToUpload.toPath()).s3Prefix(key);

			if (this.transferListener != null) {
				uploadDirectoryRequest.uploadFileRequestTransformer(
						(fileUpload) -> fileUpload.addTransferListener(this.transferListener));
			}

			return this.transferManager.uploadDirectory(uploadDirectoryRequest.build());
		}
		else {
			PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
					.applyMutation((builder) -> this.uploadMetadataProvider.accept(builder, requestMessage))
					.bucket(bucketName).key(key);

			PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

			AsyncRequestBody requestBody;
			try {
				if (payload instanceof InputStream inputStream) {
					byte[] body = IoUtils.toByteArray(inputStream);
					if (putObjectRequest.contentMD5() == null) {
						putObjectRequestBuilder.contentMD5(Md5Utils.md5AsBase64(body));
						inputStream.reset();
					}
					requestBody = AsyncRequestBody.fromBytes(body);
				}
				else if (payload instanceof File fileToUpload) {
					if (putObjectRequest.contentMD5() == null) {
						putObjectRequestBuilder.contentMD5(Md5Utils.md5AsBase64(fileToUpload));
					}
					if (putObjectRequest.contentLength() == null) {
						putObjectRequestBuilder.contentLength(fileToUpload.length());
					}
					if (putObjectRequest.contentType() == null) {
						putObjectRequestBuilder.contentType(Mimetype.getInstance().getMimetype(fileToUpload));
					}
					requestBody = AsyncRequestBody.fromFile(fileToUpload);
				}
				else if (payload instanceof byte[] payloadBytes) {
					if (putObjectRequest.contentMD5() == null) {
						putObjectRequestBuilder.contentMD5(Md5Utils.md5AsBase64(payloadBytes));
					}
					if (putObjectRequest.contentLength() == null) {
						putObjectRequestBuilder.contentLength((long) payloadBytes.length);
					}
					requestBody = AsyncRequestBody.fromBytes(payloadBytes);
				}
				else {
					throw new IllegalArgumentException("Unsupported payload type: [" + payload.getClass()
							+ "]. The only supported payloads for the upload request are "
							+ "java.io.File, java.io.InputStream, byte[] and PutObjectRequest.");
				}
			}
			catch (IOException e) {
				throw new MessageHandlingException(requestMessage, e);
			}

			if (key == null) {
				if (this.keyExpression != null) {
					throw new IllegalStateException("The 'keyExpression' [" + this.keyExpression.getExpressionString()
							+ "] must not evaluate to null. Root object is: " + requestMessage);
				}
				else {
					throw new IllegalStateException("Specify a 'keyExpression' for non-java.io.File payloads");
				}
			}

			UploadRequest.Builder uploadRequest = UploadRequest.builder()
					.putObjectRequest(putObjectRequestBuilder.build()).requestBody(requestBody);

			if (transferListener != null) {
				uploadRequest.addTransferListener(transferListener);
			}

			return this.transferManager.upload(uploadRequest.build());
		}
	}

	private Transfer download(Message<?> requestMessage) {
		Object payload = requestMessage.getPayload();
		Assert.state(payload instanceof File, () -> "For the 'DOWNLOAD' operation the 'payload' must be of "
				+ "'java.io.File' type, but gotten: [" + payload.getClass() + ']');

		File targetFile = (File) payload;

		String bucket = obtainBucket(requestMessage);

		String key = this.keyExpression != null
				? this.keyExpression.getValue(this.evaluationContext, requestMessage, String.class)
				: null;

		if (targetFile.isDirectory()) {
			DownloadDirectoryRequest.Builder downloadDirectoryRequest = DownloadDirectoryRequest.builder()
					.bucket(bucket).destination(targetFile.toPath())
					.listObjectsV2RequestTransformer(filter -> filter.prefix(key));
			if (this.transferListener != null) {
				downloadDirectoryRequest.downloadFileRequestTransformer(
						(fileDownload) -> fileDownload.addTransferListener(this.transferListener));
			}
			return this.transferManager.downloadDirectory(downloadDirectoryRequest.build());
		}
		else {
			DownloadFileRequest.Builder downloadFileRequest = DownloadFileRequest.builder().destination(targetFile)
					.getObjectRequest(request -> request.bucket(bucket).key(key != null ? key : targetFile.getName()));
			if (this.transferListener != null) {
				downloadFileRequest.addTransferListener(this.transferListener);
			}
			return this.transferManager.downloadFile(downloadFileRequest.build());
		}
	}

	private Transfer copy(Message<?> requestMessage) {
		String sourceBucketName = obtainBucket(requestMessage);

		String sourceKey = null;
		if (this.keyExpression != null) {
			sourceKey = this.keyExpression.getValue(this.evaluationContext, requestMessage, String.class);
		}

		Assert.state(sourceKey != null, () -> "The 'keyExpression' must not be null for 'copy' operation "
				+ "and 'keyExpression' can't evaluate to null. " + "Root object is: " + requestMessage);

		String destinationBucketName = null;
		if (this.destinationBucketExpression != null) {
			destinationBucketName = this.destinationBucketExpression.getValue(this.evaluationContext, requestMessage,
					String.class);
		}

		Assert.state(destinationBucketName != null,
				() -> "The 'destinationBucketExpression' must not be null for 'copy' operation "
						+ "and can't evaluate to null. Root object is: " + requestMessage);

		String destinationKey = null;
		if (this.destinationKeyExpression != null) {
			destinationKey = this.destinationKeyExpression.getValue(this.evaluationContext, requestMessage,
					String.class);
		}

		Assert.state(destinationKey != null,
				() -> "The 'destinationKeyExpression' must not be null for 'copy' operation "
						+ "and can't evaluate to null. Root object is: " + requestMessage);

		CopyObjectRequest.Builder copyObjectRequest = CopyObjectRequest.builder().sourceBucket(sourceBucketName)
				.sourceKey(sourceKey).destinationBucket(destinationBucketName).destinationKey(destinationKey);

		CopyRequest.Builder copyRequest = CopyRequest.builder().copyObjectRequest(copyObjectRequest.build());
		if (this.transferListener != null) {
			copyRequest.addTransferListener(this.transferListener);
		}
		return this.transferManager.copy(copyRequest.build());
	}

	private String obtainBucket(Message<?> requestMessage) {
		String bucketName;
		if (this.bucketExpression instanceof LiteralExpression) {
			bucketName = (String) this.bucketExpression.getValue();
		}
		else {
			bucketName = this.bucketExpression.getValue(this.evaluationContext, requestMessage, String.class);
		}
		Assert.state(bucketName != null, () -> "The 'bucketExpression' [" + this.bucketExpression.getExpressionString()
				+ "] must not evaluate to null. Root object is: " + requestMessage);

		return bucketName;
	}

	/**
	 * The {@link S3MessageHandler} mode.
	 *
	 * @see #setCommand
	 */
	public enum Command {

		/**
		 * The command to perform {@link S3TransferManager#upload} operation.
		 */
		UPLOAD,

		/**
		 * The command to perform {@link S3TransferManager#download} operation.
		 */
		DOWNLOAD,

		/**
		 * The command to perform {@link S3TransferManager#copy} operation.
		 */
		COPY

	}

}
