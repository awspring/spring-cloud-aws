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

import java.util.Objects;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

// Called PersonEntity so tableName can be resolved to person_entity. Used so it is not a plain tableName.
@DynamoDbBean
public class PersonEntity {

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
		PersonEntity personEntity = (PersonEntity) o;
		return Objects.equals(uuid, personEntity.uuid) && Objects.equals(name, personEntity.name)
				&& Objects.equals(lastName, personEntity.lastName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, name, lastName);
	}

	public static final class Builder {
		private UUID uuid;
		private String name;
		private String lastName;

		private Builder() {
		}

		public static Builder person() {
			return new Builder();
		}

		public Builder withUuid(UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public PersonEntity build() {
			PersonEntity personEntity = new PersonEntity();
			personEntity.setUuid(uuid);
			personEntity.setName(name);
			personEntity.setLastName(lastName);
			return personEntity;
		}
	}
}
