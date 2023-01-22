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
package io.awspring.cloud.s3;

import java.io.InputStream;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Converter used to serialize Java objects into S3 objects.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public interface S3ObjectConverter {
	/**
	 * Converts object into a {@link RequestBody}.
	 *
	 * @param object - the object to serialize
	 * @param <T> - type of the object
	 * @return the request body
	 */
	<T> RequestBody write(T object);

	/**
	 * Reads S3 object from the input stream into a Java object.
	 * @param is - the input stream
	 * @param clazz - the class of the object
	 * @param <T> - the the type of the object
	 * @return deserialized object
	 */
	<T> T read(InputStream is, Class<T> clazz);

	/**
	 * Supported content type.
	 *
	 * @return the content type
	 */
	String contentType();
}
