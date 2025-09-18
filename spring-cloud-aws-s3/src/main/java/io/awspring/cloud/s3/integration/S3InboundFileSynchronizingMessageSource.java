/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.s3.integration;

import java.io.File;
import java.util.Comparator;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation for the Amazon S3.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class S3InboundFileSynchronizingMessageSource extends AbstractInboundFileSynchronizingMessageSource<S3Object> {

	public S3InboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<S3Object> synchronizer) {
		super(synchronizer);
	}

	public S3InboundFileSynchronizingMessageSource(AbstractInboundFileSynchronizer<S3Object> synchronizer,
			Comparator<File> comparator) {

		super(synchronizer, comparator);
	}

	public String getComponentType() {
		return "aws:s3-inbound-channel-adapter";
	}

}
