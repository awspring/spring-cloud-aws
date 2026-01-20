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

import org.jspecify.annotations.Nullable;

/**
 * Resolves content type of S3 objects.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public interface S3ObjectContentTypeResolver {
	/**
	 * Resolves content type from a file name.
	 *
	 * @param fileName - the file name
	 * @return content type or null if not resolved
	 */
	@Nullable
	String resolveContentType(String fileName);
}
