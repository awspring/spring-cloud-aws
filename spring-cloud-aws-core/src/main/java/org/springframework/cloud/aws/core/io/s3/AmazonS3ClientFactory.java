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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AmazonS3} client factory that create clients for other regions based on the source client and a endpoint url.
 * Caches clients per region to enable re-use on a region base.
 *
 * @author Agim Emruli
 * @since 1.2
 */
public class AmazonS3ClientFactory {

    private static final String CREDENTIALS_PROVIDER_FIELD_NAME = "awsCredentialsProvider";
    private final ConcurrentHashMap<String, AmazonS3> clientCache = new ConcurrentHashMap<>(Regions.values().length);
    private final Field credentialsProviderField;

    public AmazonS3ClientFactory() {
        this.credentialsProviderField = ReflectionUtils.findField(AmazonS3Client.class, CREDENTIALS_PROVIDER_FIELD_NAME);
        Assert.notNull(this.credentialsProviderField, "Credentials Provider field not found, this class does not work with the current " +
                "AWS SDK release");
        ReflectionUtils.makeAccessible(this.credentialsProviderField);
    }

    public AmazonS3 createClientForEndpointUrl(AmazonS3 prototype, String endpointUrl) {
        Assert.notNull(prototype, "AmazonS3 must not be null");
        Assert.notNull(endpointUrl, "Endpoint Url must not be null");

        String region = getRegion(endpointUrl);
        Assert.notNull(region, "Error detecting region from endpoint url:'" + endpointUrl + "'");

        if (!this.clientCache.containsKey(region)) {
            AmazonS3ClientBuilder amazonS3ClientBuilder = buildAmazonS3ForRegion(prototype, region);
            this.clientCache.putIfAbsent(region, amazonS3ClientBuilder.build());
        }

        return this.clientCache.get(region);
    }

    private static String getRegion(String endpointUrl) {
        Assert.notNull(endpointUrl, "Endpoint Url must not be null");
        try {
            URI uri = new URI(endpointUrl);
            if ("s3.amazonaws.com".equals(uri.getHost())) {
                return Regions.DEFAULT_REGION.getName();
            } else {
                return new AmazonS3URI(endpointUrl).getRegion();
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Malformed URL received for endpoint", e);
        }
    }

    private AmazonS3ClientBuilder buildAmazonS3ForRegion(AmazonS3 prototype, String region) {
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard();
        if (prototype instanceof AmazonS3Client) {
            AWSCredentialsProvider awsCredentialsProvider = (AWSCredentialsProvider) ReflectionUtils.getField(this.credentialsProviderField, prototype);
            clientBuilder.withCredentials(awsCredentialsProvider);
        }

        return clientBuilder.withRegion(region);
    }
}