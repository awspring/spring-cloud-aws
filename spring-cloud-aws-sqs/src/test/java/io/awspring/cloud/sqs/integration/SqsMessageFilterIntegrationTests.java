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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class SqsMessageFilterIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageFilterIntegrationTests.class);
	private static final String FILTER_QUEUE_PASS = "filter-queue-pass";
	private static final String FILTER_QUEUE_BLOCK = "filter-queue-block";

	private static final String FILTERING_FACTORY = "filteringFactory";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@BeforeAll
	static void setupQueues() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(
			createQueue(client, FILTER_QUEUE_PASS),
			createQueue(client, FILTER_QUEUE_BLOCK)
		).join();
	}

	record SampleRecord(String propertyOne, String propertyTwo) {}

	@Test
	void shouldReceiveMessageThatPassesProcess() throws Exception {
		sqsTemplate.send(FILTER_QUEUE_PASS, new SampleRecord("Hello", "Accepted"));
		assertThat(latchContainer.latchForPass.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void shouldNotReceiveMessageThatFailsProcess() throws Exception {
		sqsTemplate.send(FILTER_QUEUE_BLOCK, new SampleRecord("NotHello", "Rejected"));
		assertThat(latchContainer.latchForBlock.await(10, TimeUnit.SECONDS)).isFalse();
	}

	// Configuration
	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class FilterTestConfig {

		@Bean(name = FILTERING_FACTORY)
		public SqsMessageListenerContainerFactory<SampleRecord> messageFilterFactory() {
			return SqsMessageListenerContainerFactory.<SampleRecord>builder()
													 .configure(options -> options.messageFilter(new AllowHelloOnlyFilter()))
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
		public FilteringListenerPass filteringListenerPass() {
			return new FilteringListenerPass();
		}

		@Bean
		public FilteringListenerBlock filteringListenerBlock() {
			return new FilteringListenerBlock();
		}

		@Bean
		public LatchContainer latchContainer() {
			return new LatchContainer();
		}
	}

	// Sample Filter
	static class AllowHelloOnlyFilter implements MessageFilter<SampleRecord> {
		@Override
		public Collection<Message<SampleRecord>> process(Collection<Message<SampleRecord>> messages) {
			return messages.stream()
						   .filter(msg -> {
							   SampleRecord p = msg.getPayload();
							   logger.info("Filtering message: {}", p);
							   return "Hello".equals(p.propertyOne());
						   })
						   .collect(Collectors.toList());
		}
	}

	// Listener for PASS case
	static class FilteringListenerPass {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FILTER_QUEUE_PASS, id = "filter-pass", factory = FILTERING_FACTORY)
		void listen(SampleRecord record) {
			logger.info("Received (pass): {}", record);
			latchContainer.latchForPass.countDown();
		}
	}

	// Listener for BLOCK case
	static class FilteringListenerBlock {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FILTER_QUEUE_BLOCK, id = "filter-block", factory = FILTERING_FACTORY)
		void listen(SampleRecord record) {
			logger.info("Received (block): {}", record);
			latchContainer.latchForBlock.countDown();
		}
	}

	// Shared latch
	static class LatchContainer {
		final CountDownLatch latchForPass = new CountDownLatch(1);
		final CountDownLatch latchForBlock = new CountDownLatch(1);
	}
}
