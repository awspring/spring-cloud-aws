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

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsOperations;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsRequestFactory;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsTemplate;
import io.awspring.cloud.verifiedpermissions.cache.ConcurrentDecisionCache;
import io.awspring.cloud.verifiedpermissions.cache.DecisionCache;
import io.awspring.cloud.verifiedpermissions.cache.NoOpDecisionCache;
import io.awspring.cloud.verifiedpermissions.mapping.AnnotatedEntityResolver;
import io.awspring.cloud.verifiedpermissions.mapping.DefaultEntityGraphResolver;
import io.awspring.cloud.verifiedpermissions.mapping.EntityGraphResolver;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoader;
import io.awspring.cloud.verifiedpermissions.mapping.EntityResolver;
import io.awspring.cloud.verifiedpermissions.security.ContextProvider;
import io.awspring.cloud.verifiedpermissions.security.DefaultPrincipalResolver;
import io.awspring.cloud.verifiedpermissions.security.PolicyStoreResolver;
import io.awspring.cloud.verifiedpermissions.security.PrincipalEntityContributor;
import io.awspring.cloud.verifiedpermissions.security.PrincipalResolver;
import io.awspring.cloud.verifiedpermissions.security.TokenModeResolver;
import io.awspring.cloud.verifiedpermissions.security.VerifiedPermissionsAuthorizer;
import io.awspring.cloud.verifiedpermissions.security.method.AvpPostAuthorizeAuthorizationManager;
import io.awspring.cloud.verifiedpermissions.security.method.AvpPostFilterMethodInterceptor;
import io.awspring.cloud.verifiedpermissions.security.method.AvpPreAuthorizeAuthorizationManager;
import io.micrometer.observation.ObservationRegistry;
import java.util.Set;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.ObservationAuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.authorization.method.AuthorizationManagerAfterMethodInterceptor;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.MethodInvocationResult;
import org.springframework.util.function.SingletonSupplier;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;

/**
 * Auto-configuration for Amazon Verified Permissions.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@AutoConfiguration(after = { CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@EnableConfigurationProperties(VerifiedPermissionsProperties.class)
@ConditionalOnClass({ VerifiedPermissionsClient.class, VerifiedPermissionsTemplate.class })
@ConditionalOnProperty(name = "spring.cloud.aws.verified-permissions.enabled", havingValue = "true", matchIfMissing = true)
public class VerifiedPermissionsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public VerifiedPermissionsClient verifiedPermissionsClient(VerifiedPermissionsProperties properties,
			AwsClientBuilderConfigurer configurer, ObjectProvider<AwsConnectionDetails> connectionDetails,
			ObjectProvider<VerifiedPermissionsClientCustomizer> clientCustomizers,
			ObjectProvider<AwsSyncClientCustomizer> awsCustomizers) {
		return configurer.configureSyncClient(VerifiedPermissionsClient.builder(), properties,
				connectionDetails.getIfAvailable(), clientCustomizers.orderedStream(), awsCustomizers.orderedStream())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public VerifiedPermissionsRequestFactory verifiedPermissionsRequestFactory() {
		return new VerifiedPermissionsRequestFactory();
	}

	@Bean
	@ConditionalOnMissingBean(VerifiedPermissionsOperations.class)
	public VerifiedPermissionsTemplate verifiedPermissionsTemplate(VerifiedPermissionsClient client,
			VerifiedPermissionsRequestFactory requestFactory) {
		return new VerifiedPermissionsTemplate(client, requestFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public PolicyStoreResolver verifiedPermissionsPolicyStoreResolver(VerifiedPermissionsProperties properties) {
		String policyStoreId = properties.getPolicyStoreId();
		PolicyStoreTarget target = new PolicyStoreTarget(policyStoreId, properties.getNamespace(),
				Set.of(properties.getPrincipalEntityType(), properties.getGroupEntityType()), policyStoreId);
		return call -> target;
	}

	@Bean
	@ConditionalOnMissingBean
	public TokenModeResolver verifiedPermissionsTokenModeResolver(VerifiedPermissionsProperties properties) {
		return call -> properties.getTokenMode();
	}

	@Bean
	@ConditionalOnMissingBean
	public AnnotatedEntityResolver annotatedEntityResolver() {
		return new AnnotatedEntityResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	public EntityGraphResolver verifiedPermissionsEntityGraphResolver(ObjectProvider<EntityLoader> loaders,
			VerifiedPermissionsProperties properties) {
		return new DefaultEntityGraphResolver(loaders.orderedStream().toList(),
				properties.getEntityLoading().getMaxDepth());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(DecisionCache.class)
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".cache", name = "enabled", havingValue = "false", matchIfMissing = true)
	static class DisabledCacheConfiguration {
		@Bean
		DecisionCache verifiedPermissionsDecisionCache() {
			return new NoOpDecisionCache();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(DecisionCache.class)
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".cache", name = "enabled", havingValue = "true")
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".cache", name = "strategy", havingValue = "concurrent")
	static class ConcurrentCacheConfiguration {
		@Bean
		DecisionCache verifiedPermissionsDecisionCache(VerifiedPermissionsProperties properties) {
			return new ConcurrentDecisionCache(Math.toIntExact(properties.getCache().getMaxSize()));
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
	@ConditionalOnMissingBean(DecisionCache.class)
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".cache", name = "enabled", havingValue = "true")
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".cache", name = "strategy", havingValue = "caffeine", matchIfMissing = true)
	static class CaffeineCacheConfiguration {
		@Bean
		DecisionCache verifiedPermissionsDecisionCache(VerifiedPermissionsProperties properties) {
			return new CaffeineDecisionCache(properties.getCache().getMaxSize());
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("com.github.benmanes.caffeine.cache.Caffeine")
	@ConditionalOnMissingBean(DecisionCache.class)
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".cache", name = "enabled", havingValue = "true")
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".cache", name = "strategy", havingValue = "caffeine", matchIfMissing = true)
	static class MissingCaffeineConfiguration {
		@Bean
		DecisionCache verifiedPermissionsDecisionCache() {
			throw new InvalidConfigurationPropertyValueException(
					VerifiedPermissionsProperties.PREFIX + ".cache.strategy", "caffeine",
					"Caffeine is not on the classpath. Add com.github.ben-manes.caffeine:caffeine for production, use"
							+ " strategy=concurrent for tests and small applications, or define a DecisionCache bean.");
		}
	}

	/**
	 * Only Spring MVC and non-web applications are supported. In a WebFlux application the interceptors would read an
	 * empty {@code SecurityContextHolder}, so fail at startup instead of running annotated methods unchecked.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".method-security", name = "enabled", havingValue = "true", matchIfMissing = true)
	static class ReactiveApplicationConfiguration {
		@Bean
		static Object verifiedPermissionsReactiveApplicationCheck() {
			throw new IllegalStateException("Verified Permissions method security does not support reactive web"
					+ " applications. Set " + VerifiedPermissionsProperties.PREFIX
					+ ".method-security.enabled=false to use only VerifiedPermissionsTemplate.");
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.security.authorization.AuthorizationManager")
	@ConditionalOnProperty(prefix = VerifiedPermissionsProperties.PREFIX
			+ ".method-security", name = "enabled", havingValue = "true", matchIfMissing = true)
	static class MethodSecurityConfiguration {

		@Bean
		@ConditionalOnMissingBean
		PrincipalResolver verifiedPermissionsPrincipalResolver(VerifiedPermissionsProperties properties) {
			return new DefaultPrincipalResolver(properties.getTokenMode(), properties.getPrincipalEntityType(),
					properties.getGroupEntityType(), properties.getRolePrefix());
		}

		@Bean(name = "avp")
		@ConditionalOnMissingBean(name = "avp")
		VerifiedPermissionsAuthorizer avp(VerifiedPermissionsOperations operations, PrincipalResolver principalResolver,
				ObjectProvider<EntityResolver> entityResolvers, ObjectProvider<EntityLoader> entityLoaders,
				EntityGraphResolver graphResolver, ObjectProvider<PrincipalEntityContributor> principalContributors,
				ObjectProvider<ContextProvider> contextProviders, PolicyStoreResolver policyStoreResolver,
				TokenModeResolver tokenModeResolver, DecisionCache decisionCache,
				VerifiedPermissionsProperties properties) {
			return new VerifiedPermissionsAuthorizer(operations, principalResolver,
					entityResolvers.orderedStream().toList(), entityLoaders.orderedStream().toList(), graphResolver,
					principalContributors.orderedStream().toList(), contextProviders.orderedStream().toList(),
					policyStoreResolver, tokenModeResolver, decisionCache, properties.getCache().getTtl(),
					properties.getCache().getTokenExpirySkew(), properties.getRequestDeduplication().isEnabled());
		}

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		static Advisor avpPreAuthorizeAdvisor(ObjectProvider<VerifiedPermissionsAuthorizer> authorizer,
				ObjectProvider<MethodSecurityExpressionHandler> handlers,
				ObjectProvider<ObservationRegistry> observationRegistry,
				ObjectProvider<AuthorizationEventPublisher> eventPublisher, ApplicationContext context) {
			AuthorizationManager<MethodInvocation> manager = new AvpPreAuthorizeAuthorizationManager(
					SingletonSupplier.of(authorizer::getObject), expressionHandler(handlers, context));
			ObservationRegistry registry = observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP);
			manager = new ObservationAuthorizationManager<>(registry, manager);
			AuthorizationManagerBeforeMethodInterceptor interceptor = AvpPreAuthorizeAuthorizationManager
					.interceptor(manager);
			interceptor.setAuthorizationEventPublisher(
					eventPublisher.getIfAvailable(() -> new SpringAuthorizationEventPublisher(context)));
			return interceptor;
		}

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		static Advisor avpPostAuthorizeAdvisor(ObjectProvider<VerifiedPermissionsAuthorizer> authorizer,
				ObjectProvider<MethodSecurityExpressionHandler> handlers,
				ObjectProvider<ObservationRegistry> observationRegistry,
				ObjectProvider<AuthorizationEventPublisher> eventPublisher, VerifiedPermissionsProperties properties,
				ApplicationContext context) {
			AuthorizationManager<MethodInvocationResult> manager = new AvpPostAuthorizeAuthorizationManager(
					SingletonSupplier.of(authorizer::getObject), expressionHandler(handlers, context),
					properties.getMethodSecurity().isDenyNullPostResource());
			ObservationRegistry registry = observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP);
			manager = new ObservationAuthorizationManager<>(registry, manager);
			AuthorizationManagerAfterMethodInterceptor interceptor = AvpPostAuthorizeAuthorizationManager
					.interceptor(manager);
			interceptor.setAuthorizationEventPublisher(
					eventPublisher.getIfAvailable(() -> new SpringAuthorizationEventPublisher(context)));
			return interceptor;
		}

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		static Advisor avpPostFilterAdvisor(ObjectProvider<VerifiedPermissionsAuthorizer> authorizer,
				ObjectProvider<MethodSecurityExpressionHandler> handlers, ApplicationContext context) {
			return new AvpPostFilterMethodInterceptor(SingletonSupplier.of(authorizer::getObject),
					expressionHandler(handlers, context));
		}
	}

	static Supplier<MethodSecurityExpressionHandler> expressionHandler(
			ObjectProvider<MethodSecurityExpressionHandler> expressionHandler, ApplicationContext context) {
		return SingletonSupplier.of(() -> expressionHandler.getIfAvailable(() -> {
			DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
			handler.setApplicationContext(context);
			return handler;
		}));
	}
}
