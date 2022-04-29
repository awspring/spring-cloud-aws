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
package io.awspring.cloud.autoconfigure.dynamodb.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.dynamodb.DynamoDbAutoConfiguration;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Integration tests for DynamoDBTemplate.
 *
 * @author Matej Nedic
 */

@SpringBootTest(classes = { CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class,
		AwsAutoConfiguration.class, DynamoDbAutoConfiguration.class })
@Testcontainers
public class DynamoDbTemplateIntegrationTest {

	private static final String REGION = "us-east-1";
	private static DynamoDbTable dynamoDbTable;
	@Autowired
	private DynamoDbClient dynamoDbClient;
	@Autowired
	private DynamoDbTemplate dynamoDbTemplate;

	@DynamicPropertySource
	static void snsProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.aws.dynamodb.region", REGION::toString);
		registry.add("spring.cloud.aws.dynamodb.endpoint", localstack.getEndpointOverride(DYNAMODB)::toString);
		registry.add("spring.cloud.aws.credentials.access-key", "noop"::toString);
		registry.add("spring.cloud.aws.credentials.secret-key", "noop"::toString);
		registry.add("spring.cloud.aws.region.static", "eu-west-1"::toString);
	}

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(DYNAMODB).withReuse(true);

	@BeforeAll
	public static void createTable() {
		DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
				.endpointOverride(localstack.getEndpointOverride(DYNAMODB)).region(Region.of(localstack.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
				.build();
		dynamoDbTable = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build().table("person",
				TableSchema.fromBean(Person.class));
		dynamoDbTable.createTable();
	}

	@Test
	void dynamoDbTemplate_save_entitySuccessful() {
		Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID())
				.build();
		assertThatCode(() -> dynamoDbTemplate.save(person)).doesNotThrowAnyException();

		cleanUp(person.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndRead_entitySuccessful() {
		Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID())
				.build();
		assertThatCode(() -> dynamoDbTemplate.save(person)).doesNotThrowAnyException();

		assertThatCode(() -> {
			Person person1 = dynamoDbTemplate.load(Key.builder().partitionValue(person.getUuid().toString()).build(),
					Person.class);
			assertThat(person1).isEqualTo(person);
		}).doesNotThrowAnyException();

		cleanUp(person.getUuid());
	}

	@Test
	void dynamoDbTemplate_read_entitySuccessful_returnsNull() {
		assertThatCode(() -> {
			Person person1 = dynamoDbTemplate.load(Key.builder().partitionValue(UUID.randomUUID().toString()).build(),
					Person.class);
			assertThat(person1).isNull();
		}).doesNotThrowAnyException();
	}

	@Test
	void dynamoDbTemplate_saveUpdateAndRead_entitySuccessful() {
		Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID())
				.build();
		assertThatCode(() -> dynamoDbTemplate.save(person)).doesNotThrowAnyException();

		person.setLastName("xxx");
		assertThatCode(() -> dynamoDbTemplate.update(person)).doesNotThrowAnyException();

		assertThatCode(() -> {
			Person person1 = dynamoDbTemplate.load(Key.builder().partitionValue(person.getUuid().toString()).build(),
					Person.class);
			assertThat(person1).isEqualTo(person);
		}).doesNotThrowAnyException();

		// clean up
		cleanUp(person.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndDelete_entitySuccessful() {

		Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID())
				.build();
		assertThatCode(() -> dynamoDbTemplate.save(person)).doesNotThrowAnyException();

		assertThatCode(() -> dynamoDbTemplate.delete(person)).doesNotThrowAnyException();

		assertThatCode(() -> {
			Person person1 = dynamoDbTemplate.load(Key.builder().partitionValue(person.getUuid().toString()).build(),
					Person.class);
			assertThat(person1).isEqualTo(null);
		}).doesNotThrowAnyException();
		cleanUp(person.getUuid());
	}

	@Test
	void dynamoDbTemplate_saveAndDelete_entitySuccessful_forKey() {

		Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID())
				.build();
		assertThatCode(() -> dynamoDbTemplate.save(person)).doesNotThrowAnyException();

		assertThatCode(() -> dynamoDbTemplate.delete(Key.builder().partitionValue(person.getUuid().toString()).build(),
				Person.class)).doesNotThrowAnyException();

		assertThatCode(() -> {
			Person person1 = dynamoDbTemplate.load(Key.builder().partitionValue(person.getUuid().toString()).build(),
					Person.class);
			assertThat(person1).isEqualTo(null);
		}).doesNotThrowAnyException();
		cleanUp(person.getUuid());
	}

	@Test
	void dynamoDbTemplate_delete_entitySuccessful_forNotEntityInTable() {
		assertThatCode(() -> dynamoDbTemplate.delete(Key.builder().partitionValue(UUID.randomUUID().toString()).build(),
				Person.class)).doesNotThrowAnyException();
	}

	@Test
	void dynamoDbTemplate_query_returns_singlePagePerson() {
		Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID())
				.build();
		assertThatCode(() -> dynamoDbTemplate.save(person)).doesNotThrowAnyException();

		QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
				.queryConditional(
						QueryConditional.keyEqualTo(Key.builder().partitionValue(person.getUuid().toString()).build()))
				.build();

		PageIterable<Person> persons = dynamoDbTemplate.query(queryEnhancedRequest, Person.class);
		// items size
		assertThat((int) persons.items().stream().count()).isEqualTo(1);
		// page size
		assertThat((int) persons.stream().count()).isEqualTo(1);
		cleanUp(person.getUuid());
	}

	@Test
	void dynamoDbTemplate_query_returns_empty() {
		QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
				.queryConditional(
						QueryConditional.keyEqualTo(Key.builder().partitionValue(UUID.randomUUID().toString()).build()))
				.build();

		PageIterable<Person> persons = dynamoDbTemplate.query(queryEnhancedRequest, Person.class);
		// items size
		assertThat((int) persons.items().stream().count()).isEqualTo(0);
		// page size
		assertThat((int) persons.stream().count()).isEqualTo(1);
	}

	public static void cleanUp(UUID uuid) {
		dynamoDbTable.deleteItem(Key.builder().partitionValue(uuid.toString()).build());
	}

}
