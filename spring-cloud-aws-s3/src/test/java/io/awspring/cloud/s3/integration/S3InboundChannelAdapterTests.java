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

import io.awspring.cloud.s3.LocalstackContainerTest;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Artem Bilan
 * @author Jim Krygowski
 * @author Xavier François
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class S3InboundChannelAdapterTests implements LocalstackContainerTest {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private static final String S3_BUCKET = "s3-bucket";

	private static S3Client S3;

	@TempDir
	static Path TEMPORARY_FOLDER;

	private static File LOCAL_FOLDER;

	@Autowired
	private PollableChannel s3FilesChannel;

	@BeforeAll
	static void setup() {
		S3 = LocalstackContainerTest.s3Client();
		S3.createBucket(request -> request.bucket(S3_BUCKET));
		S3.putObject(request -> request.bucket(S3_BUCKET).key("subdir/a.test"), RequestBody.fromString("Hello"));
		S3.putObject(request -> request.bucket(S3_BUCKET).key("subdir/b.test"), RequestBody.fromString("Bye"));

		LOCAL_FOLDER = TEMPORARY_FOLDER.resolve("local").toFile();
	}

	@Test
	void s3InboundChannelAdapter() throws IOException {
		Message<?> message = this.s3FilesChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(File.class);
		File localFile = (File) message.getPayload();
		assertThat(localFile).hasName("A.TEST.a");

		String content = FileCopyUtils.copyToString(new FileReader(localFile));
		assertThat(content).isEqualTo("Hello");

		message = this.s3FilesChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(File.class);
		localFile = (File) message.getPayload();
		assertThat(localFile).hasName("B.TEST.a");

		content = FileCopyUtils.copyToString(new FileReader(localFile));
		assertThat(content).isEqualTo("Bye");

		assertThat(message.getHeaders()).containsKeys(FileHeaders.REMOTE_DIRECTORY, FileHeaders.REMOTE_HOST_PORT,
				FileHeaders.REMOTE_FILE);
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public S3SessionFactory s3SessionFactory() {
			S3SessionFactory s3SessionFactory = new S3SessionFactory(S3);
			s3SessionFactory.setEndpoint("s3-url.com:8000");
			return s3SessionFactory;
		}

		@Bean
		public S3InboundFileSynchronizer s3InboundFileSynchronizer() {
			S3InboundFileSynchronizer synchronizer = new S3InboundFileSynchronizer(s3SessionFactory());
			synchronizer.setDeleteRemoteFiles(true);
			synchronizer.setPreserveTimestamp(true);
			synchronizer.setRemoteDirectory(S3_BUCKET);
			synchronizer.setFilter(new S3RegexPatternFileListFilter(".*\\.test$"));
			Expression expression = PARSER.parseExpression(
					"(#this.contains('/') ? #this.substring(#this.lastIndexOf('/') + 1) : #this).toUpperCase() + '.a'");
			synchronizer.setLocalFilenameGeneratorExpression(expression);
			return synchronizer;
		}

		@Bean
		@InboundChannelAdapter(value = "s3FilesChannel", poller = @Poller(fixedDelay = "100"))
		public S3InboundFileSynchronizingMessageSource s3InboundFileSynchronizingMessageSource() {
			S3InboundFileSynchronizingMessageSource messageSource = new S3InboundFileSynchronizingMessageSource(
					s3InboundFileSynchronizer());
			messageSource.setAutoCreateLocalDirectory(true);
			messageSource.setLocalDirectory(LOCAL_FOLDER);
			messageSource.setLocalFilter(new AcceptOnceFileListFilter<>());
			return messageSource;
		}

		@Bean
		public PollableChannel s3FilesChannel() {
			return new QueueChannel();
		}

	}

}
