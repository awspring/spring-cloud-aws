/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.autoconfigure.verifiedpermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.verifiedpermissions.AuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsRequestFactory;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsTemplate;
import io.awspring.cloud.verifiedpermissions.cache.ConcurrentDecisionCache;
import io.awspring.cloud.verifiedpermissions.cache.DecisionCache;
import io.awspring.cloud.verifiedpermissions.cache.NoOpDecisionCache;
import io.awspring.cloud.verifiedpermissions.security.DefaultPrincipalResolver;
import io.awspring.cloud.verifiedpermissions.security.PolicyStoreResolver;
import io.awspring.cloud.verifiedpermissions.security.PrincipalResolver;
import io.awspring.cloud.verifiedpermissions.security.TokenMode;
import io.awspring.cloud.verifiedpermissions.security.VerifiedPermissionsAuthorizer;
import io.awspring.cloud.verifiedpermissions.security.method.AvpPreAuthorize;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;
import software.amazon.awssdk.services.verifiedpermissions.model.Decision;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedResponse;

/**
 * Tests for {@link VerifiedPermissionsAutoConfiguration}.
 *
 * @author Matej Nedic
 */
class VerifiedPermissionsAutoConfigurationTest {

	private static final AutoConfigurations CONFIGURATIONS = AutoConfigurations.of(AwsAutoConfiguration.class,
			RegionProviderAutoConfiguration.class, CredentialsProviderAutoConfiguration.class,
			VerifiedPermissionsAutoConfiguration.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
					"spring.cloud.aws.verified-permissions.policy-store-id:policy-store-alias/photos",
					"spring.cloud.aws.verified-permissions.namespace:PhotoFlash")
			.withConfiguration(CONFIGURATIONS);

	private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
					"spring.cloud.aws.verified-permissions.policy-store-id:ps-1")
			.withConfiguration(CONFIGURATIONS);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createsBlockingBeans() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(VerifiedPermissionsClient.class);
			assertThat(context).hasSingleBean(VerifiedPermissionsTemplate.class);
			assertThat(context).hasSingleBean(DefaultPrincipalResolver.class);
			assertThat(context).getBean("avp").isInstanceOf(VerifiedPermissionsAuthorizer.class);
			PolicyStoreTarget target = context.getBean(PolicyStoreResolver.class).resolve(null);
			assertThat(target.policyStoreId()).isEqualTo("policy-store-alias/photos");
		});
	}

	@Test
	void failsWithoutPolicyStore() {
		new ApplicationContextRunner().withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
				.withConfiguration(CONFIGURATIONS).run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("policyStoreId");
				});
	}

	@Test
	void canBeDisabled() {
		contextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(VerifiedPermissionsClient.class);
			assertThat(context).doesNotHaveBean("avp");
		});
	}

	@Test
	void methodSecurityCanBeDisabled() {
		contextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.method-security.enabled:false")
				.run(context -> {
					assertThat(context).hasSingleBean(VerifiedPermissionsTemplate.class);
					assertThat(context).doesNotHaveBean("avp");
					assertThat(context).doesNotHaveBean(PrincipalResolver.class);
				});
	}

	@Test
	void usesCustomEndpoint() {
		contextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.endpoint:http://localhost:8090")
				.run(context -> {
					ConfiguredAwsClient client = new ConfiguredAwsClient(
							context.getBean(VerifiedPermissionsClient.class));
					assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
					assertThat(client.isEndpointOverridden()).isTrue();
				});
	}

	@Test
	void appliesClientCustomizer() {
		contextRunner
				.withBean(VerifiedPermissionsClientCustomizer.class,
						() -> builder -> builder.endpointOverride(URI.create("http://customized:4566")))
				.run(context -> {
					ConfiguredAwsClient client = new ConfiguredAwsClient(
							context.getBean(VerifiedPermissionsClient.class));
					assertThat(client.getEndpoint()).isEqualTo(URI.create("http://customized:4566"));
				});
	}

	@Test
	void tokenModeRejectsPrincipalEntities() {
		contextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.token-mode:access-token")
				.run(context -> {
					VerifiedPermissionsRequestFactory factory = context
							.getBean(VerifiedPermissionsRequestFactory.class);
					AuthorizationRequest request = new AuthorizationRequest(PrincipalSource.accessToken("jwt"),
							"ViewPhoto", new EntityRef("Photo", "1"), Map.of(),
							List.of(Entity.of(new EntityRef("User", "bob"))));
					PolicyStoreTarget target = context.getBean(PolicyStoreResolver.class).resolve(null);
					assertThatThrownBy(() -> factory.isAuthorizedWithToken(target, request))
							.isInstanceOf(IllegalStateException.class);
				});
	}

	@Test
	void annotationsWorkEndToEndOnBlockingStack() {
		VerifiedPermissionsClient client = mock(VerifiedPermissionsClient.class);
		when(client.isAuthorized(any(IsAuthorizedRequest.class))).thenAnswer(invocation -> {
			IsAuthorizedRequest request = invocation.getArgument(0);
			return IsAuthorizedResponse.builder()
					.decision("1".equals(request.resource().entityId()) ? Decision.ALLOW : Decision.DENY).build();
		});
		contextRunner.withBean(VerifiedPermissionsClient.class, () -> client)
				.withUserConfiguration(BlockingSecurityConfig.class).run(context -> {
					SecurityContextHolder.getContext()
							.setAuthentication(new TestingAuthenticationToken("alice", "pw", "ROLE_USER"));
					BlockingPhotoService service = context.getBean(BlockingPhotoService.class);

					assertThat(service.delete("1")).isEqualTo("deleted 1");
					assertThatThrownBy(() -> service.delete("2")).isInstanceOf(AuthorizationDeniedException.class);

					ArgumentCaptor<IsAuthorizedRequest> captor = ArgumentCaptor.forClass(IsAuthorizedRequest.class);
					verify(client, times(2)).isAuthorized(captor.capture());
					IsAuthorizedRequest sent = captor.getAllValues().get(0);
					assertThat(sent.policyStoreId()).isEqualTo("policy-store-alias/photos");
					assertThat(sent.principal().entityType()).isEqualTo("PhotoFlash::User");
					assertThat(sent.principal().entityId()).isEqualTo("alice");
					assertThat(sent.action().actionType()).isEqualTo("PhotoFlash::Action");
					assertThat(sent.action().actionId()).isEqualTo("DeletePhoto");
					assertThat(sent.resource().entityType()).isEqualTo("PhotoFlash::Photo");
				});
	}

	@Test
	void bindsRenamedPropertiesAndDefaults() {
		contextRunner.run(context -> {
			VerifiedPermissionsProperties properties = context.getBean(VerifiedPermissionsProperties.class);
			assertThat(properties.getTokenMode()).isEqualTo(TokenMode.ENTITY);
			assertThat(properties.getRequestDeduplication().isEnabled()).isTrue();
			assertThat(properties.getEntityLoading().getMaxDepth()).isEqualTo(2);
			assertThat(properties.getCache().getStrategy())
					.isEqualTo(VerifiedPermissionsProperties.CacheStrategy.CAFFEINE);
		});
		contextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.token-mode:entity",
				"spring.cloud.aws.verified-permissions.request-deduplication.enabled:false",
				"spring.cloud.aws.verified-permissions.entity-loading.max-depth:3").run(context -> {
					VerifiedPermissionsProperties properties = context.getBean(VerifiedPermissionsProperties.class);
					assertThat(properties.getTokenMode()).isEqualTo(TokenMode.ENTITY);
					assertThat(properties.getRequestDeduplication().isEnabled()).isFalse();
					assertThat(properties.getEntityLoading().getMaxDepth()).isEqualTo(3);
				});
	}

	@Test
	void crossRequestCacheIsDisabledByDefault() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(NoOpDecisionCache.class));
	}

	@Test
	void selectsConcurrentCacheByProperty() {
		contextRunner
				.withPropertyValues("spring.cloud.aws.verified-permissions.cache.enabled:true",
						"spring.cloud.aws.verified-permissions.cache.strategy:concurrent")
				.run(context -> assertThat(context).hasSingleBean(ConcurrentDecisionCache.class));
	}

	@Test
	void selectsCaffeineCacheByDefaultWhenEnabled() {
		contextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.cache.enabled:true")
				.run(context -> assertThat(context).hasSingleBean(CaffeineDecisionCache.class));
	}

	@Test
	void defaultCaffeineStrategyWithoutCaffeineFailsWithActionableError() {
		contextRunner.withClassLoader(new FilteredClassLoader(Caffeine.class))
				.withPropertyValues("spring.cloud.aws.verified-permissions.cache.enabled:true")
				.run(context -> assertThat(context).getFailure().rootCause()
						.isInstanceOf(InvalidConfigurationPropertyValueException.class)
						.hasMessageContaining("spring.cloud.aws.verified-permissions.cache.strategy")
						.hasMessageContaining("com.github.ben-manes.caffeine:caffeine")
						.hasMessageContaining("strategy=concurrent for tests and small applications"));
	}

	@Test
	void reactiveApplicationFailsInsteadOfRunningMethodsUnchecked() {
		reactiveContextRunner
				.run(context -> assertThat(context).getFailure().rootCause().isInstanceOf(IllegalStateException.class)
						.hasMessageContaining("does not support reactive web applications"));
	}

	@Test
	void reactiveApplicationStartsWhenMethodSecurityIsDisabled() {
		reactiveContextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.method-security.enabled:false")
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void caffeineStrategyWithoutCaffeineIsIgnoredWhileCacheIsDisabled() {
		contextRunner.withClassLoader(new FilteredClassLoader(Caffeine.class))
				.withPropertyValues("spring.cloud.aws.verified-permissions.cache.strategy:caffeine")
				.run(context -> assertThat(context).hasSingleBean(NoOpDecisionCache.class));
	}

	@Test
	void customDecisionCacheReplacesMissingCaffeine() {
		DecisionCache custom = new NoOpDecisionCache();
		contextRunner.withClassLoader(new FilteredClassLoader(Caffeine.class))
				.withPropertyValues("spring.cloud.aws.verified-permissions.cache.enabled:true")
				.withBean(DecisionCache.class, () -> custom)
				.run(context -> assertThat(context.getBean(DecisionCache.class)).isSameAs(custom));
	}

	@Test
	void customDecisionCacheOverridesBuiltInStrategy() {
		DecisionCache custom = new NoOpDecisionCache();
		contextRunner.withPropertyValues("spring.cloud.aws.verified-permissions.cache.enabled:true")
				.withBean(DecisionCache.class, () -> custom)
				.run(context -> assertThat(context.getBean(DecisionCache.class)).isSameAs(custom));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class BlockingSecurityConfig {

		@Bean
		BlockingPhotoService blockingPhotoService() {
			return new BlockingPhotoService();
		}
	}

	static class BlockingPhotoService {

		@AvpPreAuthorize(action = "DeletePhoto", resourceType = "Photo", resourceId = "#id")
		public String delete(String id) {
			return "deleted " + id;
		}
	}
}
