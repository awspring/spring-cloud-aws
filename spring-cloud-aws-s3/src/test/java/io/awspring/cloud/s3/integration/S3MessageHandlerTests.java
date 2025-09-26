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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.s3.LocalstackContainerTest;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * @author Artem Bilan
 * @author John Logan
 * @author Jim Krygowski
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class S3MessageHandlerTests implements LocalstackContainerTest {

	static S3AsyncClient S3;

	// define the bucket and file names used throughout the test
	static final String S3_BUCKET_NAME = "my-bucket";

	static final String S3_FILE_KEY_BAR = "subdir/bar";

	static final String S3_FILE_KEY_FOO = "subdir/foo";

	static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@TempDir
	static Path temporaryFolder;

	@Autowired
	ContextConfiguration contextConfiguration;

	@Autowired
	MessageChannel s3SendChannel;

	@Autowired
	MessageChannel s3ProcessChannel;

	@Autowired
	PollableChannel s3ReplyChannel;

	@Autowired
	@Qualifier("s3MessageHandler")
	S3MessageHandler s3MessageHandler;

	@BeforeAll
	static void setup() {
		S3 = LocalstackContainerTest.s3AsyncClient();
		S3.createBucket(request -> request.bucket(S3_BUCKET_NAME)).join();
	}

	@BeforeEach
	void prepareBucket() {
		S3.listObjects(request -> request.bucket(S3_BUCKET_NAME)).thenCompose(result -> {
			if (result.hasContents()) {
				return S3.deleteObjects(request -> request.bucket(S3_BUCKET_NAME)
						.delete(delete -> delete.objects(result.contents().stream().map(S3Object::key)
								.map(key -> ObjectIdentifier.builder().key(key).build()).toList())));
			}
			else {
				return CompletableFuture.completedFuture(null);
			}
		}).join();

		this.contextConfiguration.transferCompletedLatch = new CountDownLatch(1);
	}

	@Test
	void uploadFile() throws IOException, InterruptedException {
		File file = new File(temporaryFolder.toFile(), "foo.mp3");
		file.createNewFile();
		byte[] testData = "test data".getBytes();
		FileCopyUtils.copy(testData, file);
		Message<?> message = MessageBuilder.withPayload(file)
				.setHeader("s3Command", S3MessageHandler.Command.UPLOAD.name()).build();

		this.s3SendChannel.send(message);
		assertThat(this.contextConfiguration.transferCompletedLatch.await(10, TimeUnit.SECONDS)).isTrue();

		File outputFile = new File(temporaryFolder.toFile(), "outputFile1");

		GetObjectResponse getObjectResponse = S3
				.getObject(request -> request.bucket(S3_BUCKET_NAME).key("foo.mp3"), outputFile.toPath()).join();

		assertThat(getObjectResponse.contentLength()).isEqualTo(testData.length);
		assertThat(getObjectResponse.contentType()).isEqualTo("audio/mpeg");

		assertThat(FileCopyUtils.copyToByteArray(outputFile)).isEqualTo(testData);
	}

	@Test
	void uploadInputStream() throws IOException, InterruptedException {
		Expression actualKeyExpression = TestUtils.getPropertyValue(this.s3MessageHandler, "keyExpression",
				Expression.class);

		this.s3MessageHandler.setKeyExpression(null);

		String testData = "a";

		InputStream payload = new StringInputStream(testData);
		Message<?> message = MessageBuilder.withPayload(payload)
				.setHeader("s3Command", S3MessageHandler.Command.UPLOAD.name()).setHeader("key", "myStream").build();

		assertThatThrownBy(() -> this.s3SendChannel.send(message))
				.hasCauseExactlyInstanceOf(IllegalStateException.class)
				.hasStackTraceContaining("Specify a 'keyExpression' for non-java.io.File payloads");

		this.s3MessageHandler.setKeyExpression(actualKeyExpression);

		this.s3SendChannel.send(message);

		assertThat(this.contextConfiguration.transferCompletedLatch.await(10, TimeUnit.SECONDS)).isTrue();

		File outputFile = new File(temporaryFolder.toFile(), "outputFile2");

		GetObjectResponse getObjectResponse = S3
				.getObject(request -> request.bucket(S3_BUCKET_NAME).key("myStream"), outputFile.toPath()).join();

		assertThat(getObjectResponse.contentLength()).isEqualTo(testData.length());
		assertThat(getObjectResponse.contentType()).isEqualTo("application/json");
		assertThat(getObjectResponse.contentDisposition()).isEqualTo("test.json");

		assertThat(FileCopyUtils.copyToByteArray(outputFile)).isEqualTo(testData.getBytes());
	}

	@Test
	void uploadByteArray() throws InterruptedException, IOException {
		byte[] payload = "b".getBytes(StandardCharsets.UTF_8);
		Message<?> message = MessageBuilder.withPayload(payload)
				.setHeader("s3Command", S3MessageHandler.Command.UPLOAD.name()).setHeader("key", "myStream").build();

		this.s3SendChannel.send(message);

		assertThat(this.contextConfiguration.transferCompletedLatch.await(10, TimeUnit.SECONDS)).isTrue();

		File outputFile = new File(temporaryFolder.toFile(), "outputFile3");

		GetObjectResponse getObjectResponse = S3
				.getObject(request -> request.bucket(S3_BUCKET_NAME).key("myStream"), outputFile.toPath()).join();

		assertThat(getObjectResponse.contentLength()).isEqualTo(payload.length);
		assertThat(getObjectResponse.contentType()).isEqualTo("application/json");
		assertThat(getObjectResponse.contentDisposition()).isEqualTo("test.json");

		assertThat(FileCopyUtils.copyToByteArray(outputFile)).isEqualTo(payload);
	}

	@Test
	void downloadDirectory() throws IOException {
		CompletableFuture<PutObjectResponse> bb = S3.putObject(
				request -> request.bucket(S3_BUCKET_NAME).key(S3_FILE_KEY_BAR), AsyncRequestBody.fromString("bb"));
		CompletableFuture<PutObjectResponse> f = S3.putObject(
				request -> request.bucket(S3_BUCKET_NAME).key(S3_FILE_KEY_FOO), AsyncRequestBody.fromString("f"));

		CompletableFuture.allOf(bb, f).join();

		File directoryForDownload = new File(temporaryFolder.toFile(), "myFolder");
		directoryForDownload.mkdir();
		Message<?> message = MessageBuilder.withPayload(directoryForDownload)
				.setHeader("s3Command", S3MessageHandler.Command.DOWNLOAD).build();

		this.s3SendChannel.send(message);

		// get the "root" directory
		File[] directoryArray = directoryForDownload.listFiles();
		assertThat(directoryArray).isNotNull();
		assertThat(directoryArray.length).isEqualTo(1);

		File subDirectory = directoryArray[0];
		assertThat(subDirectory).hasName("subdir");

		// get the files we downloaded
		File[] fileArray = subDirectory.listFiles();
		assertThat(fileArray).isNotNull();
		assertThat(fileArray.length).isEqualTo(2);

		List<File> files = Arrays.asList(fileArray);
		files.sort(Comparator.comparing(File::getName));

		File file1 = files.get(0);
		assertThat(file1).hasName(S3_FILE_KEY_BAR.split("/", 2)[1]);
		assertThat(FileCopyUtils.copyToString(new FileReader(file1))).isEqualTo("bb");

		File file2 = files.get(1);
		assertThat(file2).hasName(S3_FILE_KEY_FOO.split("/", 2)[1]);
		assertThat(FileCopyUtils.copyToString(new FileReader(file2))).isEqualTo("f");
	}

	@Test
	void copy() throws IOException {
		byte[] testData = "ff".getBytes();
		CompletableFuture<PutObjectResponse> mySource = S3.putObject(
				request -> request.bucket(S3_BUCKET_NAME).key("mySource"), AsyncRequestBody.fromBytes(testData));
		CompletableFuture<CreateBucketResponse> theirBucket = S3
				.createBucket(request -> request.bucket("their-bucket"));

		CompletableFuture.allOf(mySource, theirBucket).join();
		Map<String, String> payload = new HashMap<>();
		payload.put("key", "mySource");
		payload.put("destination", "their-bucket");
		payload.put("destinationKey", "theirTarget");
		this.s3ProcessChannel.send(new GenericMessage<>(payload));

		Message<?> receive = this.s3ReplyChannel.receive(10000);
		assertThat(receive).isNotNull();

		assertThat(receive.getPayload()).isInstanceOf(Copy.class);
		Copy copy = (Copy) receive.getPayload();

		copy.completionFuture().join();

		File outputFile = new File(temporaryFolder.toFile(), "outputFile4");

		GetObjectResponse getObjectResponse = S3
				.getObject(request -> request.bucket("their-bucket").key("theirTarget"), outputFile.toPath()).join();

		assertThat(getObjectResponse.contentLength()).isEqualTo(testData.length);

		assertThat(FileCopyUtils.copyToByteArray(outputFile)).isEqualTo(testData);
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		volatile CountDownLatch transferCompletedLatch;

		@Bean
		@ServiceActivator(inputChannel = "s3SendChannel")
		public MessageHandler s3MessageHandler() {
			S3MessageHandler s3MessageHandler = new S3MessageHandler(S3, S3_BUCKET_NAME);
			s3MessageHandler.setCommandExpression(PARSER.parseExpression("headers.s3Command"));
			Expression keyExpression = PARSER.parseExpression(
					"payload instanceof T(java.io.File) and !payload.directory ? payload.name : headers[key]");
			s3MessageHandler.setKeyExpression(keyExpression);
			s3MessageHandler.setUploadMetadataProvider((metadata, message) -> {
				if (message.getPayload() instanceof InputStream || message.getPayload() instanceof byte[]) {
					metadata.contentLength(1L).contentType("application/json").contentDisposition("test.json")
							.acl(ObjectCannedACL.PUBLIC_READ_WRITE);
				}
			});
			s3MessageHandler.setTransferListener(new TransferListener() {

				@Override
				public void transferComplete(Context.TransferComplete context) {
					transferCompletedLatch.countDown();
				}

			});
			return s3MessageHandler;
		}

		@Bean
		public PollableChannel s3ReplyChannel() {
			return new QueueChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "s3ProcessChannel")
		public MessageHandler s3ProcessMessageHandler() {
			S3MessageHandler s3MessageHandler = new S3MessageHandler(S3, S3_BUCKET_NAME, true);
			s3MessageHandler.setOutputChannel(s3ReplyChannel());
			s3MessageHandler.setCommand(S3MessageHandler.Command.COPY);
			s3MessageHandler.setKeyExpression(PARSER.parseExpression("payload.key"));
			s3MessageHandler.setDestinationBucketExpression(PARSER.parseExpression("payload.destination"));
			s3MessageHandler.setDestinationKeyExpression(PARSER.parseExpression("payload.destinationKey"));
			return s3MessageHandler;
		}

	}

}
