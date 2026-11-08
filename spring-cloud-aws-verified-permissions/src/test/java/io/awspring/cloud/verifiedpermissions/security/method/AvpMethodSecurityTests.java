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
package io.awspring.cloud.verifiedpermissions.security.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.verifiedpermissions.AuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.mapping.AnnotatedEntityResolver;
import io.awspring.cloud.verifiedpermissions.security.AvpAuthorizationResult;
import io.awspring.cloud.verifiedpermissions.security.DefaultPrincipalResolver;
import io.awspring.cloud.verifiedpermissions.security.VerifiedPermissionsAuthorizer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

class AvpMethodSecurityTests {

	private final PolicyStub policies = new PolicyStub("ViewPhoto Photo::\"1\"", "DeletePhoto Photo::\"1\"",
			"ViewPhoto Photo::\"3\"", "EditPhoto Photo::\"1\"", "Search Album::\"a1\"");

	private AnnotationConfigApplicationContext context;

	private PhotoService service;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext();
		context.registerBean(PolicyStub.class, () -> policies);
		context.register(Config.class);
		context.refresh();
		service = context.getBean(PhotoService.class);
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("alice", "pw", "ROLE_USER"));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		context.close();
	}

	@Test
	void preAuthorizeWithDomainObject() {
		assertThat(service.view(new Photo("1", "alice"))).isEqualTo("viewed 1");

		AuthorizationRequest request = policies.requests.get(0);
		assertThat(request.principal()).hasToString(
				"EntityPrincipal[principal=Entity[User::\"alice\", attributes=[], parents=[Role::\"USER\"]], relatedEntities=[]]");
		assertThat(request.entities()).extracting(Entity::ref).containsExactly(new EntityRef("Photo", "1"));
		assertThat(request.entities().get(0).attributes()).containsEntry("owner", new EntityRef("User", "alice"));

		assertThatThrownBy(() -> service.view(new Photo("2", "bob"))).isInstanceOfSatisfying(
				AuthorizationDeniedException.class,
				denied -> assertThat(denied.getAuthorizationResult()).isInstanceOfSatisfying(
						AvpAuthorizationResult.class,
						result -> assertThat(result.getResource()).isEqualTo(new EntityRef("Photo", "2"))));
	}

	@Test
	void preAuthorizeWithTypeIdContextAndDefaultActionName() {
		service.deletePhoto("1");
		assertThat(policies.requests.get(0).action()).isEqualTo("DeletePhoto");

		service.search("a1", "cats");
		assertThat(policies.requests.get(1).context()).containsEntry("query", "cats");

		assertThatThrownBy(() -> service.deletePhoto("2")).isInstanceOf(AuthorizationDeniedException.class);
	}

	@Test
	void deniesUnauthenticatedCallersWithoutCallingVerifiedPermissions() {
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("alice", "pw", List.of()) {
			@Override
			public boolean isAuthenticated() {
				return false;
			}
		});

		assertThatThrownBy(() -> service.deletePhoto("1")).isInstanceOf(AuthorizationDeniedException.class);
		assertThat(policies.requests).isEmpty();
	}

	@Test
	void postAuthorizeChecksReturnedObject() {
		assertThat(service.find("1").id()).isEqualTo("1");
		assertThatThrownBy(() -> service.find("2")).isInstanceOf(AuthorizationDeniedException.class);
		assertThatThrownBy(service::findNothing).isInstanceOf(AuthorizationDeniedException.class);
	}

	@Test
	void postFilterUsesBatchCalls() {
		List<Photo> photos = IntStream.rangeClosed(1, 40).mapToObj(i -> new Photo(String.valueOf(i), "bob")).toList();

		assertThat(service.album(photos)).extracting(Photo::id).containsExactly("1", "3");
		assertThat(service.albumArray(photos.toArray(Photo[]::new))).extracting(Photo::id).containsExactly("1", "3");
		assertThat(service.albumSet(Set.copyOf(photos))).extracting(Photo::id).containsExactlyInAnyOrder("1", "3");
		assertThat(policies.batchCalls).hasValue(3);
	}

	@Test
	void postFilterOnStreamPullsOneBatchAtATimeAndClosesTheSource() {
		AtomicBoolean closed = new AtomicBoolean();
		Stream<Photo> unbounded = Stream.iterate(1, i -> i + 1).map(i -> new Photo(String.valueOf(i), "bob"))
				.onClose(() -> closed.set(true));

		try (Stream<Photo> filtered = service.albumStream(unbounded)) {
			assertThat(filtered.limit(2)).extracting(Photo::id).containsExactly("1", "3");
		}

		assertThat(policies.batchCalls).hasValue(1);
		assertThat(closed).isTrue();
	}

	@Test
	void spelBeanWorksWithPreAuthorize() {
		assertThat(service.viewWithSpel("1")).isEqualTo("viewed 1");
		assertThatThrownBy(() -> service.viewWithSpel("2")).isInstanceOf(AuthorizationDeniedException.class);
	}

	@Test
	void allowedActionsUsesOneBatchCall() {
		VerifiedPermissionsAuthorizer avp = context.getBean(VerifiedPermissionsAuthorizer.class);

		assertThat(avp.allowedActions(new Photo("1", "alice"), "ViewPhoto", "EditPhoto", "DeletePhoto", "SharePhoto"))
				.containsExactly("ViewPhoto", "EditPhoto", "DeletePhoto");
		assertThat(policies.batchCalls).hasValue(1);
	}

	@Configuration
	@EnableMethodSecurity
	static class Config {

		@Bean
		VerifiedPermissionsAuthorizer avp(PolicyStub policies) {
			return new VerifiedPermissionsAuthorizer(policies.blocking(), new DefaultPrincipalResolver(),
					List.of(new AnnotatedEntityResolver()));
		}

		@Bean
		PhotoService photoService() {
			return new PhotoService();
		}

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		static Advisor avpPre(ObjectProvider<VerifiedPermissionsAuthorizer> avp, ApplicationContext context) {
			return AvpPreAuthorizeAuthorizationManager
					.interceptor(new AvpPreAuthorizeAuthorizationManager(avp::getObject, () -> handler(context)));
		}

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		static Advisor avpPost(ObjectProvider<VerifiedPermissionsAuthorizer> avp, ApplicationContext context) {
			return AvpPostAuthorizeAuthorizationManager
					.interceptor(new AvpPostAuthorizeAuthorizationManager(avp::getObject, () -> handler(context)));
		}

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		static Advisor avpPostFilter(ObjectProvider<VerifiedPermissionsAuthorizer> avp, ApplicationContext context) {
			return new AvpPostFilterMethodInterceptor(avp::getObject, () -> handler(context));
		}

		static DefaultMethodSecurityExpressionHandler handler(ApplicationContext context) {
			DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
			handler.setApplicationContext(context);
			return handler;
		}
	}

	static class PhotoService {

		@AvpPreAuthorize(action = "ViewPhoto", resource = "#photo")
		public String view(Photo photo) {
			return "viewed " + photo.id();
		}

		@AvpPreAuthorize(resourceType = "Photo", resourceId = "#id")
		public void deletePhoto(String id) {
		}

		@AvpPreAuthorize(resourceType = "Album", resourceId = "#albumId", context = "{'query': #query}")
		public void search(String albumId, String query) {
		}

		@AvpPostAuthorize(action = "ViewPhoto")
		public Photo find(String id) {
			return new Photo(id, "bob");
		}

		@AvpPostAuthorize(action = "ViewPhoto")
		public Photo findNothing() {
			return null;
		}

		@AvpPostFilter(action = "ViewPhoto")
		public List<Photo> album(List<Photo> photos) {
			return photos;
		}

		@AvpPostFilter(action = "ViewPhoto")
		public Photo[] albumArray(Photo[] photos) {
			return photos;
		}

		@AvpPostFilter(action = "ViewPhoto")
		public Set<Photo> albumSet(Set<Photo> photos) {
			return photos;
		}

		@AvpPostFilter(action = "ViewPhoto")
		public Stream<Photo> albumStream(Stream<Photo> photos) {
			return photos;
		}

		@PreAuthorize("@avp.authorize('ViewPhoto', 'Photo', #id)")
		public String viewWithSpel(String id) {
			return "viewed " + id;
		}
	}
}
