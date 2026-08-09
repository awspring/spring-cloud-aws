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
package io.awspring.cloud.kinesis;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.ResourceInUseException;

/**
 * The base contract for JUnit tests based on the container for Localstack. The Testcontainers 'reuse' option must be
 * disabled, so, Ryuk container is started and will clean all the containers up from this test suite after JVM exit.
 * Since the Localstack container instance is shared via static property, it is going to be started only once per JVM;
 * therefore, the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface LocalstackContainerTest {

	LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	Semaphore STREAM_CREATION = new Semaphore(3);

	@BeforeAll
	static void startContainer() {
		synchronized (LOCAL_STACK_CONTAINER) {
			LOCAL_STACK_CONTAINER.start();
		}
	}

	/**
	 * Creates a stream and waits until it exists, at most three at a time. Test classes run concurrently and AWS only
	 * allows a few streams to be in the 'CREATING' state at once, which the concurrent creations were exceeding.
	 */
	static CompletableFuture<WaiterResponse<DescribeStreamResponse>> createStream(KinesisAsyncClient client,
			String streamName, int shardCount) {
		STREAM_CREATION.acquireUninterruptibly();
		try {
			return client.createStream(request -> request.streamName(streamName).shardCount(shardCount))
					// A slow CreateStream can exceed the client's read timeout, and the SDK retries it. The
					// stream is created either way, so the retry answers 'already exists', which is the state
					// this method is asking for.
					.exceptionally(throwable -> {
						if (hasCause(throwable, ResourceInUseException.class)) {
							return null;
						}
						throw throwable instanceof CompletionException ? (CompletionException) throwable
								: new CompletionException(throwable);
					})
					.thenCompose(
							result -> client.waiter().waitUntilStreamExists(request -> request.streamName(streamName)))
					.whenComplete((result, throwable) -> STREAM_CREATION.release());
		}
		catch (RuntimeException ex) {
			// The release is attached to the future, so a throw before it exists would lose the permit and
			// eventually block every later creation.
			STREAM_CREATION.release();
			throw ex;
		}
	}

	private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (type.isInstance(current)) {
				return true;
			}
		}
		return false;
	}

	static KinesisAsyncClient kinesisClient() {
		return applyAwsClientOptions(KinesisAsyncClient.builder().httpClientBuilder(NettyNioAsyncHttpClient.builder()));
	}

	static DynamoDbAsyncClient dynamoDbClient() {
		return applyAwsClientOptions(DynamoDbAsyncClient.builder());
	}

	static DynamoDbStreamsClient dynamoDbStreamsClient() {
		return applyAwsClientOptions(DynamoDbStreamsClient.builder());
	}

	static CloudWatchAsyncClient cloudWatchClient() {
		return applyAwsClientOptions(CloudWatchAsyncClient.builder());
	}

	static AwsCredentialsProvider credentialsProvider() {
		return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(LOCAL_STACK_CONTAINER.getAccessKey(), LOCAL_STACK_CONTAINER.getSecretKey()));
	}

	private static <B extends AwsClientBuilder<B, T>, T> T applyAwsClientOptions(B clientBuilder) {
		return clientBuilder.region(Region.of(LOCAL_STACK_CONTAINER.getRegion()))
				.credentialsProvider(credentialsProvider()).endpointOverride(LOCAL_STACK_CONTAINER.getEndpoint())
				.build();
	}

}
