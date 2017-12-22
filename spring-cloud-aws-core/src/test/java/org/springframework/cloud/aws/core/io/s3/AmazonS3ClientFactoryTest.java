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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Agim Emruli
 */
public class AmazonS3ClientFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createClientForEndpointUrl_withNullEndpoint_throwsIllegalArgumentException() {
        // Arrange
        AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();

        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("Endpoint Url must not be null");

        // Act
        amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, null);

        // Prepare

    }

    @Test
    public void createClientForEndpointUrl_withNullAmazonS3Client_throwsIllegalArgumentException() {
        // Arrange
        AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();

        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("AmazonS3 must not be null");

        // Act
        amazonS3ClientFactory.createClientForEndpointUrl(null, "http://s3.amazonaws.com");

        // Prepare

    }

    @Test
    public void createClientForEndpointUrl_withDefaultRegionUrl_createClientForDefaultRegion() {
        // Arrange
        AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();

        // Act
        AmazonS3 newClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, "http://s3.amazonaws.com");

        // Prepare
        Assert.assertEquals(Regions.DEFAULT_REGION.getName(), newClient.getRegionName());
    }


    @Test
    public void createClientForEndpointUrl_withCustomRegionUrl_createClientForCustomRegion() {
        // Arrange
        AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();

        // Act
        AmazonS3 newClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, "https://myBucket.s3.eu-central-1.amazonaws.com");

        // Prepare
        Assert.assertEquals(Regions.EU_CENTRAL_1.getName(), newClient.getRegionName());
    }

    @Test
    public void createClientForEndpointUrl_withProxiedClient_createClientForCustomRegion() {
        // Arrange
        AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
        AmazonS3 amazonS3 = AmazonS3ProxyFactory.createProxy(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build());


        // Act
        AmazonS3 newClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, "https://myBucket.s3.eu-central-1.amazonaws.com");

        // Prepare
        Assert.assertEquals(Regions.EU_CENTRAL_1.getName(), newClient.getRegionName());
    }

    @Test
    public void createClientForEndpointUrl_withCustomRegionUrlAndCachedClient_returnsCachedClient() {
        // Arrange
        AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();

        AmazonS3 existingClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, "https://myBucket.s3.eu-central-1.amazonaws.com");

        // Act
        AmazonS3 cachedClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, "https://myBucket.s3.eu-central-1.amazonaws.com");

        // Prepare
        Assert.assertSame(cachedClient, existingClient);
    }
}