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

import io.floci.testcontainers.FlociContainer;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;
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
 * The base contract for KCL-based JUnit tests, backed by a shared Floci container. KCL drives Kinesis, DynamoDB (lease
 * table), DynamoDB Streams and CloudWatch, all of which Floci emulates in-process. The container instance is shared via
 * a static property, so it is started only once per JVM and cleaned up by Ryuk after JVM exit.
 *
 * @since 4.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface FlociContainerTest {

	FlociContainer FLOCI_CONTAINER = new FlociContainer(DockerImageName.parse("floci/floci:1.5.33"));

	Semaphore STREAM_CREATION = new Semaphore(3);

	@BeforeAll
	static void startContainer() {
		synchronized (FLOCI_CONTAINER) {
			FLOCI_CONTAINER.start();
		}
	}

	/**
	 * Creates a stream and waits until it exists, at most three at a time, mirroring
	 * {@link LocalstackContainerTest#createStream}. An 'already exists' answer to an SDK retry is treated as success.
	 */
	static CompletableFuture<WaiterResponse<DescribeStreamResponse>> createStream(KinesisAsyncClient client,
			String streamName, int shardCount) {
		STREAM_CREATION.acquireUninterruptibly();
		try {
			return client.createStream(request -> request.streamName(streamName).shardCount(shardCount))
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

	static URI endpoint() {
		return URI.create(FLOCI_CONTAINER.getEndpoint());
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
		return StaticCredentialsProvider
				.create(AwsBasicCredentials.create(FLOCI_CONTAINER.getAccessKey(), FLOCI_CONTAINER.getSecretKey()));
	}

	private static <B extends AwsClientBuilder<B, T>, T> T applyAwsClientOptions(B clientBuilder) {
		return clientBuilder.region(Region.of(FLOCI_CONTAINER.getRegion())).credentialsProvider(credentialsProvider())
				.endpointOverride(endpoint()).build();
	}

}
