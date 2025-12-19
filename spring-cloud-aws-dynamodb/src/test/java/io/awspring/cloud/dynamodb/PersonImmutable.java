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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

/**
 * Immutable version of {@link Person}.
 *
 * @author Marcus Voltolim
 */
@DynamoDbImmutable(builder = PersonImmutable.Builder.class)
public class PersonImmutable extends Person {

	private PersonImmutable(String firstName, String lastName) {
		super(firstName, lastName);
	}

	public static class Builder {

		private String firstName;
		private String lastName;

		public Builder firstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		public Builder lastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public PersonImmutable build() {
			return new PersonImmutable(firstName, lastName);
		}

	}

}
