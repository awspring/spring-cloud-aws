/*
 * Copyright 2013-2023 the original author or authors.
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
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

/**
 * Tests for {@link DynamoDbTemplate}.
 *
 * @author Matej Nedic
 * @author Arun Patra
 * @author Artem Bilan
 * @author Marcus Voltolim
 */
public class DynamoDbTemplateIntegrationTest implements LocalstackContainerTest {

	private static DynamoDbTable<PersonEntity> dynamoDbTable;
	private static DynamoDbTable<PersonEntity> prefixedDynamoDbTable;
	private static DynamoDbTemplate dynamoDbTemplate;
	private static DynamoDbTemplate prefixedDynamoDbTemplate;
	private static final String indexName = "gsiPersonEntityTable";
	private static final String nameOfGSPK = "gsPk";

	@BeforeAll
	static void createTable() {
		DynamoDbClient dynamoDbClient = LocalstackContainerTest.dynamoDbClient();
		DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();

		dynamoDbTemplate = new DynamoDbTemplate(enhancedClient);
		dynamoDbTable = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build().table("person_entity",
				TableSchema.fromBean(PersonEntity.class));
		describeAndCreateTable(dynamoDbClient, null);

		prefixedDynamoDbTemplate = new DynamoDbTemplate("my_prefix_", enhancedClient);
		prefixedDynamoDbTable = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build()
				.table("my_prefix_person_entity", TableSchema.fromBean(PersonEntity.class));
		describeAndCreateTable(dynamoDbClient, "my_prefix_");
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndRead_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isEqualTo(personEntity);

		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_read_entitySuccessful_returnsNull(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(UUID.randomUUID().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isNull();
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveUpdateAndRead_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		personEntity.setName(null);
		personEntity.setLastName("xxx");
		dynamoDbTemplate.update(personEntity);

		Key key = Key.builder().partitionValue(personEntity.getUuid().toString()).build();

		assertThat(dynamoDbTemplate.load(key, PersonEntity.class))
				.extracting(PersonEntity::getName, PersonEntity::getLastName).containsExactly(null, "xxx");

		// clean up
		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveUpdateIgnoreNullAndRead_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		personEntity.setName(null);
		personEntity.setLastName("xxx");
		UpdateItemEnhancedRequest<PersonEntity> request = UpdateItemEnhancedRequest.builder(PersonEntity.class)
				.item(personEntity).ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY).build();
		dynamoDbTemplate.update(request);

		Key key = Key.builder().partitionValue(personEntity.getUuid().toString()).build();

		assertThat(dynamoDbTemplate.load(key, PersonEntity.class))
				.extracting(PersonEntity::getName, PersonEntity::getLastName).containsExactly("foo", "xxx");

		// clean up
		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndDelete_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		var deletedItem = dynamoDbTemplate.delete(personEntity);

		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(deletedItem.getName()).isEqualTo("foo");
		assertThat(personEntity1).isEqualTo(null);
		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndDelete_entitySuccessful_forKey(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		dynamoDbTemplate.delete(Key.builder().partitionValue(personEntity.getUuid().toString()).build(),
				PersonEntity.class);

		PersonEntity personEntity1 = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(personEntity1).isEqualTo(null);
		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_delete_entitySuccessful_forNotEntityInTable(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		dynamoDbTemplate.delete(Key.builder().partitionValue(UUID.randomUUID().toString()).build(), PersonEntity.class);
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_queryIndex_returns_singlePagePerson(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
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
		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_query_returns_singlePagePerson(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
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
		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_query_returns_empty(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
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

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndScanAll_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity);

		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class);
		assertThat(persons.items().iterator().next()).isEqualTo(personEntity);

		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndScanAll_index_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity = PersonEntity.Builder.person().withName("foo").withLastName("bar")
				.withGpk1("partition_key").withUuid(UUID.randomUUID()).build();
		dynamoDbTemplate.save(personEntity);

		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class, indexName);
		assertThat(persons.items().iterator().next()).isEqualTo(personEntity);

		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_scanAll_returnsEmpty(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class);
		assertThat(persons.items().stream()).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndScanAllReturnsMultiplePersons_entitySuccessful(
			DynamoDbTable<PersonEntity> dynamoDbTable, DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity1 = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		PersonEntity personEntity2 = new PersonEntity(UUID.randomUUID(), "foo", "bar");
		dynamoDbTemplate.save(personEntity1);
		dynamoDbTemplate.save(personEntity2);
		PageIterable<PersonEntity> persons = dynamoDbTemplate.scanAll(PersonEntity.class);
		assertThat(persons.items().stream()).hasSize(2);
		cleanUp(dynamoDbTable, personEntity1.getUuid());
		cleanUp(dynamoDbTable, personEntity2.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndScanForParticular_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
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
		cleanUp(dynamoDbTable, personEntity1.getUuid());
		cleanUp(dynamoDbTable, personEntity2.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveAndScanForParticularIndex_entitySuccessful(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		PersonEntity personEntity1 = PersonEntity.Builder.person().withName("foo").withLastName("bar")
				.withGpk1("my_random_key").withUuid(UUID.randomUUID()).build();
		PersonEntity personEntity2 = PersonEntity.Builder.person().withName("foo").withLastName("bar")
				.withGpk1("some_other_key").withUuid(UUID.randomUUID()).build();
		dynamoDbTemplate.save(personEntity1);
		dynamoDbTemplate.save(personEntity2);
		Expression expression = Expression.builder().expression("#gsPkToBeLooked = :myValue")
				.putExpressionName("#gsPkToBeLooked", nameOfGSPK)
				.putExpressionValue(":myValue", AttributeValue.builder().s(personEntity1.getGsPk()).build()).build();
		ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder().limit(2).filterExpression(expression)
				.build();
		PageIterable<PersonEntity> persons = dynamoDbTemplate.scan(scanEnhancedRequest, PersonEntity.class, indexName);
		assertThat(persons.items().stream()).hasSize(1);
		assertThat(persons.items().iterator().next()).isEqualTo(personEntity1);
		cleanUp(dynamoDbTable, personEntity1.getUuid());
		cleanUp(dynamoDbTable, personEntity2.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveConditionallyAndRead_entitySuccessfully(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		UUID uuid = UUID.randomUUID();
		PersonEntity personEntity = new PersonEntity(uuid, "foo", null);
		PersonEntity secondPersonEntity = new PersonEntity(uuid, "foo", "jar");
		PutItemEnhancedRequest<PersonEntity> putItemEnhancedRequest = PutItemEnhancedRequest.builder(PersonEntity.class)
				.conditionExpression(Expression.builder().expression("attribute_not_exists(lastName)").build())
				.item(secondPersonEntity).build();
		// save a person with lastName "bar"
		dynamoDbTemplate.save(personEntity);
		// attempt to replace person with lastName "jar"
		dynamoDbTemplate.save(putItemEnhancedRequest);
		PersonEntity savedPersonEntity = dynamoDbTemplate.load(
				Key.builder().partitionValue(secondPersonEntity.getUuid().toString()).build(), PersonEntity.class);
		assertThat(savedPersonEntity).isEqualTo(secondPersonEntity);

		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_saveConditionally_entityFails(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		UUID uuid = UUID.randomUUID();
		PersonEntity personEntity = new PersonEntity(uuid, "foo", "bar");
		PersonEntity secondPersonEntity = new PersonEntity(uuid, "foo", "jar");
		PutItemEnhancedRequest<PersonEntity> putItemEnhancedRequest = PutItemEnhancedRequest.builder(PersonEntity.class)
				.conditionExpression(Expression.builder().expression("attribute_not_exists(lastName)").build())
				.item(secondPersonEntity).build();
		// save person with lastName "bar"
		dynamoDbTemplate.save(personEntity);
		// try to save new lastName "jar" for the same person
		assertThrows(DynamoDbException.class, () -> {
			dynamoDbTemplate.save(putItemEnhancedRequest);
		});

		cleanUp(dynamoDbTable, personEntity.getUuid());

	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_deleteConditionally_entitySuccessfully(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		UUID uuid = UUID.randomUUID();
		PersonEntity personEntity = new PersonEntity(uuid, "notfoo", "bar");
		DeleteItemEnhancedRequest deleteItemEnhancedRequest = DeleteItemEnhancedRequest.builder()
				.conditionExpression(Expression.builder().expression("#nameNotBeDeleted <> :value")
						.putExpressionName("#nameNotBeDeleted", "name")
						.putExpressionValue(":value", AttributeValue.builder().s("foo").build()).build())
				.key(Key.builder().partitionValue(personEntity.getUuid().toString()).build()).build();
		dynamoDbTemplate.save(personEntity);
		dynamoDbTemplate.delete(deleteItemEnhancedRequest, PersonEntity.class);

		PersonEntity deletedEntity = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);

		assertThat(deletedEntity).isNull();
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void dynamoDbTemplate_deleteConditionally_entityFails(DynamoDbTable<PersonEntity> dynamoDbTable,
			DynamoDbTemplate dynamoDbTemplate) {
		UUID uuid = UUID.randomUUID();
		PersonEntity personEntity = new PersonEntity(uuid, "foo", "bar");
		DeleteItemEnhancedRequest deleteItemEnhancedRequest = DeleteItemEnhancedRequest.builder()
				.conditionExpression(Expression.builder().expression("#nameNotBeDeleted <> :value")
						.putExpressionName("#nameNotBeDeleted", "name")
						.putExpressionValue(":value", AttributeValue.builder().s("foo").build()).build())
				.key(Key.builder().partitionValue(personEntity.getUuid().toString()).build()).build();
		dynamoDbTemplate.save(personEntity);
		assertThrows(DynamoDbException.class, () -> {
			dynamoDbTemplate.delete(deleteItemEnhancedRequest, PersonEntity.class);
		});

		PersonEntity deletedEntity = dynamoDbTemplate
				.load(Key.builder().partitionValue(personEntity.getUuid().toString()).build(), PersonEntity.class);

		assertThat(deletedEntity).isNotNull();
		cleanUp(dynamoDbTable, personEntity.getUuid());
	}

	public static void cleanUp(DynamoDbTable<PersonEntity> dynamoDbTable, UUID uuid) {
		dynamoDbTable.deleteItem(Key.builder().partitionValue(uuid.toString()).build());
	}

	private static Stream<Arguments> argumentSource() {
		return Stream.of(Arguments.of(dynamoDbTable, dynamoDbTemplate),
				Arguments.of(prefixedDynamoDbTable, prefixedDynamoDbTemplate));
	}

	private static void describeAndCreateTable(DynamoDbClient dynamoDbClient, @Nullable String tablePrefix) {
		ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
		attributeDefinitions.add(AttributeDefinition.builder().attributeName("uuid").attributeType("S").build());
		attributeDefinitions.add(AttributeDefinition.builder().attributeName(nameOfGSPK).attributeType("S").build());
		ArrayList<KeySchemaElement> tableKeySchema = new ArrayList<>();
		tableKeySchema.add(KeySchemaElement.builder().attributeName("uuid").keyType(KeyType.HASH).build());
		List<KeySchemaElement> indexKeySchema = new ArrayList<>();
		indexKeySchema.add(KeySchemaElement.builder().attributeName(nameOfGSPK).keyType(KeyType.HASH).build());

		GlobalSecondaryIndex precipIndex = GlobalSecondaryIndex.builder().indexName(indexName)
				.provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits((long) 10)
						.writeCapacityUnits((long) 1).build())
				.projection(Projection.builder().projectionType(ProjectionType.ALL).build()).keySchema(indexKeySchema)
				.build();
		String tableName = StringUtils.hasText(tablePrefix) ? tablePrefix.concat("person_entity") : "person_entity";
		CreateTableRequest createTableRequest = CreateTableRequest.builder().tableName(tableName)
				.provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits((long) 10)
						.writeCapacityUnits((long) 1).build())
				.attributeDefinitions(attributeDefinitions).keySchema(tableKeySchema)
				.globalSecondaryIndexes(precipIndex).build();

		try {
			dynamoDbClient.createTable(createTableRequest);
		}
		catch (ResourceInUseException e) {
			// table already exists, do nothing
		}
	}

}
