package io.awspring.cloud.sqs.integration;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.filter.MessageFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SqsMessageFifoFilterIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(SqsMessageFifoFilterIntegrationTests.class);

	private static final String FILTER_QUEUE_FIFO = "filter-queue-test.fifo";
	private static final String FIFO_FACTORY      = "fifoFilteringFactory";

	@Autowired SqsTemplate sqsTemplate;
	@Autowired LatchContainer latch;
	@Autowired ApplicationContext applicationContext;

	record SampleRecord(String propertyOne, String propertyTwo) {}

	@BeforeAll
	static void setupQueues() {
		SqsAsyncClient client = createAsyncClient();
		createFifoQueue(client, FILTER_QUEUE_FIFO).join();
	}

	@Test
	void shouldPreserveOrderForAllowedInFifo() throws Exception {
		var m1 = MessageBuilder.withPayload(new SampleRecord("Hello", "A1"))
							   .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, "g1")
							   .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString())
							   .build();

		var m2 = MessageBuilder.withPayload(new SampleRecord("NotHello", "A2"))
							   .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, "g1")
							   .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString())
							   .build();

		var m3 = MessageBuilder.withPayload(new SampleRecord("Hello", "A3"))
							   .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, "g1")
							   .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString())
							   .build();

		sqsTemplate.send(FILTER_QUEUE_FIFO, m1);
		sqsTemplate.send(FILTER_QUEUE_FIFO, m2);
		sqsTemplate.send(FILTER_QUEUE_FIFO, m3);

		// 통과 메시지 2건(A1, A3) 수신 대기
		assertThat(latch.pass.await(30, TimeUnit.SECONDS)).isTrue();

		var ordered = applicationContext.getBean(FilteringOrderedListener.class);
		assertThat(ordered.receivedOrder).containsExactly("A1", "A3");
	}

	// ==== Config
	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class Config {
		@Bean(name = FIFO_FACTORY)
		SqsMessageListenerContainerFactory<SampleRecord> fifoFactory() {
			return SqsMessageListenerContainerFactory.<SampleRecord>builder()
													 .configure(o -> {
														 o.messageFilter(new AllowHelloOnlyFilter());
														 o.maxMessagesPerPoll(10); // 선택
													 })
													 .sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
													 .build();
		}

		@Bean SqsTemplate sqsTemplate() {
			return SqsTemplate.builder()
							  .sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient())
							  .build();
		}

		@Bean FilteringOrderedListener orderedListener() { return new FilteringOrderedListener(); }
		@Bean LatchContainer latch() { return new LatchContainer(); }
	}

	// ==== Filter
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

	// ==== Listener
	static class FilteringOrderedListener {
		@Autowired LatchContainer latch;
		final List<String> receivedOrder = new CopyOnWriteArrayList<>();

		@SqsListener(queueNames = FILTER_QUEUE_FIFO, id = "filter-fifo", factory = FIFO_FACTORY)
		void listen(SampleRecord r) {
			log.info("Received(fifo): {}", r);
			receivedOrder.add(r.propertyTwo());
			latch.pass.countDown();
		}
	}

	// ==== Latch
	static class LatchContainer {
		final CountDownLatch pass  = new CountDownLatch(2); // A1, A3 두 건
		final CountDownLatch block = new CountDownLatch(1);
	}
}
