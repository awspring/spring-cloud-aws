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

package org.springframework.cloud.aws.cache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * @author Agim Emruli
 */
public class StaticMemcachedFactoryBean extends AbstractFactoryBean<MemcachedClientIF> {

	private final String address;

	public StaticMemcachedFactoryBean(String address) {
		this.address = address;
	}

	@Override
	public Class<?> getObjectType() {
		return MemcachedClientIF.class;
	}

	@Override
	protected MemcachedClientIF createInstance() throws Exception {
		return new MemcachedClient(AddrUtil.getAddresses(this.address));
	}

	@Override
	protected void destroyInstance(MemcachedClientIF instance) throws Exception {
		instance.shutdown();
	}
}
