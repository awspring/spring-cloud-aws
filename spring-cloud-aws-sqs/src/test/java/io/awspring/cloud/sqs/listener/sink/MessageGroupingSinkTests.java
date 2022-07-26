//package io.awspring.cloud.sqs.listener.sink;
//
//import io.awspring.cloud.sqs.listener.MessageProcessingContext;
//import io.awspring.cloud.sqs.listener.sink.adapter.MessageGroupingSinkAdapter;
//import org.junit.jupiter.api.Test;
//import org.springframework.core.task.SimpleAsyncTaskExecutor;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.IntStream;
//
//import static java.util.stream.Collectors.groupingBy;
//import static java.util.stream.Collectors.toList;
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * @author Tomaz Fernandes
// * @since 3.0
// */
//class MessageGroupingSinkTests {
//
//	@Test
//	void maintainsOrderWithinEachGroup() {
//		String header = "header";
//		String firstMessageGroupId = UUID.randomUUID().toString();
//		String secondMessageGroupId = UUID.randomUUID().toString();
//		String thirdMessageGroupId = UUID.randomUUID().toString();
//		List<Message<Integer>> firstMessageGroupMessages = IntStream.range(0, 10)
//			.mapToObj(index -> MessageBuilder.withPayload(index).setHeader(header, firstMessageGroupId).build()).collect(toList());
//		List<Message<Integer>> secondMessageGroupMessages = IntStream.range(0, 10)
//			.mapToObj(index -> MessageBuilder.withPayload(index).setHeader(header, secondMessageGroupId).build()).collect(toList());
//		List<Message<Integer>> thirdMessageGroupMessages = IntStream.range(0, 10)
//			.mapToObj(index -> MessageBuilder.withPayload(index).setHeader(header, thirdMessageGroupId).build()).collect(toList());
//		List<Message<Integer>> messagesToEmit = new ArrayList<>();
//		messagesToEmit.addAll(firstMessageGroupMessages);
//		messagesToEmit.addAll(secondMessageGroupMessages);
//		messagesToEmit.addAll(thirdMessageGroupMessages);
//
//		List<Message<Integer>> received = new ArrayList<>();
//
//		MessageGroupingSinkAdapter<Integer> sinkAdapter = new MessageGroupingSinkAdapter<>(new OrderedMessageListeningSink<>());
//		sinkAdapter.setHeaderName(header);
//		sinkAdapter.setTaskExecutor(new SimpleAsyncTaskExecutor());
//		sinkAdapter.setMessageListener(msg -> {
//			try {
//				Thread.sleep(new Random().nextInt(1000));
//			} catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			}
//			received.add(msg);
//			return CompletableFuture.completedFuture(null);
//		});
//		sinkAdapter.start();
//		sinkAdapter.emit(messagesToEmit, MessageProcessingContext.withCompletionCallback(msg -> {})).join();
//		Map<String, List<Message<Integer>>> receivedMessages = received.stream().collect(groupingBy(message -> (String) message.getHeaders().get(header)));
//
//		assertThat(receivedMessages.get(firstMessageGroupId)).containsSequence(firstMessageGroupMessages);
//		assertThat(receivedMessages.get(secondMessageGroupId)).containsSequence(secondMessageGroupMessages);
//		assertThat(receivedMessages.get(thirdMessageGroupId)).containsSequence(thirdMessageGroupMessages);
//	}
//
//}
