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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class DynamoDbTableSchemaResolverTest {

	DefaultDynamoDbTableSchemaResolver defaultTableSchemaResolver = new DefaultDynamoDbTableSchemaResolver();

	@Test
	void tableSchemaResolved_successfully() {
		// given when
		TableSchema<Person> tableSchema = defaultTableSchemaResolver.resolve(Person.class, "person");

		// Call one more time to see if cache is being filled properly.
		defaultTableSchemaResolver.resolve(Person.class, "person");

		// then
		assertThat(((Map<String, TableSchema>) ReflectionTestUtils.getField(defaultTableSchemaResolver,
				"tableSchemaCache")).size()).isEqualTo(1);
		assertThat(tableSchema).isNotNull();
	}

	@Test
	void tableSchemaResolved_successfully_for_multiple_tables() {
		// given when
		TableSchema<Person> person = defaultTableSchemaResolver.resolve(Person.class, "person");
		TableSchema<MoreComplexPerson> moreComplexPersonTableSchema = defaultTableSchemaResolver
				.resolve(MoreComplexPerson.class, "more_complex_person");

		// then
		assertThat(((Map<String, TableSchema>) ReflectionTestUtils.getField(defaultTableSchemaResolver,
				"tableSchemaCache")).size()).isEqualTo(2);
		assertThat(person).isNotNull();
		assertThat(moreComplexPersonTableSchema).isNotNull();
	}

	@Test
	void tableSchemaResolver_fail_entity_not_annotated() {
		assertThatThrownBy(() -> defaultTableSchemaResolver.resolve(FakePerson.class, "fake_person"));

	}
}
