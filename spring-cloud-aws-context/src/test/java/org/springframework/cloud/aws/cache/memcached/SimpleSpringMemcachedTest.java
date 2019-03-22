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

package org.springframework.cloud.aws.cache.memcached;

import net.spy.memcached.MemcachedClientIF;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.Cache;
import org.springframework.scheduling.annotation.AsyncResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class SimpleSpringMemcachedTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getName_configuredName_configuredNameReturned() throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		String cacheName = cache.getName();

		// Assert
		assertThat(cacheName).isEqualTo("test");
	}

	@Test
	public void simpleSpringMemcached_withoutName_reportsError() throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("cacheName is mandatory");

		MemcachedClientIF client = mock(MemcachedClientIF.class);

		// Act
		// noinspection ResultOfObjectAllocationIgnored
		new SimpleSpringMemcached(client, null);

		// Assert

	}

	@Test
	public void simpleSpringMemcached_withoutMemcachedClient_reportsError()
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("memcachedClient is mandatory");

		// Act
		// noinspection ResultOfObjectAllocationIgnored
		new SimpleSpringMemcached(null, "test");

		// Assert

	}

	@Test
	public void getNativeCache_withConfiguredMemcachedClient_returnsConfiguredMemcachedClient()
			throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		Object nativeCache = cache.getNativeCache();

		// Assert
		assertThat(nativeCache).isSameAs(client);
	}

	@Test
	public void get_withoutTypeParameterAndFoundInstance_returnsValueWrapperWithInstance()
			throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		when(client.get("test")).thenReturn("cachedValue");

		// Act
		Cache.ValueWrapper valueWrapper = cache.get("test");

		// Assert
		assertThat(valueWrapper.get()).isSameAs("cachedValue");
	}

	@Test
	public void get_withoutTypeParameterAndNonFoundInstance_returnsValue()
			throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		Cache.ValueWrapper valueWrapper = cache.get("test");

		// Assert
		assertThat(valueWrapper).isNull();
	}

	@Test
	public void get_withTypeParameterAndFoundInstance_returnsConvertedValue()
			throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		when(client.get("test")).thenReturn("cachedValue");

		// Act
		String cachedElement = cache.get("test", String.class);

		// Assert
		assertThat(cachedElement).isEqualTo("cachedValue");
	}

	@Test
	public void get_withTypeParameterAndNonFoundInstance_returnsValue() throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		String cachedElement = cache.get("test", String.class);

		// Assert
		assertThat(cachedElement).isNull();
	}

	@Test
	public void get_withTypeParameterAndNonCompatibleInstance_reportsIllegalArgumentException()
			throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"java.lang.Long is not assignable to class java.lang.String");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		when(client.get("test")).thenReturn(23L);

		// Act
		cache.get("test", String.class);

		// Assert
	}

	@Test
	public void get_withoutTypeParameterAndNonCompatibleCacheKey_reportsIllegalArgumentException()
			throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"java.lang.Long is not assignable to class java.lang.String");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.get(23L);

		// Assert
	}

	@Test
	public void get_withTypeParameterAndNonCompatibleCacheKey_reportsIllegalArgumentException()
			throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"java.lang.Long is not assignable to class java.lang.String");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.get(23L, Object.class);

		// Assert
	}

	@Test
	public void get_withoutTypeParameterAndNullCacheKey_reportsIllegalArgumentException()
			throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("key parameter is mandatory");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.get(null);

		// Assert
	}

	@Test
	public void get_withTypeParameterAndNullCacheKey_reportsIllegalArgumentException()
			throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("key parameter is mandatory");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.get(null, Object.class);

		// Assert
	}

	@Test
	public void get_witValueLoaderAndNonExistingValue_createsValueFromValueLoaderAndStoresItInCache()
			throws Exception {

		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");
		cache.setExpiration(42);

		when(client.set("myKey", 42, "createdValue")).thenReturn(new AsyncResult<>(true));

		// Act
		String value = cache.get("myKey", () -> "createdValue");

		// Assert
		assertThat(value).isEqualTo("createdValue");
	}

	@Test
	public void get_witValueLoaderAndExistingValue_doesNotCallValueLoader()
			throws Exception {

		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		when(client.get("myKey")).thenReturn("existingValue");

		// Act
		String value = cache.get("myKey", () -> {
			throw new UnsupportedOperationException("Should not be called");
		});

		// Assert
		assertThat(value).isEqualTo("existingValue");
	}

	@Test
	public void put_nullCacheKey_reportsIllegalArgumentException() throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("key parameter is mandatory");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.put(null, "test");

		// Assert
	}

	@Test
	public void put_longCacheKey_reportsIllegalArgumentException() throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"java.lang.Long is not assignable to class java.lang.String");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.put(23L, "test");

		// Assert
	}

	@Test
	public void put_nullCacheValueWithDefaultExpiration_keyStoredInCache()
			throws Exception {

		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");
		when(client.set("test", 0, null)).thenReturn(new AsyncResult<>(true));

		// Act
		cache.put("test", null);

		// Assert
		verify(client, times(1)).set("test", 0, null);
	}

	@Test
	public void put_withDefaultExpiration_keyStoredInCache() throws Exception {

		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");
		when(client.set("test", 0, "cachedElement")).thenReturn(new AsyncResult<>(true));

		// Act
		cache.put("test", "cachedElement");

		// Assert
		verify(client, times(1)).set("test", 0, "cachedElement");
	}

	@Test
	public void put_withCustomExpiration_keyStoredInCache() throws Exception {

		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");
		cache.setExpiration(42);
		when(client.set("test", 42, "cachedElement")).thenReturn(new AsyncResult<>(true));

		// Act
		cache.put("test", "cachedElement");

		// Assert
		verify(client, times(1)).set("test", 42, "cachedElement");
	}

	@Test
	public void evict_nullCacheKey_reportsIllegalArgumentException() throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("key parameter is mandatory");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.evict(null);

		// Assert
	}

	@Test
	public void evict_longCacheKey_reportsIllegalArgumentException() throws Exception {

		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"java.lang.Long is not assignable to class java.lang.String");

		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.evict(23L);

		// Assert
	}

	@Test
	public void evict_withCacheKey_deletedObjectInCache() throws Exception {

		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");
		when(client.delete("test")).thenReturn(new AsyncResult<>(true));

		// Act
		cache.evict("test");

		// Assert
		verify(client, times(1)).delete("test");
	}

	@Test
	public void clear_withDefaultSettings_flushesCache() throws Exception {

		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");

		// Act
		cache.clear();

		// Assert
		verify(client, times(1)).flush();
	}

	@Test
	public void putIfAbsent_withNewValue_shouldPutTheNewValueAndReturnNull()
			throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");
		when(client.add("key", 0, "value")).thenReturn(new AsyncResult<>(true));

		// Act
		Cache.ValueWrapper valueWrapper = cache.putIfAbsent("key", "value");

		// Assert
		assertThat(valueWrapper).isNull();
	}

	@Test
	public void putIfAbsent_withExistingValue_shouldNotPutTheValueAndReturnTheExistingOne()
			throws Exception {
		// Arrange
		MemcachedClientIF client = mock(MemcachedClientIF.class);
		SimpleSpringMemcached cache = new SimpleSpringMemcached(client, "test");
		when(client.get("key")).thenReturn("value");

		// Act
		Cache.ValueWrapper valueWrapper = cache.putIfAbsent("key", "value");

		// Assert
		assertThat(valueWrapper.get()).isEqualTo("value");
	}

}
