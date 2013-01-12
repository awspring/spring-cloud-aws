/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class SimpleStorageResourceLoader implements ResourceLoader {


	private final AmazonS3 amazonS3;
	private final ResourceLoader delegate;
	private static final Pattern S3_LOCATION_PATTERN = Pattern.compile("^s3://([A-Za-z0-9\\.\\-]*)/([A-Za-z0-9\\.\\-]*)/?$");
	private static final String S3_PROTOCOL_PREFIX = "s3://";

	public SimpleStorageResourceLoader(AmazonS3 amazonS3, ClassLoader classLoader) {
		this.amazonS3 = amazonS3;
		this.delegate = new DefaultResourceLoader(classLoader);
	}

	public SimpleStorageResourceLoader(AmazonS3 amazonS3) {
		this(amazonS3, ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Resource getResource(String location) {
		if (location.startsWith(S3_PROTOCOL_PREFIX)) {
			Matcher matcher = S3_LOCATION_PATTERN.matcher(location);
			if (matcher.matches()) {
				return new SimpleStorageResource(this.amazonS3, getBucketNameFromUri(matcher), getObjectNameFromUri(matcher));
			} else {
				throw new IllegalArgumentException(String.format("The s3 location '%s' is not a valid s3 location!", location));
			}
		}

		return this.delegate.getResource(location);
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.delegate.getClassLoader();
	}

	private String getBucketNameFromUri(Matcher matcher) {
		return matcher.group(1);
	}

	private String getObjectNameFromUri(Matcher matcher) {
		return matcher.group(2);
	}
}