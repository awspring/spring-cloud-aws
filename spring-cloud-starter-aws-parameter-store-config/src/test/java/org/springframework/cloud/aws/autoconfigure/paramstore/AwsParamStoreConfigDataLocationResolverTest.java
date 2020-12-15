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

package org.springframework.cloud.aws.autoconfigure.paramstore;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsParamStoreConfigDataLocationResolverTest {

	@Test
	void testResolveProfileSpecificWithAutomaticPaths() {
		String location = "aws-parameterstore:";
		List<AwsParamStoreConfigDataResource> locations = testResolveProfileSpecific(location);
		assertThat(locations).hasSize(4);
		assertThat(toContexts(locations)).containsExactly("/config/testapp_dev/", "/config/testapp/",
				"/config/application_dev/", "/config/application/");
	}

	@Test
	void testResolveProfileSpecificWithCustomPaths() {
		String location = "aws-parameterstore:/mypath1;/mypath2;/mypath3";
		List<AwsParamStoreConfigDataResource> locations = testResolveProfileSpecific(location);
		assertThat(locations).hasSize(3);
		assertThat(toContexts(locations)).containsExactly("/mypath1", "/mypath2", "/mypath3");
	}

	private List<String> toContexts(List<AwsParamStoreConfigDataResource> locations) {
		return locations.stream().map(AwsParamStoreConfigDataResource::getContext).collect(Collectors.toList());
	}

	private List<AwsParamStoreConfigDataResource> testResolveProfileSpecific(String location) {
		AwsParamStoreConfigDataLocationResolver resolver = createResolver();
		ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.application.name", "testapp");
		when(context.getBinder()).thenReturn(Binder.get(env));
		Profiles profiles = mock(Profiles.class);
		when(profiles.getAccepted()).thenReturn(Collections.singletonList("dev"));
		return resolver.resolveProfileSpecific(context, ConfigDataLocation.of(location), profiles);
	}

	private AwsParamStoreConfigDataLocationResolver createResolver() {
		return new AwsParamStoreConfigDataLocationResolver() {
			@Override
			public <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type, T instance) {
				// do nothing
			}

			@Override
			protected <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type,
					BootstrapRegistry.InstanceSupplier<T> supplier) {
				// do nothing
			}

			@Override
			protected <T> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<T> type,
					BootstrapRegistry.InstanceSupplier<T> supplier) {
				// do nothing
			}
		};
	}

}
