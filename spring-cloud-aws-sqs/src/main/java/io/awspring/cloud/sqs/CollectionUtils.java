/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.sqs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CollectionUtils {

	public static <T> List<Collection<T>> partition(Collection<T> messagesToAck, int pageSize) {
		List<T> messagesToUse = getAsList(messagesToAck);
		int totalSize = messagesToUse.size();
		return IntStream.rangeClosed(0, (totalSize - 1) / pageSize)
				.mapToObj(index -> messagesToUse.subList(index * pageSize, Math.min((index + 1) * pageSize, totalSize)))
				.collect(Collectors.toList());
	}

	private static <T> List<T> getAsList(Collection<T> elements) {
		return elements instanceof List ? (List<T>) elements : new ArrayList<>(elements);
	}

}
