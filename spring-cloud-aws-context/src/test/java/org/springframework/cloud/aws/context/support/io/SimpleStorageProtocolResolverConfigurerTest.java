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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.generateBeanName;

/**
 *
 */
public class SimpleStorageProtocolResolverConfigurerTest {

    @Test
    public void postProcessBeans_beanWithFieldInjectedResourceLoader_receivesSimpleStorageResourceLoader() {
        //Arrange
        StaticApplicationContext staticApplicationContext = new StaticApplicationContext();

        configureApplicationContext(staticApplicationContext);

        staticApplicationContext.registerSingleton("client", FieldInjectionTarget.class);
        staticApplicationContext.refresh();

        //Act
        FieldInjectionTarget fieldInjectionTarget = staticApplicationContext.getBean(FieldInjectionTarget.class);

        //Assert
        assertNotNull(fieldInjectionTarget.getResourceLoader());
        assertTrue(DefaultResourceLoader.class.isInstance(fieldInjectionTarget.getResourceLoader()));

        DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) fieldInjectionTarget.getResourceLoader();
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(defaultResourceLoader.getProtocolResolvers().iterator().next()));
    }

    @Test
    public void postProcessBeans_beanWithMethodInjectedResourceLoader_receivesSimpleStorageResourceLoader() {
        //Arrange
        StaticApplicationContext staticApplicationContext = new StaticApplicationContext();

        configureApplicationContext(staticApplicationContext);

        staticApplicationContext.registerSingleton("client", MethodInjectionTarget.class);

        staticApplicationContext.refresh();

        //Act
        MethodInjectionTarget methodInjectionTarget = staticApplicationContext.getBean(MethodInjectionTarget.class);

        //Assert
        assertNotNull(methodInjectionTarget.getResourceLoader());
        assertTrue(DefaultResourceLoader.class.isInstance(methodInjectionTarget.getResourceLoader()));

        assertTrue(DefaultResourceLoader.class.isInstance(methodInjectionTarget.getResourceLoader()));

        DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) methodInjectionTarget.getResourceLoader();
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(defaultResourceLoader.getProtocolResolvers().iterator().next()));
    }

    @Test
    public void postProcessBeans_beanWithConstructorInjectedResourceLoader_receivesSimpleStorageResourceLoader() {
        //Arrange
        StaticApplicationContext staticApplicationContext = new StaticApplicationContext();

        configureApplicationContext(staticApplicationContext);

        staticApplicationContext.registerSingleton("client", ConstructorInjectionTarget.class);
        staticApplicationContext.refresh();

        //Act
        ConstructorInjectionTarget constructorInjectionTarget = staticApplicationContext.getBean(ConstructorInjectionTarget.class);

        //Assert
        assertNotNull(constructorInjectionTarget.getResourceLoader());
        assertTrue(DefaultResourceLoader.class.isInstance(constructorInjectionTarget.getResourceLoader()));

        assertTrue(DefaultResourceLoader.class.isInstance(constructorInjectionTarget.getResourceLoader()));

        DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) constructorInjectionTarget.getResourceLoader();
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(defaultResourceLoader.getProtocolResolvers().iterator().next()));
    }

    @Test
    public void postProcessBeans_beanWithResourceLoaderAwareInterface_receivesSimpleStorageResourceLoader() {
        StaticApplicationContext staticApplicationContext = new StaticApplicationContext();

        configureApplicationContext(staticApplicationContext);

        staticApplicationContext.registerSingleton("client", ResourceLoaderAwareBean.class);
        staticApplicationContext.refresh();

        ResourceLoaderAwareBean resourceLoaderAwareBean = staticApplicationContext.getBean(ResourceLoaderAwareBean.class);
        assertNotNull(resourceLoaderAwareBean.getResourceLoader());
        assertTrue(DefaultResourceLoader.class.isInstance(resourceLoaderAwareBean.getResourceLoader()));

        assertTrue(DefaultResourceLoader.class.isInstance(resourceLoaderAwareBean.getResourceLoader()));

        DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) resourceLoaderAwareBean.getResourceLoader();
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(defaultResourceLoader.getProtocolResolvers().iterator().next()));
    }

    private static void configureApplicationContext(StaticApplicationContext staticApplicationContext) {
        AmazonS3 amazonS3Mock = mock(AmazonS3.class);

        AnnotationConfigUtils.registerAnnotationConfigProcessors(staticApplicationContext);

        BeanDefinitionBuilder loader = BeanDefinitionBuilder.genericBeanDefinition(SimpleStorageProtocolResolver.class);
        loader.addConstructorArgValue(amazonS3Mock);

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SimpleStorageProtocolResolverConfigurer.class);
        builder.addConstructorArgValue(loader.getBeanDefinition());
        AbstractBeanDefinition definition = builder.getBeanDefinition();

        staticApplicationContext.registerBeanDefinition(generateBeanName(definition, staticApplicationContext), definition);
    }

    private static final class FieldInjectionTarget {

        @SuppressWarnings("SpringJavaAutowiringInspection")
        @Autowired
        private ResourceLoader resourceLoader;

        public ResourceLoader getResourceLoader() {
            return this.resourceLoader;
        }
    }

    private static final class MethodInjectionTarget {

        private ResourceLoader resourceLoader;

        public ResourceLoader getResourceLoader() {
            return this.resourceLoader;
        }

        @Autowired
        public void setResourceLoader(@SuppressWarnings("SpringJavaAutowiringInspection") ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }
    }

    private static final class ConstructorInjectionTarget {

        private final ResourceLoader resourceLoader;

        @Autowired
        private ConstructorInjectionTarget(@SuppressWarnings("SpringJavaAutowiringInspection") ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        public ResourceLoader getResourceLoader() {
            return this.resourceLoader;
        }
    }


    private static final class ResourceLoaderAwareBean implements ResourceLoaderAware {

        private ResourceLoader resourceLoader;

        public ResourceLoader getResourceLoader() {
            return this.resourceLoader;
        }

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }
    }
}
