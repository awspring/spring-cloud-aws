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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Comparator;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class S3StreamingChannelAdapterTests implements LocalstackContainerTest {

	private static final String S3_BUCKET = "s3-bucket";

	private static S3Client S3;

	@Autowired
	private PollableChannel s3FilesChannel;

	@BeforeAll
	static void setup() {
		S3 = LocalstackContainerTest.s3Client();
		S3.createBucket(request -> request.bucket(S3_BUCKET));
		S3.putObject(request -> request.bucket(S3_BUCKET).key("subdir/a.test"), RequestBody.fromString("Hello"));
		S3.putObject(request -> request.bucket(S3_BUCKET).key("subdir/b.test"), RequestBody.fromString("Bye"));
	}

	@Test
	void s3InboundStreamingChannelAdapter() throws IOException {
		Message<?> message = this.s3FilesChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("subdir/a.test");

		InputStream inputStreamA = (InputStream) message.getPayload();
		assertThat(inputStreamA).isNotNull();
		assertThat(IOUtils.toString(inputStreamA, Charset.defaultCharset())).isEqualTo("Hello");
		inputStreamA.close();

		message = this.s3FilesChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("subdir/b.test");
		assertThat(message.getHeaders()).containsKeys(FileHeaders.REMOTE_DIRECTORY, FileHeaders.REMOTE_HOST_PORT,
				FileHeaders.REMOTE_FILE);
		InputStream inputStreamB = (InputStream) message.getPayload();
		assertThat(IOUtils.toString(inputStreamB, Charset.defaultCharset())).isEqualTo("Bye");

		assertThat(this.s3FilesChannel.receive(10)).isNull();

		inputStreamB.close();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		@InboundChannelAdapter(value = "s3FilesChannel", poller = @Poller(fixedDelay = "100"))
		public S3StreamingMessageSource s3InboundStreamingMessageSource() {
			S3SessionFactory s3SessionFactory = new S3SessionFactory(S3);
			S3RemoteFileTemplate s3FileTemplate = new S3RemoteFileTemplate(s3SessionFactory);
			S3StreamingMessageSource s3MessageSource = new S3StreamingMessageSource(s3FileTemplate,
					Comparator.comparing(S3Object::key));
			s3MessageSource.setRemoteDirectory("/" + S3_BUCKET + "/subdir");
			s3MessageSource.setFilter(new S3PersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "streaming"));

			return s3MessageSource;
		}

		@Bean
		public PollableChannel s3FilesChannel() {
			return new QueueChannel();
		}

	}

}
