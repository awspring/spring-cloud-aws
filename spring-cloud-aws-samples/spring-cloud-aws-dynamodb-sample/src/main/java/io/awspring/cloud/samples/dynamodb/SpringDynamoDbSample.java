/*
 * Copyright 2013-2021 the original author or authors.
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
package io.awspring.cloud.samples.dynamodb;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

import io.awspring.cloud.dynamodb.DynamoDbOperations;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SpringBootApplication
public class SpringDynamoDbSample {

	private DynamoDbOperations dynamoDbOperations;
	private DynamoDbEnhancedClient dynamoDbEnhancedClient;
	private Log LOG = LogFactory.getLog(this.getClass());

	private static LocalStackContainer localStack;

	public SpringDynamoDbSample(DynamoDbOperations dynamoDbOperations, DynamoDbEnhancedClient dynamoDbEnhancedClient) {
		this.dynamoDbOperations = dynamoDbOperations;
		this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
	}

	public static void main(String[] args) {
		Testcontainers.exposeHostPorts(8080);
		localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.0"))
				.withServices(DYNAMODB);
		localStack.start();
		System.setProperty("spring.cloud.aws.dynamodb.region", localStack.getRegion());
		System.setProperty("spring.cloud.aws.dynamodb.endpoint", localStack.getEndpointOverride(DYNAMODB).toString());
		System.setProperty("spring.cloud.aws.credentials.access-key", "test");
		System.setProperty("spring.cloud.aws.credentials.secret-key", "test");
		SpringApplication.run(SpringDynamoDbSample.class, args);

	}

	@EventListener(ApplicationReadyEvent.class)
	public void sendMessage() {
		dynamoDbEnhancedClient.table("banking_information", TableSchema.fromBean(BankingInformation.class))
				.createTable();
		UUID cardId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		BankingInformation bankingInformation = BankingInformation.Builder.builder().withCardId(cardId)
				.withUserId(userId).withCardName("Visa").withValidity(LocalDate.now()).withBalance(BigDecimal.TEN)
				.build();
		// Saving BankingInformation
		dynamoDbOperations.save(bankingInformation);

		// Get Banking Information for CardId
		BankingInformation bankingInformationLoaded = dynamoDbOperations
				.load(Key.builder().partitionValue(AttributeValue.builder().s(cardId.toString()).build())
						.sortValue(userId.toString()).build(), BankingInformation.class);

		// Query
		PageIterable<BankingInformation> bankingInformationPageIterable = dynamoDbOperations
				.query(QueryEnhancedRequest.builder()
						.queryConditional(
								QueryConditional.keyEqualTo(Key.builder().partitionValue(cardId.toString()).build()))
						.build(), BankingInformation.class);

		// Delete
		dynamoDbOperations.delete(bankingInformation);
	}
}
