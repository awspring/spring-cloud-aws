package io.awspring.cloud.sqs.sample;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Person {

	private String firstName;

	private String lastName;

	public Person(@JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public String toString() {
		return "Person{" + "firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + '}';
	}

}
