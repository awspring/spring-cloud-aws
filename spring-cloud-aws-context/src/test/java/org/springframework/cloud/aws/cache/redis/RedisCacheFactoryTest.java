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

package org.springframework.cloud.aws.cache.redis;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RedisCacheFactoryTest {

    @Test
    public void createCache_withMockedRedisConnectionFactory_createsAndDestroysConnectionFactory() throws Exception {
        //Arrange
        final RedisConnectionFactory connectionFactory = Mockito.mock(RedisConnectionFactory.class, Mockito.withSettings().extraInterfaces(DisposableBean.class));
        RedisCacheFactory redisCacheFactory = new RedisCacheFactory() {

            @Override
            protected RedisConnectionFactory createConnectionClient(String hostName, int port) {
                assertEquals("someHost", hostName);
                assertEquals(4711, port);
                return connectionFactory;
            }
        };

        //Act
        Cache cache = redisCacheFactory.createCache("test", "someHost", 4711);
        redisCacheFactory.destroy();

        //Assert
        assertNotNull(cache);
        assertEquals("test", cache.getName());
        assertNotNull(cache.getNativeCache());

        DisposableBean disposableBean = (DisposableBean) connectionFactory;
        Mockito.verify(disposableBean, Mockito.times(1)).destroy();
    }
}
