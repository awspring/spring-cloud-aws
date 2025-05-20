package io.awspring.cloud.sqs.integration;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.filter.MessageFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class SqsMessageFilterIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageFilterIntegrationTests.class);
	private static final String FILTER_QUEUE_NAME = "test-filter-queue";

	static final String MESSAGE_FILTERED_FACTORY = "messageFilteredFactory";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(
			createQueue(client, FILTER_QUEUE_NAME)
		).join();
	}

	record SampleRecord(String propertyOne, String propertyTwo) {}

	@Test
	void shouldReceiveMessageThatPassesProcess() throws Exception {
		sqsTemplate.send(FILTER_QUEUE_NAME, new SampleRecord("Hello!", "Filtered In"));
		assertThat(latchContainer.messageReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	// --- Configuration for test environment ---
	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class FilterTestConfig {

		@Bean(name = MESSAGE_FILTERED_FACTORY)
		public SqsMessageListenerContainerFactory<SampleRecord> filteredFactory() {
			return SqsMessageListenerContainerFactory.<SampleRecord>builder()
													 .configure(options -> options.messageFilter(new SampleRecordFilter()))
													 .sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
													 .build();
		}

		@Bean
		public SqsTemplate sqsTemplate() {
			return SqsTemplate.builder()
							  .sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient())
							  .build();
		}

		@Bean
		public FilteringListener filteringListener() {
			return new FilteringListener();
		}

		@Bean
		public LatchContainer latchContainer() {
			return new LatchContainer();
		}
	}

	static class SampleRecordFilter implements MessageFilter<SampleRecord> {
		@Override
		public boolean process(Message<SampleRecord> message) {
			logger.info("Filtering message: {}", message.getPayload());
			logger.info(String.valueOf("Hello".equals(message.getPayload().propertyOne())));
			return "Hello".equals(message.getPayload().propertyOne());
		}
	}

	static class FilteringListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FILTER_QUEUE_NAME, id = "filter-test-listener", factory = MESSAGE_FILTERED_FACTORY)
		void listen(SampleRecord record) {
			logger.info("Received message: {}", record);
			latchContainer.messageReceivedLatch.countDown();
		}
	}

	static class LatchContainer {
		CountDownLatch messageReceivedLatch = new CountDownLatch(1);
	}
}
