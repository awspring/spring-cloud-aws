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

package org.springframework.cloud.aws.context.support.io;

import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;

/**
 * {@link BeanPostProcessor} and {@link BeanFactoryPostProcessor} implementation that allows classes to receive
 * a specialized {@link ResourceLoader} that can handle S3 resources with the {@link ResourceLoaderAware} interface
 * or through injecting the resource loader.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class ResourceLoaderBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor, Ordered, ResourceLoaderAware {

    private final AmazonS3 amazonS3;
    private ResourceLoader resourceLoader;
    private TaskExecutor executor;

    public ResourceLoaderBeanPostProcessor(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.executor = taskExecutor;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ResourceLoaderAware) {
            ((ResourceLoaderAware) bean).setResourceLoader(this.resourceLoader);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SimpleStorageResourceLoader simpleStorageResourceLoader = new SimpleStorageResourceLoader(this.amazonS3, this.resourceLoader);
        if (this.executor != null) {
            simpleStorageResourceLoader.setTaskExecutor(this.executor);
        }

        try {
            simpleStorageResourceLoader.afterPropertiesSet();
        } catch (Exception e) {
            throw new BeanInstantiationException(SimpleStorageResourceLoader.class, "Error instantiating class", e);
        }

        this.resourceLoader = new PathMatchingSimpleStorageResourcePatternResolver(this.amazonS3,
                simpleStorageResourceLoader, (ResourcePatternResolver) this.resourceLoader);


        beanFactory.registerResolvableDependency(ResourceLoader.class, this.resourceLoader);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
