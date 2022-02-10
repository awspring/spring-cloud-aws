/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.cloud.v3.messaging.support.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.codec.EncodingException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.MimeType;

/**
 * @author Agim Emruli
 */
public class ObjectMessageConverter extends AbstractMessageConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMessageConverter.class);

	private static final String DEFAULT_ENCODING = "UTF-8";

	private final Charset encoding;

	public ObjectMessageConverter(String encoding) {
		super(new MimeType("application", "x-java-serialized-object", Charset.forName(encoding)));
		this.encoding = Charset.forName(encoding);
	}

	public ObjectMessageConverter() {
		this(DEFAULT_ENCODING);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		String messagePayload = message.getPayload().toString();
		byte[] decodedBase64Content = decodeBase64(messagePayload);

		Serializable result;
		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBase64Content);
			ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
			result = (Serializable) objectInputStream.readObject();
		} catch (ClassNotFoundException e) {
			throw new MessageConversionException(
					"Error loading class from message payload, make sure class is in classpath!", e);
		} catch (IOException e) {
			throw new MessageConversionException("Error reading payload from binary representation", e);
		}

		return result;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		if (!(payload instanceof Serializable)) {
			throw new IllegalArgumentException("Can't convert payload, it must be of type Serializable");
		}

		try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
			objectOutputStream.writeObject(payload);
			objectOutputStream.flush();
			byteArrayOutputStream.flush();
			return new String(Base64.getEncoder().encode(byteArrayOutputStream.toByteArray()), this.encoding);
		}
		catch (IOException e) {
			throw new MessageConversionException("Error converting payload into binary representation", e);
		}
	}

	private byte[] decodeBase64(final String payload) {
		try {
			return Base64.getDecoder().decode(payload.getBytes(this.encoding));
		} catch (IllegalArgumentException e) {
			throw new MessageConversionException("Error converting payload '" + payload
				+ "' because it is not a valid base64 encoded stream!", e);
		}
	}

}
