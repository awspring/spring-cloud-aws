/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageProtocolResolver implements ProtocolResolver, InitializingBean, BeanFactoryAware {

	private AmazonS3 amazonS3;

	/**
	 * <b>IMPORTANT:</b> If a task executor is set with an unbounded queue there will be a
	 * huge memory consumption. The reason is that each multipart of 5MB will be put in
	 * the queue to be uploaded. Therefore a bounded queue is recommended.
	 */
	private TaskExecutor taskExecutor;

	private BeanFactory beanFactory;

	public SimpleStorageProtocolResolver() {
	}

	public SimpleStorageProtocolResolver(AmazonS3 amazonS3) {
		this.amazonS3 = AmazonS3ProxyFactory.createProxy(amazonS3);
	}

	@RuntimeUse
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.taskExecutor == null) {
			this.taskExecutor = new SyncTaskExecutor();
		}
	}

	@Override
	public Resource resolve(String location, ResourceLoader resourceLoader) {
		if (SimpleStorageNameUtils.isSimpleStorageResource(location)) {
			return new SimpleStorageResource(this.getAmazonS3(),
					SimpleStorageNameUtils.getBucketNameFromLocation(location),
					SimpleStorageNameUtils.getObjectNameFromLocation(location), this.taskExecutor,
					SimpleStorageNameUtils.getVersionIdFromLocation(location),
					SimpleStorageNameUtils.getContentTypeFromLocation(location));
		}
		else {
			return null;
		}
	}

	public AmazonS3 getAmazonS3() {
		if (this.amazonS3 == null) {
			this.amazonS3 = AmazonS3ProxyFactory.createProxy(this.beanFactory.getBean(AmazonS3.class));
		}
		return this.amazonS3;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
