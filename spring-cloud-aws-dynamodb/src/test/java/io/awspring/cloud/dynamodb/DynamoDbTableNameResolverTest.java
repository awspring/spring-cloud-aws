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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DynamoDbTableNameResolver}.
 *
 * @author Matej Nedic
 */
class DynamoDbTableNameResolverTest {

	private static final DefaultDynamoDbTableNameResolver tableNameResolver = new DefaultDynamoDbTableNameResolver();

	@Test
	void resolveTableNameSuccessfully() {
		assertThat(tableNameResolver.resolve(MoreComplexPerson.class)).isEqualTo("more_complex_person");
		assertThat(tableNameResolver.resolve(Person.class)).isEqualTo("person");
	}

	@Test
	void resolvesTableNameFromRecord() {
		assertThat(tableNameResolver.resolve(PersonRecord.class)).isEqualTo("person_record");
	}

	record PersonRecord(String name) {
	}

	private static class Person {
	}

	private static class MoreComplexPerson {
	}
}
