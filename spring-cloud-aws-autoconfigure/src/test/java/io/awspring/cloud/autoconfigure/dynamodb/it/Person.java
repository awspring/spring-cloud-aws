package io.awspring.cloud.autoconfigure.dynamodb.it;

import io.awspring.cloud.dynamodb.TableNameProvider;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Objects;
import java.util.UUID;

@DynamoDbBean
public class Person implements TableNameProvider {

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
	public String getTableName() {
		return "person";
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Person person = (Person) o;
		return Objects.equals(uuid, person.uuid) && Objects.equals(name, person.name) && Objects.equals(lastName, person.lastName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, name, lastName);
	}

	public static final class PersonBuilder {
		private UUID uuid;
		private String name;
		private String lastName;

		private PersonBuilder() {
		}

		public static PersonBuilder person() {
			return new PersonBuilder();
		}

		public PersonBuilder withUuid(UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		public PersonBuilder withName(String name) {
			this.name = name;
			return this;
		}

		public PersonBuilder withLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public Person build() {
			Person person = new Person();
			person.setUuid(uuid);
			person.setName(name);
			person.setLastName(lastName);
			return person;
		}
	}
}
