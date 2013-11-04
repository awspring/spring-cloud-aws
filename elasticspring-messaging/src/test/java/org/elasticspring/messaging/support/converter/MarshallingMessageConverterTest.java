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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.xstream.XStreamMarshaller;

import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 */
public class MarshallingMessageConverterTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testSerializeBean() throws Exception {
		XStreamMarshaller xStreamMarshaller = new XStreamMarshaller();
		xStreamMarshaller.setStreamDriver(new JettisonMappedXmlDriver());
		xStreamMarshaller.setMode(XStream.NO_REFERENCES);

		TestPerson testPerson = new TestPerson("Agim", "Emruli", new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).parse("1984-12-18"));
		MessageConverter messageConverter = new MarshallingMessageConverter(xStreamMarshaller, xStreamMarshaller);
		Message<?> result = messageConverter.toMessage(testPerson, null);

		TestPerson candidate = (TestPerson) messageConverter.fromMessage(result,TestPerson.class);
		Assert.assertEquals(testPerson, candidate);
	}

	@Test
	public void testWrongCharset() throws Exception {
		this.expectedException.expect(UnsupportedCharsetException.class);
		Marshaller marshaller = Mockito.mock(Marshaller.class);
		Unmarshaller unmarshaller = Mockito.mock(Unmarshaller.class);
		//noinspection ResultOfObjectAllocationIgnored
		new MarshallingMessageConverter(marshaller, unmarshaller, "FOO");
	}

	private static class TestPerson {

		private final String firstName;
		private final String lastName;
		private final Date birthdate;

		private TestPerson(String firstName, String lastName, Date birthdate) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.birthdate = birthdate;
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

			if (this.getBirthdate() != null ? !this.getBirthdate().equals(that.getBirthdate()) : that.getBirthdate() != null) {
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
			result = 31 * result + (this.getBirthdate() != null ? this.getBirthdate().hashCode() : 0);
			return result;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public Date getBirthdate() {
			return this.birthdate;
		}
	}
}
