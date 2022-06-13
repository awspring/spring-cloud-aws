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

import java.util.Objects;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

// Called MoreComplexPerson so tableName can be resolved to more_complex_person. Used so it is not a plain tableName.
@DynamoDbBean
public class MoreComplexPerson {

	private UUID uuid;
	private String name;
	private String lastName;

	@DynamoDbPartitionKey
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MoreComplexPerson moreComplexPerson = (MoreComplexPerson) o;
		return Objects.equals(uuid, moreComplexPerson.uuid) && Objects.equals(name, moreComplexPerson.name)
				&& Objects.equals(lastName, moreComplexPerson.lastName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, name, lastName);
	}

	public static final class MoreComplexPersonBuilder {
		private UUID uuid;
		private String name;
		private String lastName;

		private MoreComplexPersonBuilder() {
		}

		public static MoreComplexPersonBuilder person() {
			return new MoreComplexPersonBuilder();
		}

		public MoreComplexPersonBuilder withUuid(UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		public MoreComplexPersonBuilder withName(String name) {
			this.name = name;
			return this;
		}

		public MoreComplexPersonBuilder withLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public MoreComplexPerson build() {
			MoreComplexPerson moreComplexPerson = new MoreComplexPerson();
			moreComplexPerson.setUuid(uuid);
			moreComplexPerson.setName(name);
			moreComplexPerson.setLastName(lastName);
			return moreComplexPerson;
		}
	}
}
