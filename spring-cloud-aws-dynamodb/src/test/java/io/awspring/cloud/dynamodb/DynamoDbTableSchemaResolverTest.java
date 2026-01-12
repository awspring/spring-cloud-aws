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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

/**
 * Tests for {@link DefaultDynamoDbTableSchemaResolver}.
 *
 * @author Matej Nedic
 * @author Maciej Walkowiak
 * @author Marcus Voltolim
 */
class DynamoDbTableSchemaResolverTest {

	@Test
	void tableSchemaResolved_successfully() {
		// given
		DefaultDynamoDbTableSchemaResolver defaultTableSchemaResolver = new DefaultDynamoDbTableSchemaResolver();
		// when
		TableSchema<Person> tableSchema = defaultTableSchemaResolver.resolve(Person.class);

		// Call one more time to see if cache is being filled properly.
		defaultTableSchemaResolver.resolve(Person.class);

		// then
		assertThat(tableSchema).isNotNull();
		assertThat(defaultTableSchemaResolver.getTableSchemaCache()).hasSize(1);
	}

	@Test
	void tableSchemaForImmutableResolved_successfully() {
		// given
		DefaultDynamoDbTableSchemaResolver defaultTableSchemaResolver = new DefaultDynamoDbTableSchemaResolver();
		// when
		TableSchema<PersonImmutable> tableSchema = defaultTableSchemaResolver.resolve(PersonImmutable.class);

		// Call one more time to see if cache is being filled properly.
		defaultTableSchemaResolver.resolve(PersonImmutable.class);

		// then
		assertThat(tableSchema).isNotNull();
		assertThat(defaultTableSchemaResolver.getTableSchemaCache()).hasSize(1);
	}

	@Test
	void tableSchemaResolved_whenSchemaPassedThroughConstructor() {
		StaticTableSchema<Library> librarySchema = StaticTableSchema.builder(Library.class).build();
		DefaultDynamoDbTableSchemaResolver defaultTableSchemaResolver = new DefaultDynamoDbTableSchemaResolver(
				List.of(librarySchema));

		// when
		TableSchema<Library> tableSchema = defaultTableSchemaResolver.resolve(Library.class);

		// then
		assertThat(tableSchema).isNotNull().isEqualTo(librarySchema);
		assertThat(defaultTableSchemaResolver.getTableSchemaCache()).hasSize(1);
	}

	@Test
	void tableSchemaResolved_successfully_for_multiple_tables() {
		// given
		DefaultDynamoDbTableSchemaResolver defaultTableSchemaResolver = new DefaultDynamoDbTableSchemaResolver();

		// when
		TableSchema<Person> person = defaultTableSchemaResolver.resolve(Person.class);
		TableSchema<Book> bookTableSchema = defaultTableSchemaResolver.resolve(Book.class);

		// then
		assertThat(person).isNotNull();
		assertThat(bookTableSchema).isNotNull();
		assertThat(defaultTableSchemaResolver.getTableSchemaCache()).hasSize(2);
	}

	@Test
	void tableSchemaResolver_fail_entity_not_annotated() {
		DefaultDynamoDbTableSchemaResolver defaultTableSchemaResolver = new DefaultDynamoDbTableSchemaResolver();

		assertThatThrownBy(() -> defaultTableSchemaResolver.resolve(FakePerson.class))
				.isInstanceOf(IllegalArgumentException.class);

	}

	static class Library {
	}

}
