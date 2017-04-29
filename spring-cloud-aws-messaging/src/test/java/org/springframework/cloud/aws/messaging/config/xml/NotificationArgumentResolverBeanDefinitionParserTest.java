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

package org.springframework.cloud.aws.messaging.config.xml;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.getBeanName;

public class NotificationArgumentResolverBeanDefinitionParserTest {

    @Test
    public void parseInternal_minimalConfiguration_configuresHandlerMethodArgumentResolverWithAmazonSnsImplicitlyConfigured() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-minimal.xml", getClass());

        //Act
        HandlerMethodArgumentResolver argumentResolver = context.getBean(HandlerMethodArgumentResolver.class);

        //Assert
        assertNotNull(argumentResolver);
        assertTrue(context.containsBean(getBeanName(AmazonSNSClient.class.getName())));
    }

    @Test
    public void parseInternal_customRegion_configuresHandlerMethodArgumentResolverWithAmazonSnsImplicitlyConfiguredAndCustomRegionSet() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRegion.xml", getClass());

        //Act
        AmazonSNSClient snsClient = context.getBean(AmazonSNSClient.class);

        //Assert
        assertEquals(new URI("https", Region.getRegion(Regions.EU_WEST_1).getServiceEndpoint("sns"), null, null), ReflectionTestUtils.getField(snsClient, "endpoint"));
    }

    @Test
    public void parseInternal_customRegionProvider_configuresHandlerMethodArgumentResolverWithAmazonSnsImplicitlyConfiguredAndCustomRegionSet() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRegionProvider.xml", getClass());

        //Act
        AmazonSNSClient snsClient = context.getBean(AmazonSNSClient.class);

        //Assert
        assertEquals(new URI("https", Region.getRegion(Regions.US_WEST_2).getServiceEndpoint("sns"), null, null), ReflectionTestUtils.getField(snsClient, "endpoint"));
    }

    @Test
    public void parseInternal_customSnsClient_configuresHandlerMethodArgumentResolverWithCustomSnsClient() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customSnsClient.xml", getClass());

        //Act
        AmazonSNSClient snsClient = context.getBean("customSnsClient", AmazonSNSClient.class);

        //Assert
        assertNotNull(snsClient);
        assertFalse(context.containsBean(getBeanName(AmazonSNSClient.class.getName())));
    }
}
