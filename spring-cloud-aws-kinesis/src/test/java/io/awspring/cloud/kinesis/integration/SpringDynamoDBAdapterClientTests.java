/*
 * Copyright 2013-2019 the original author or authors.
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
package io.awspring.cloud.kinesis.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.DescribeStreamRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeStreamResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Shard;
import software.amazon.awssdk.services.dynamodb.model.StreamDescription;
import software.amazon.awssdk.services.dynamodb.model.StreamStatus;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.kinesis.model.InvalidArgumentException;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;

/**
 * @author Asiel Caballero
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class SpringDynamoDBAdapterClientTests {

	private final DynamoDbStreamsClient amazonDynamoDBStreams = new InMemoryDynamoDbStreamsClient();

	private final SpringDynamoDBStreamsAdapterClient springDynamoDBAdapterClient = new SpringDynamoDBStreamsAdapterClient(
			amazonDynamoDBStreams);

	@Test
	public void listShardsForNotFoundStream() {
		assertThat(springDynamoDBAdapterClient.listShards(builder -> builder.streamName("not-found")))
				.completesExceptionallyWithin(Duration.ofSeconds(10)).withThrowableThat()
				.withCauseInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	public void listShardsWithInvalidToken() {
		assertThat(springDynamoDBAdapterClient.listShards(builder -> builder.nextToken("invalid-token")))
				.completesExceptionallyWithin(Duration.ofSeconds(10)).withThrowableThat()
				.withCauseInstanceOf(InvalidArgumentException.class);
	}

	@Test
	public void listShardsWithTokenAndStreamName() {
		assertThat(springDynamoDBAdapterClient.listShards(
				builder -> builder.streamName(InMemoryDynamoDbStreamsClient.STREAM_ARN).nextToken("valid!!##%%token")))
				.completesExceptionallyWithin(Duration.ofSeconds(10)).withThrowableThat()
				.withCauseInstanceOf(InvalidArgumentException.class);
	}

	@Test
	public void listShardsNoPagination() {
		ListShardsResponse listShards = springDynamoDBAdapterClient
				.listShards(builder -> builder.streamName(InMemoryDynamoDbStreamsClient.STREAM_ARN)).join();

		assertThat(listShards.nextToken()).isNull();
		assertThat(listShards.shards()).hasSize(InMemoryDynamoDbStreamsClient.SHARDS.size());
	}

	@Test
	public void listShardWithTokenPagination() {
		int maxResults = (InMemoryDynamoDbStreamsClient.SHARDS.size() / 2)
				+ (InMemoryDynamoDbStreamsClient.SHARDS.size() % 2);
		ListShardsResponse listShards = springDynamoDBAdapterClient
				.listShards(
						builder -> builder.streamName(InMemoryDynamoDbStreamsClient.STREAM_ARN).maxResults(maxResults))
				.join();

		assertThat(listShards.nextToken()).isNotNull();
		assertThat(listShards.shards()).hasSize(maxResults);

		listShards = springDynamoDBAdapterClient
				.listShards(ListShardsRequest.builder().nextToken(listShards.nextToken()).build()).join();

		assertThat(listShards.nextToken()).isNull();
		assertThat(listShards.shards()).hasSize(InMemoryDynamoDbStreamsClient.SHARDS.size() - maxResults);
	}

	@Test
	public void listShardsWithShardIdPagination() {
		ListShardsResponse listShards = springDynamoDBAdapterClient
				.listShards(builder -> builder.streamName(InMemoryDynamoDbStreamsClient.STREAM_ARN)
						.exclusiveStartShardId(InMemoryDynamoDbStreamsClient.SHARDS.get(2).shardId()).maxResults(1))
				.join();

		assertThat(listShards.nextToken()).isNotNull();
		assertThat(listShards.shards()).hasSize(1);
		assertThat(listShards.shards().get(0).shardId())
				.isEqualTo(InMemoryDynamoDbStreamsClient.SHARDS.get(3).shardId());
	}

	private static class InMemoryDynamoDbStreamsClient implements DynamoDbStreamsClient {

		private static final String TABLE_NAME = "test-streams";

		private static final String STREAM_LABEL = "2020-10-21T11:49:13.355";

		private static final String STREAM_ARN = String.format("arn:aws:dynamodb:%s:%s:table/%s/stream/%s",
				Region.US_EAST_1.id(), "000000000000", TABLE_NAME, STREAM_LABEL);

		private static final List<Shard> SHARDS = Arrays.asList(
				buildShard("shardId-00000001603195033866-c5d0c2b1", "51100000000002515059163",
						"51300000000002521847055").build(),
				buildShard("shardId-00000001603208699318-b15c42af", "804300000000026046960744",
						"804300000000026046960744").parentShardId("shardId-00000001603195033866-c5d0c2b1").build(),
				buildShard("shardId-00000001603223404428-90b80e6c", "1613900000000033324335703",
						"1613900000000033324335703").parentShardId("shardId-00000001603208699318-b15c42af").build(),
				buildShard("shardId-00000001603237029376-bd9c40dd", "2364600000000001701561758",
						"2364600000000001701561758").parentShardId("shardId-00000001603223404428-90b80e6c").build(),
				buildShard("shardId-00000001603249855034-b917a47f", "3071400000000035046301998",
						"3071400000000035046301998").parentShardId("shardId-00000001603237029376-bd9c40dd").build());

		private final StreamDescription.Builder streamDescription = StreamDescription.builder().streamArn(STREAM_ARN)
				.streamLabel(STREAM_LABEL).streamViewType(StreamViewType.KEYS_ONLY)
				.creationRequestDateTime(Instant.now()).tableName(TABLE_NAME)
				.keySchema(KeySchemaElement.builder().attributeName("name").keyType(KeyType.HASH).build())
				.streamStatus(StreamStatus.ENABLED);

		private static Shard.Builder buildShard(String parentShardIdString, String startingSequenceNumber,
				String endingSequenceNumber) {

			return Shard.builder().shardId(parentShardIdString).sequenceNumberRange(builder -> builder
					.startingSequenceNumber(startingSequenceNumber).endingSequenceNumber(endingSequenceNumber));
		}

		@Override
		public DescribeStreamResponse describeStream(DescribeStreamRequest request) {
			// Invalid StreamArn (Service: AmazonDynamoDBStreams; Status Code: 400;
			// Error Code: ValidationException; Request ID: <Request ID>; Proxy: null)
			if (!STREAM_ARN.equals(request.streamArn())) {
				throw software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException.builder()
						.awsErrorDetails(
								AwsErrorDetails.builder().errorCode("404").errorMessage("Invalid StreamArn").build())
						.build();
			}

			int limit = request.limit() != null ? request.limit() : 100;

			int shardIndex = request.exclusiveStartShardId() != null ? findShardIndex(request.exclusiveStartShardId())
					: 0;
			int lastShardIndex = Math.min(shardIndex + limit, SHARDS.size());
			streamDescription.shards(SHARDS.subList(shardIndex, lastShardIndex)).lastEvaluatedShardId(
					lastShardIndex != SHARDS.size() ? SHARDS.get(lastShardIndex).shardId() : null);

			return DescribeStreamResponse.builder().streamDescription(streamDescription.build()).build();
		}

		private static int findShardIndex(String shardId) {
			int i = 0;
			// checkstyle forced this way of writing it
			while (i < SHARDS.size() && !SHARDS.get(i).shardId().equals(shardId)) {
				i++;
			}

			if (i + 1 >= SHARDS.size()) {
				throw new RuntimeException("ShardId not found");
			}

			return i + 1;
		}

		@Override
		public String serviceName() {
			return DynamoDbStreamsClient.SERVICE_NAME;
		}

		@Override
		public void close() {

		}

	}

}
