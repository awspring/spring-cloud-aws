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

package org.springframework.cloud.aws.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ClassUtils;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageResourceLoader implements ResourceLoader, InitializingBean {

    private final AmazonS3 amazonS3;
    private final ResourceLoader delegate;

    /**
     * <b>IMPORTANT:</b> If a task executor is set with an unbounded queue there will be a huge memory consumption. The
     * reason is that each multipart of 5MB will be put in the queue to be uploaded. Therefore a bounded queue is recommended.
     */
    private TaskExecutor taskExecutor;

    public SimpleStorageResourceLoader(AmazonS3 amazonS3, ResourceLoader delegate) {
        this.amazonS3 = AmazonS3ProxyFactory.createProxy(amazonS3);
        this.delegate = delegate;
    }

    public SimpleStorageResourceLoader(AmazonS3 amazonS3, ClassLoader classLoader) {
        this.amazonS3 = AmazonS3ProxyFactory.createProxy(amazonS3);
        this.delegate = new DefaultResourceLoader(classLoader);
    }

    public SimpleStorageResourceLoader(AmazonS3 amazonS3) {
        this(amazonS3, ClassUtils.getDefaultClassLoader());
    }

    @RuntimeUse
    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.taskExecutor == null) {
            this.taskExecutor = new SyncTaskExecutor();
        }
    }

    @Override
    public Resource getResource(String location) {
        if (SimpleStorageNameUtils.isSimpleStorageResource(location)) {
            return new SimpleStorageResource(this.amazonS3, SimpleStorageNameUtils.getBucketNameFromLocation(location),
                    SimpleStorageNameUtils.getObjectNameFromLocation(location), this.taskExecutor,
                    SimpleStorageNameUtils.getVersionIdFromLocation(location));
        }

        return this.delegate.getResource(location);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.delegate.getClassLoader();
    }
}
