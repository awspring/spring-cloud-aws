/*
 * Copyright 2013-2014 the original author or authors.
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

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
class SimpleStorageResourceLoader implements ResourceLoader {


	private final AmazonS3 amazonS3;
	private final ResourceLoader delegate;

	SimpleStorageResourceLoader(AmazonS3 amazonS3, ClassLoader classLoader) {
		this.amazonS3 = amazonS3;
		this.delegate = new DefaultResourceLoader(classLoader);
	}

	SimpleStorageResourceLoader(AmazonS3 amazonS3) {
		this(amazonS3, ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Resource getResource(String location) {
		if (SimpleStorageNameUtils.isSimpleStorageResource(location)) {
			return new SimpleStorageResource(this.amazonS3, SimpleStorageNameUtils.getBucketNameFromLocation(location),
					SimpleStorageNameUtils.getObjectNameFromLocation(location));
		}

		return this.delegate.getResource(location);
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.delegate.getClassLoader();
	}
}