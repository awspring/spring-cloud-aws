package io.awspring.cloud.autoconfigure.dynamodb.it;

import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

/**
 * Integration tests for DynamoDBTemplate.
 *
 * @author Matej Nedic
 */
@Testcontainers
public class DynamoDbTemplateIntegrationTest {

	private static final String REGION = "us-east-1";
	private static DynamoDbTable dynamoDbTable;
	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
		DockerImageName.parse("localstack/localstack:0.14.0")).withServices(DYNAMODB).withReuse(true);


	@BeforeAll
	public static void before() {
		DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.of(REGION)).endpointOverride(localstack.getEndpointOverride(DYNAMODB))
			.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop"))).build();
		dynamoDbTable = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build().table("person", TableSchema.fromBean(Person.class));
		dynamoDbTable.createTable();
	}

	@Test
	void save() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			DynamoDbTemplate dynamoDBTemplate = context.getBean(DynamoDbTemplate.class);
			Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID()).build();
			assertThatCode(() -> dynamoDBTemplate.save(person)).doesNotThrowAnyException();

			cleanUp(person.getUuid());
		}
	}

	@Test
	void saveAndRead() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			DynamoDbTemplate dynamoDBTemplate = context.getBean(DynamoDbTemplate.class);
			Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID()).build();
			assertThatCode(() -> dynamoDBTemplate.save(person)).doesNotThrowAnyException();

			assertThatCode(() -> {
				Person person1 = dynamoDBTemplate.load(Key.builder().partitionValue(person.getUuid().toString()).build(), Person.class, "person");
				assertThat(person1).isEqualTo(person);
			}).doesNotThrowAnyException();

			cleanUp(person.getUuid());
		}
	}

	@Test
	void saveUpdateAndRead() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			DynamoDbTemplate dynamoDBTemplate = context.getBean(DynamoDbTemplate.class);
			Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID()).build();
			assertThatCode(() -> dynamoDBTemplate.save(person)).doesNotThrowAnyException();

			person.setLastName("xxx");
			assertThatCode(() -> dynamoDBTemplate.update(person)).doesNotThrowAnyException();

			assertThatCode(() -> {
				Person person1 =	dynamoDBTemplate.load(Key.builder().partitionValue(person.getUuid().toString()).build(), Person.class, "person");
				assertThat(person1).isEqualTo(person);
			}).doesNotThrowAnyException();

			//clean up
			cleanUp(person.getUuid());
		}
	}

	@Test
	void saveAndDelete() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			DynamoDbTemplate dynamoDBTemplate = context.getBean(DynamoDbTemplate.class);
			Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID()).build();
			assertThatCode(() -> dynamoDBTemplate.save(person)).doesNotThrowAnyException();

			assertThatCode(() -> dynamoDBTemplate.delete(person)).doesNotThrowAnyException();

			assertThatCode(() -> {
				Person person1 =	dynamoDBTemplate.load(Key.builder().partitionValue(person.getUuid().toString()).build(), Person.class, "person");
				assertThat(person1).isEqualTo(null);
			}).doesNotThrowAnyException();
		}
	}


	@Test
	void query() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			DynamoDbTemplate dynamoDBTemplate = context.getBean(DynamoDbTemplate.class);
			Person person = Person.PersonBuilder.person().withName("foo").withLastName("bar").withUuid(UUID.randomUUID()).build();
			assertThatCode(() -> dynamoDBTemplate.save(person)).doesNotThrowAnyException();

			QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(person.getUuid().toString()).build())).build();

			PageIterable<Person> persons = dynamoDBTemplate.query(queryEnhancedRequest, Person.class, "person");
			//items size
			assertThat((int) persons.items().stream().count()).isEqualTo(1);
			//page size
			assertThat((int) persons.stream().count()).isEqualTo(1);
		}
	}


	public static void cleanUp(UUID uuid) {
		dynamoDbTable.deleteItem(Key.builder().partitionValue(uuid.toString()).build());
	}


	private ConfigurableApplicationContext runApplication(SpringApplication application) {
		return application.run(
			"--spring.cloud.aws.dynamodb.region=" + REGION,
			"--spring.cloud.aws.dynamodb.endpoint=" + localstack.getEndpointOverride(DYNAMODB).toString(),
			"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
			"--spring.cloud.aws.region.static=eu-west-1");
	}

	@SpringBootApplication
	static class App {

	}

}
