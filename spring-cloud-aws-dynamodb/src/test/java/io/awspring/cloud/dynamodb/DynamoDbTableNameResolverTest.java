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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DynamoDbTableNameResolver}.
 *
 * @author Matej Nedic
 * @author Arun Patra
 * @author Volodymyr Ivakhnenko
 */
class DynamoDbTableNameResolverTest {

	private final DefaultDynamoDbTableNameResolver tableNameResolver = new DefaultDynamoDbTableNameResolver();

	private final DefaultDynamoDbTableNameResolver prefixedTableNameResolver = new DefaultDynamoDbTableNameResolver(
			"my_prefix_");

	private final DefaultDynamoDbTableNameResolver prefixedAndSuffixedTableNameResolver = new DefaultDynamoDbTableNameResolver(
			"my_prefix_", "_my_suffix");

	private final DefaultDynamoDbTableNameResolver prefixedAndSuffixedTableAndSeparatorTableNameResolver = new DefaultDynamoDbTableNameResolver(
			"prefix-", "-suffix", "-");

	@Test
	void resolveTableNameSuccessfully() {
		assertThat(tableNameResolver.resolve(MoreComplexPerson.class)).isEqualTo("more_complex_person");
		assertThat(tableNameResolver.resolve(Person.class)).isEqualTo("person");
		assertThat(tableNameResolver.getTableNameCache()).hasSize(2);
	}

	@Test
	void resolvePrefixedTableNameSuccessfully() {
		assertThat(prefixedTableNameResolver.resolve(MoreComplexPerson.class))
				.isEqualTo("my_prefix_more_complex_person");
		assertThat(prefixedTableNameResolver.resolve(Person.class)).isEqualTo("my_prefix_person");
		assertThat(prefixedTableNameResolver.getTableNameCache()).hasSize(2);
	}

	@Test
	void resolvePrefixedAndSuffixedTableNameSuccessfully() {
		assertThat(prefixedAndSuffixedTableNameResolver.resolve(MoreComplexPerson.class))
				.isEqualTo("my_prefix_more_complex_person_my_suffix");
		assertThat(prefixedAndSuffixedTableNameResolver.resolve(Person.class)).isEqualTo("my_prefix_person_my_suffix");
		assertThat(prefixedAndSuffixedTableNameResolver.getTableNameCache()).hasSize(2);
	}

	@Test
	void resolvePrefixedAndSuffixedAndSeparatorTableNameSuccessfully() {
		assertThat(prefixedAndSuffixedTableAndSeparatorTableNameResolver.resolve(MoreComplexPerson.class))
				.isEqualTo("prefix-more-complex-person-suffix");
		assertThat(prefixedAndSuffixedTableAndSeparatorTableNameResolver.resolve(Person.class))
				.isEqualTo("prefix-person-suffix");
		assertThat(prefixedAndSuffixedTableAndSeparatorTableNameResolver.getTableNameCache()).hasSize(2);
	}

	@Test
	void resolvesTableNameFromRecord() {
		assertThat(tableNameResolver.resolve(PersonRecord.class)).isEqualTo("person_record");
		assertThat(tableNameResolver.getTableNameCache()).hasSize(1);
	}

	@Test
	void resolvesPrefixedTableNameFromRecord() {
		assertThat(prefixedTableNameResolver.resolve(PersonRecord.class)).isEqualTo("my_prefix_person_record");
		assertThat(prefixedTableNameResolver.getTableNameCache()).hasSize(1);
	}

	@Test
	void resolvePrefixedAndSuffixedAndSeparatorTableNameFromRecord() {
		assertThat(prefixedAndSuffixedTableAndSeparatorTableNameResolver.resolve(PersonRecord.class))
				.isEqualTo("prefix-person-record-suffix");
		assertThat(prefixedAndSuffixedTableAndSeparatorTableNameResolver.getTableNameCache()).hasSize(1);
	}

	@Test
	void resolveSameTableNameSuccessfully() {
		assertThat(tableNameResolver.getTableNameCache()).isEmpty();
		assertThat(tableNameResolver.resolve(MoreComplexPerson.class)).isEqualTo("more_complex_person");
		assertThat(tableNameResolver.resolve(MoreComplexPerson.class)).isEqualTo("more_complex_person");
		assertThat(tableNameResolver.resolve(MoreComplexPerson.class)).isEqualTo("more_complex_person");
		assertThat(tableNameResolver.getTableNameCache()).hasSize(1);
	}

	record PersonRecord(String name) {
	}

	private static class Person {
	}

	private static class MoreComplexPerson {
	}

}
