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
package io.awspring.cloud.kinesis.integration;

import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import com.amazonaws.services.dynamodbv2.streamsadapter.adapter.DynamoDBStreamsGetRecordsResponseAdapter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsRequest;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.InvalidArgumentException;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;

/**
 * This is Spring Cloud DynamoDB Adapter to be able to support {@code ListShards} operations. Also, adapts
 * {@link #getDynamoDBStreamsRecords(software.amazon.awssdk.services.dynamodb.model.GetRecordsRequest)} to
 * {@link #getRecords(software.amazon.awssdk.services.kinesis.model.GetRecordsRequest)} for regular
 * {@link KinesisMessageDrivenChannelAdapter}. The KCL bindings uses {@link KclMessageDrivenChannelAdapter}'s native
 * support for {@link AmazonDynamoDBStreamsAdapterClient} API.
 *
 * @author Asiel Caballero
 * @author Artem Bilan
 *
 * @since 4.0
 *
 * @see <a href="https://docs.aws.amazon.com/kinesis/latest/APIReference/API_ListShards.html">ListShards</a>
 * @see <a href="https://github.com/awslabs/dynamodb-streams-kinesis-adapter/issues/39">ListShards GH issue</a>
 */
public class SpringDynamoDBStreamsAdapterClient extends AmazonDynamoDBStreamsAdapterClient {

	private static final String SEPARATOR = "!!##%%";

	public SpringDynamoDBStreamsAdapterClient(DynamoDbStreamsClient dynamoDbStreamsClient) {
		super(dynamoDbStreamsClient);
	}

	/**
	 * List shards for a DynamoDB Stream using its {@code DescribeStream} API, as they don't support {@code ListShards}
	 * operations. Returns the result adapted to use the AmazonKinesis model.
	 * @param request Container for the necessary parameters to execute the ListShards service method
	 * @return The response from the DescribeStream service method, adapted for use with the AmazonKinesis model
	 */
	@Override
	public CompletableFuture<ListShardsResponse> listShards(ListShardsRequest request) {
		String nextToken = request.nextToken();
		if (nextToken != null && request.streamName() != null) {
			return CompletableFuture.failedFuture(InvalidArgumentException.builder()
					.message("NextToken and StreamName cannot be provided together.").build());
		}

		String streamName = request.streamName();
		String exclusiveStartShardId = request.exclusiveStartShardId();

		if (nextToken != null) {
			String[] split = nextToken.split(SEPARATOR);

			if (split.length != 2) {
				return CompletableFuture
						.failedFuture(InvalidArgumentException.builder().message("Invalid ShardIterator").build());
			}

			streamName = split[0];
			exclusiveStartShardId = split[1];
		}

		DescribeStreamRequest dsr = DescribeStreamRequest.builder().streamName(streamName)
				.exclusiveStartShardId(exclusiveStartShardId).limit(request.maxResults()).build();

		StreamDescription streamDescription;
		try {
			streamDescription = describeStream(dsr).join().streamDescription();
		}
		catch (CompletionException ex) {
			ResourceNotFoundException resourceEx = ResourceNotFoundException.builder().message(ex.getMessage()).build();
			resourceEx.setStackTrace(ex.getStackTrace());
			return CompletableFuture.failedFuture(resourceEx);
		}

		List<Shard> shards = streamDescription.shards();
		ListShardsResponse.Builder result = ListShardsResponse.builder().shards(shards);

		if (streamDescription.hasMoreShards()) {
			result.nextToken(buildFakeNextToken(streamName, shards.get(shards.size() - 1).shardId()));
		}

		return CompletableFuture.completedFuture(result.build());
	}

	@Override
	public CompletableFuture<GetRecordsResponse> getRecords(
			software.amazon.awssdk.services.kinesis.model.GetRecordsRequest getRecordsRequest)
			throws AwsServiceException, SdkClientException {

		GetRecordsRequest dynamoDbStreamsRecordsRequest = GetRecordsRequest.builder().limit(getRecordsRequest.limit())
				.shardIterator(getRecordsRequest.shardIterator()).build();

		return getDynamoDBStreamsRecords(dynamoDbStreamsRecordsRequest).thenApply(this::toGetRecordsResponse);
	}

	private GetRecordsResponse toGetRecordsResponse(DynamoDBStreamsGetRecordsResponseAdapter recordsResponseAdapter) {
		return GetRecordsResponse.builder().childShards(recordsResponseAdapter.childShards())
				.millisBehindLatest(recordsResponseAdapter.millisBehindLatest())
				.nextShardIterator(recordsResponseAdapter.nextShardIterator())
				.records(recordsResponseAdapter.records().stream()
						.map((kinesisClientRecord) -> Record.builder()
								.approximateArrivalTimestamp(kinesisClientRecord.approximateArrivalTimestamp())
								.encryptionType(kinesisClientRecord.encryptionType())
								.partitionKey(kinesisClientRecord.partitionKey())
								.sequenceNumber(kinesisClientRecord.sequenceNumber())
								.data(SdkBytes.fromByteBuffer(kinesisClientRecord.data())).build())
						.toList())
				.build();
	}

	private static String buildFakeNextToken(String streamName, String lastShard) {
		return lastShard != null ? streamName + SEPARATOR + lastShard : null;
	}

}
