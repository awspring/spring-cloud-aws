/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Testcontainers
public class DynamoDbTemplateIntegrationTest {

	private static DynamoDbTable<PersonEntity> dynamoDbTable;
	private static DynamoDbTemplate dynamoDbTemplate;
	private static final String indexName = "gsiPersonEntityTable";
	private static final String nameOfGSPK = "gsPk";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:1.3.1")).withServices(DYNAMODB).withReuse(true);

	@BeforeAll
	public static void createTable() {
		DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
				.endpointOverride(localstack.getEndpointOverride(DYNAMODB)).region(Region.of(localstack.getRegion()))
				.credentialsProvider(StaticCredentialsProvider
						.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
				.build();
		DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
		dynamoDbTemplate = new DynamoDbTemplate(enhancedClient);
		dynamoDbTable = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build().table("person_entity",
				TableSchema.fromBean(PersonEntity.class));
		describeAndCreateTable(dynamoDbClient);
	}

	@Test
	void dynamoDbTemplate_saveAndRead_entitySuccessful() {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isEqualTo(personEntity);

		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_read_entitySuccessful_returnsNull() {
		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(UUID.randomUUID().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isNull();
	}

	@Test
	void dynamoDbTemplate_saveUpdateAndRead_entitySuccessful() {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		personEntity.setLastName("xxx");
		dynamoDbTemplate.update(personEntity);

		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isEqualTo(personEntity);

		// clean up
		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndDelete_entitySuccessful() {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		dynamoDbTemplate.delete(personEntity);
		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isEqualTo(null);
		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndDelete_entitySuccessful_forKey() {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		dynamoDbTemplate.delete(Key.builder().partitionValue(personEntity.getUuid().toString()).build(),
				PersonEntity.class);

		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isEqualTo(null);
		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_delete_entitySuccessful_forNotEntityInTable() {
		dynamoDbTemplate.delete(Key.builder().partitionValue(UUID.randomUUID().toString()).build(), PersonEntity.class);
	}

	@Test
	void dynamoDbTemplate_queryIndex_returns_singlePagePerson() {
		PersonEntity personEntity = PersonEntity.Builder.person().withName("foo").withLastName("bar")
				.withGpk1("thisIsMyKey").withUuid(UUID.randomUUID()).build();
		dynamoDbTemplate.save(personEntity);

		QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
				.queryConditional(
						QueryConditional.keyEqualTo(Key.builder().partitionValue(personEntity.getGsPk()).build()))
				.build();

		PageIterable<PersonEntity> persons = dynamoDbTemplate.query(queryEnhancedRequest, PersonEntity.class,
				indexName);
		// items size
		assertThat(persons.items().stream()).hasSize(1);
		// page size
		assertThat(persons.stream()).hasSize(1);
		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_query_returns_singlePagePerson() {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional(
				QueryConditional.keyEqualTo(Key.builder().partitionValue(personEntity.getUuid().toString()).build()))
				.build();

		PageIterable<PersonEntity> persons = dynamoDbTemplate.query(queryEnhancedRequest, PersonEntity.class);
		// items size
		assertThat(persons.items().stream()).hasSize(1);
		// page size
		assertThat(persons.stream()).hasSize(1);
		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_query_returns_empty() {
		QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
				.queryConditional(
						QueryConditional.keyEqualTo(Key.builder().partitionValue(UUID.randomUUID().toString()).build()))
				.build();

		PageIterable<PersonEntity> persons = dynamoDbTemplate.query(queryEnhancedRequest, PersonEntity.class);
		// items size
		assertThat(persons.items().stream()).isEmpty();
		// page size
		assertThat(persons.stream()).hasSize(1);
	}

	@Test
	void dynamoDbTemplate_saveAndScanAll_entitySuccessful() {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class);
		assertThat(persons.items().iterator().next()).isEqualTo(personEntity);

		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndScanAll_index_entitySuccessful() {
		PersonEntity personEntity = PersonEntity.Builder.person().withName("foo").withLastName("bar")
				.withGpk1("partition_key").withUuid(UUID.randomUUID()).build();
		dynamoDbTemplate.save(personEntity);

		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class, indexName);
		assertThat(persons.items().iterator().next()).isEqualTo(personEntity);

		cleanUp(personEntity.getUuid());
	}

	@Test
	void dynamoDbTemplate_scanAll_returnsEmpty() {
		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class);
		assertThat(persons.items().stream()).isEmpty();
	}

	@Test
	void dynamoDbTemplate_saveAndScanAllReturnsMultiplePersons_entitySuccessful() {
		PersonEntity personEntity1 = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		PersonEntity personEntity2 = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity1);
		dynamoDbTemplate.save(personEntity2);
		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class);
		assertThat(persons.items().stream()).hasSize(2);
		cleanUp(personEntity1.getUuid());
		cleanUp(personEntity2.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndScanForParticular_entitySuccessful() {
		PersonEntity personEntity1 = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		PersonEntity personEntity2 = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity1);
		dynamoDbTemplate.save(personEntity2);
		Expression expression = Expression.builder().expression("#uuidToBeLooked = :myValue")
				.putExpressionName("#uuidToBeLooked", "uuid")
				.putExpressionValue(":myValue", AttributeValue.builder().s(personEntity1.getUuid().toString()).build())
				.build();
		ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder().limit(2).consistentRead(true)
				.filterExpression(expression).build();
		PageIterable<PersonEntity> persons = dynamoDbTemplate.scan(scanEnhancedRequest, PersonEntity.class);
		assertThat(persons.items().stream()).hasSize(1);
		assertThat(persons.items().iterator().next()).isEqualTo(personEntity1);
		cleanUp(personEntity1.getUuid());
		cleanUp(personEntity2.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndScanForParticularIndex_entitySuccessful() {
		PersonEntity personEntity1 = PersonEntity.Builder.person().withName("foo").withLastName("bar")
				.withGpk1("my_random_key").withUuid(UUID.randomUUID()).build();
		PersonEntity personEntity2 = PersonEntity.Builder.person().withName("foo").withLastName("bar")
				.withGpk1("some_other_key").withUuid(UUID.randomUUID()).build();
		dynamoDbTemplate.save(personEntity1);
		dynamoDbTemplate.save(personEntity2);
		Expression expression = Expression.builder().expression("#gsPkToBeLooked = :myValue")
				.putExpressionName("#gsPkToBeLooked", nameOfGSPK)
				.putExpressionValue(":myValue", AttributeValue.builder().s(personEntity1.getGsPk()).build()).build();
		ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder().limit(2)
				.filterExpression(expression).build();
		PageIterable<PersonEntity> persons = dynamoDbTemplate.scan(scanEnhancedRequest, PersonEntity.class, indexName);
		assertThat(persons.items().stream()).hasSize(1);
		assertThat(persons.items().iterator().next()).isEqualTo(personEntity1);
		cleanUp(personEntity1.getUuid());
		cleanUp(personEntity2.getUuid());
	}

	public static void cleanUp(UUID uuid) {
		dynamoDbTable.deleteItem(Key.builder().partitionValue(uuid.toString()).build());
	}

	private static void describeAndCreateTable(DynamoDbClient dynamoDbClient) {
		ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(AttributeDefinition.builder().attributeName("uuid").attributeType("S").build());
		attributeDefinitions.add(AttributeDefinition.builder().attributeName(nameOfGSPK).attributeType("S").build());
		ArrayList<KeySchemaElement> tableKeySchema = new ArrayList<KeySchemaElement>();
		tableKeySchema.add(KeySchemaElement.builder().attributeName("uuid").keyType(KeyType.HASH).build());
		List<KeySchemaElement> indexKeySchema = new ArrayList<>();
		indexKeySchema.add(KeySchemaElement.builder().attributeName(nameOfGSPK).keyType(KeyType.HASH).build());

		GlobalSecondaryIndex precipIndex = GlobalSecondaryIndex.builder().indexName(indexName)
				.provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits((long) 10)
						.writeCapacityUnits((long) 1).build())
				.projection(Projection.builder().projectionType(ProjectionType.ALL).build()).keySchema(indexKeySchema)
				.build();

		CreateTableRequest createTableRequest = CreateTableRequest.builder().tableName("person_entity")
				.provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits((long) 10)
						.writeCapacityUnits((long) 1).build())
				.attributeDefinitions(attributeDefinitions).keySchema(tableKeySchema)
				.globalSecondaryIndexes(precipIndex).build();
		dynamoDbClient.createTable(createTableRequest);
	}
}
