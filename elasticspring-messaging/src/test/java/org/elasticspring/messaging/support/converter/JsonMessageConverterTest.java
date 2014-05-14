/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support.converter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertNull;

public class JsonMessageConverterTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testSerializeBean() throws Exception {
		TestPerson testPerson = new TestPerson("Agim", "Emruli", new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).parse("1984-12-18"));
		MessageConverter messageConverter = new JsonMessageConverter();
		Message<?> result = messageConverter.toMessage(testPerson, null);

		TestPerson candidate = (TestPerson) messageConverter.fromMessage(result, TestPerson.class);
		Assert.assertEquals(testPerson, candidate);
	}

	@Test
	public void toMessage_withNullPayload_shouldReturnNull() throws Exception {
		// Arrange
		JsonMessageConverter messageConverter = new JsonMessageConverter();

		// Act
		Message<String> result = messageConverter.toMessage(null, null);

		// Assert
		assertNull(result);
	}

	@Test
	public void toMessage_withInvalidPayload_shouldReturnNull() throws Exception {
		// Arrange
		JsonMessageConverter messageConverter = new JsonMessageConverter();

		// Act
		Message<String> result = messageConverter.toMessage(new ByteArrayInputStream(new byte[1024]), null);

		// Assert
		assertNull(result);
	}

	@Test
	public void fromMessage_withNullPayload_shouldReturnNull() throws Exception {
		// Arrange
		JsonMessageConverter messageConverter = new JsonMessageConverter();

		// Act
		Object result = messageConverter.fromMessage(null, null);

		// Assert
		assertNull(result);
	}

	@Test
	public void fromMessage_withInvalidPayload_shouldReturnNull() throws Exception {
		// Arrange
		JsonMessageConverter messageConverter = new JsonMessageConverter();

		// Act
		Object result = messageConverter.fromMessage(MessageBuilder.withPayload("json{]").build(), InputStream.class);

		// Assert
		assertNull(result);
	}

	public static class TestPerson {

		private final String firstName;
		private final String lastName;
		private final Date birthDate;

		@JsonCreator
		public TestPerson(@JsonProperty("firstName") String firstName,
						  @JsonProperty("lastName")String lastName,
						  @JsonProperty("birthDate") Date birthDate) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.birthDate = birthDate;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}

			TestPerson that = (TestPerson) obj;

			if (this.getBirthDate() != null ? !this.getBirthDate().equals(that.getBirthDate()) : that.getBirthDate() != null) {
				return false;
			}
			if (this.getFirstName() != null ? !this.getFirstName().equals(that.getFirstName()) : that.getFirstName() != null) {
				return false;
			}
			return !(this.getLastName() != null ? !this.getLastName().equals(that.getLastName()) : that.getLastName() != null);

		}

		@Override
		public int hashCode() {
			int result = this.getFirstName() != null ? this.getFirstName().hashCode() : 0;
			result = 31 * result + (this.getLastName() != null ? this.getLastName().hashCode() : 0);
			result = 31 * result + (this.getBirthDate() != null ? this.getBirthDate().hashCode() : 0);
			return result;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public Date getBirthDate() {
			return this.birthDate;
		}
	}
}
