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

package org.springframework.cloud.aws.autoconfigure.context.properties;

/**
 * Properties related to S3 client behavior within the application
 * {@link org.springframework.core.io.ResourceLoader}.
 *
 * @author Tom Gianos
 * @since 2.0.2
 * @see org.springframework.cloud.aws.autoconfigure.context.ContextResourceLoaderAutoConfiguration
 */
public class AwsS3ResourceLoaderProperties {

	/**
	 * The core pool size of the Task Executor used for parallel S3 interaction.
	 *
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor#setCorePoolSize(int)
	 */
	private int corePoolSize = 1;

	/**
	 * The maximum pool size of the Task Executor used for parallel S3 interaction.
	 *
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor#setMaxPoolSize(int)
	 */
	private int maxPoolSize = Integer.MAX_VALUE;

	/**
	 * The maximum queue capacity for backed up S3 requests.
	 *
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor#setQueueCapacity(int)
	 */
	private int queueCapacity = Integer.MAX_VALUE;

	public int getCorePoolSize() {
		return this.corePoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaxPoolSize() {
		return this.maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public int getQueueCapacity() {
		return this.queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

}
