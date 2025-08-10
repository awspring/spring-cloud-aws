package io.awspring.cloud.sqs.integration;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ListenerMode;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SqsMessageBatchFilterIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(SqsMessageBatchFilterIntegrationTests.class);

	private static final String FILTER_QUEUE_BATCH = "filter-queue-batch";
	private static final String BATCH_FACTORY      = "batchFilteringFactory";

	@Autowired SqsTemplate sqsTemplate;
	@Autowired LatchContainer latch;

	record SampleRecord(String propertyOne, String propertyTwo) {}

	@BeforeAll
	static void setupQueues() {
		SqsAsyncClient client = createAsyncClient();
		createQueue(client, FILTER_QUEUE_BATCH).join();
	}

	@Test
	void shouldDeliverAllowedInBatch() throws Exception {
		sqsTemplate.send(FILTER_QUEUE_BATCH, new SampleRecord("Hello",    "world1"));
		sqsTemplate.send(FILTER_QUEUE_BATCH, new SampleRecord("NotHello", "world2"));
		sqsTemplate.send(FILTER_QUEUE_BATCH, new SampleRecord("Hello",    "world3"));

		assertThat(latch.pass.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(latch.received).containsExactly("world1", "world3");
	}

	// ==== Config
	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class Config {
		@Bean(name = BATCH_FACTORY)
		SqsMessageListenerContainerFactory<SampleRecord> batchFactory() {
			return SqsMessageListenerContainerFactory.<SampleRecord>builder()
													 .configure(o -> {
														 o.messageFilter(new AllowHelloOnlyFilter());
														 o.listenerMode(ListenerMode.BATCH);
														 o.maxMessagesPerPoll(10);
													 })
													 .sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
													 .build();
		}

		@Bean SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
		}

		@Bean FilteringBatchListener batchListener() { return new FilteringBatchListener(); }
		@Bean LatchContainer latch() { return new LatchContainer(); }
	}

	static class AllowHelloOnlyFilter implements MessageFilter<SampleRecord> {
		@Override public Collection<Message<SampleRecord>> process(Collection<Message<SampleRecord>> msgs) {
			return msgs.stream()
					   .filter(m -> {
						   SampleRecord p = m.getPayload();
						   log.info("Filtering message: {}", p);
						   return "Hello".equals(p.propertyOne());
					   })
					   .collect(Collectors.toList());
		}
	}

	static class FilteringBatchListener {
		@Autowired LatchContainer latch;

		@SqsListener(queueNames = FILTER_QUEUE_BATCH, id = "filter-batch", factory = BATCH_FACTORY)
		void listen(List<SampleRecord> batch) {
			log.info("Received batch size={}", batch.size());
			latch.batchSizes.add(batch.size());

			for (SampleRecord r : batch) {
				if ("Hello".equals(r.propertyOne())) {
					latch.received.add(r.propertyTwo());
					latch.pass.countDown();
				} else {
					latch.block.countDown();
				}
			}
		}
	}

	// ==== Latch
	static class LatchContainer {
		final CountDownLatch pass  = new CountDownLatch(2);
		final CountDownLatch block = new CountDownLatch(1);
		
		final List<String>  received   = new CopyOnWriteArrayList<>();
		final List<Integer> batchSizes = new CopyOnWriteArrayList<>();
	}
}
