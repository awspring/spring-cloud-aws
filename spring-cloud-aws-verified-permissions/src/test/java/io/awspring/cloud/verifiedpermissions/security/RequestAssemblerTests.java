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
package io.awspring.cloud.verifiedpermissions.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.mapping.DefaultEntityGraphResolver;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoadContext;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class RequestAssemblerTests {

	private final TestingAuthenticationToken alice = new TestingAuthenticationToken("alice", "pw");

	private final PolicyStoreTarget photos = new PolicyStoreTarget("photos-store", "PhotoFlash");

	private final PolicyStoreTarget archive = new PolicyStoreTarget("archive-store", "PhotoFlash");

	private final ExecutorService executor = Executors.newFixedThreadPool(2);

	@AfterEach
	void shutdown() throws InterruptedException {
		executor.shutdownNow();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void doesNotShareLoadedRootsAcrossPolicyStoresOrAuthentications() {
		List<String> loads = new CopyOnWriteArrayList<>();
		EntityLoader loader = loader(context -> {
			loads.add(context.target().policyStoreId() + "/" + context.call().authentication().getName());
			return Map.of("store", context.target().policyStoreId());
		});
		RequestAssembler assembler = assembler(loader);
		RequestAuthorizationState state = new RequestAuthorizationState();
		EntityRef photo = EntityRef.of("Photo", "1");
		TestingAuthenticationToken bob = new TestingAuthenticationToken("bob", "pw");

		RequestAssembler.Resource fromPhotos = assembler.resource(photo, photos, call(alice, photo), state);
		RequestAssembler.Resource fromArchive = assembler.resource(photo, archive, call(alice, photo), state);
		assembler.resource(photo, archive, call(bob, photo), state);
		assembler.resource(EntityRef.of("PhotoFlash::Photo", "1"), photos, call(alice, photo), state);

		assertThat(fromPhotos.entities().get(0).attributes()).containsEntry("store", "photos-store");
		assertThat(fromArchive.entities().get(0).attributes()).containsEntry("store", "archive-store");
		assertThat(loads).containsExactly("photos-store/alice", "archive-store/alice", "archive-store/bob");
	}

	@Test
	void batchLoadContextListsEveryCallItServes() {
		List<EntityLoadContext> contexts = new CopyOnWriteArrayList<>();
		RequestAssembler assembler = assembler(loader(context -> {
			contexts.add(context);
			return Map.of();
		}));
		AuthorizationCall first = call(alice, EntityRef.of("Photo", "1"));
		AuthorizationCall second = new AuthorizationCall(alice, "EditPhoto", EntityRef.of("Photo", "2"), null, null,
				TokenModeOverride.DEFAULT);

		assembler.preload(
				List.of(new RequestAssembler.Root(EntityRef.of("Photo", "1"), photos, first),
						new RequestAssembler.Root(EntityRef.of("Photo", "2"), photos, second)),
				new RequestAuthorizationState());

		assertThat(contexts).hasSize(1);
		assertThat(contexts.get(0).call()).isEqualTo(first);
		assertThat(contexts.get(0).calls()).containsExactly(first, second);
	}

	@Test
	void concurrentCallersInOneRequestShareOneRootLoad() throws Exception {
		AtomicInteger loads = new AtomicInteger();
		CountDownLatch loading = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		RequestAssembler assembler = assembler(loader(context -> {
			loads.incrementAndGet();
			loading.countDown();
			await(release);
			return Map.of();
		}));
		RequestAuthorizationState state = new RequestAuthorizationState();
		EntityRef photo = EntityRef.of("Photo", "1");

		Future<RequestAssembler.Resource> leader = executor
				.submit(() -> assembler.resource(photo, photos, call(alice, photo), state));
		assertThat(loading.await(10, TimeUnit.SECONDS)).isTrue();
		CompletableFuture<RequestAssembler.Resource> follower = new CompletableFuture<>();
		Thread followerThread = new Thread(() -> {
			try {
				follower.complete(assembler.resource(photo, photos, call(alice, photo), state));
			}
			catch (Throwable e) {
				follower.completeExceptionally(e);
			}
		}, "avp-follower");
		followerThread.start();
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (followerThread.getState() != Thread.State.WAITING
				&& followerThread.getState() != Thread.State.TIMED_WAITING) {
			assertThat(System.nanoTime()).as("follower parked").isLessThan(deadline);
			Thread.onSpinWait();
		}
		release.countDown();

		assertThat(leader.get(10, TimeUnit.SECONDS).found()).isTrue();
		assertThat(follower.get(10, TimeUnit.SECONDS).found()).isTrue();
		followerThread.join(TimeUnit.SECONDS.toMillis(10));
		assertThat(loads).hasValue(1);
	}

	@Test
	void failedLoadIsNotCachedForTheRestOfTheRequest() {
		AtomicInteger loads = new AtomicInteger();
		RequestAssembler assembler = assembler(loader(context -> {
			if (loads.incrementAndGet() == 1) {
				throw new IllegalStateException("database down");
			}
			return Map.of();
		}));
		RequestAuthorizationState state = new RequestAuthorizationState();
		EntityRef photo = EntityRef.of("Photo", "1");

		assertThatThrownBy(() -> assembler.resource(photo, photos, call(alice, photo), state))
				.hasMessage("database down");
		assertThat(assembler.resource(photo, photos, call(alice, photo), state).found()).isTrue();
		assertThat(loads).hasValue(2);
	}

	private static AuthorizationCall call(TestingAuthenticationToken authentication, EntityRef resource) {
		return new AuthorizationCall(authentication, "ViewPhoto", resource, null, null, TokenModeOverride.DEFAULT);
	}

	private static RequestAssembler assembler(EntityLoader loader) {
		return new RequestAssembler(new DefaultPrincipalResolver(), List.of(), List.of(), List.of(loader),
				new DefaultEntityGraphResolver(List.of(), 2), List.of());
	}

	private static EntityLoader loader(java.util.function.Function<EntityLoadContext, Map<String, Object>> body) {
		return new EntityLoader() {
			@Override
			public boolean supports(EntityRef ref) {
				return true;
			}

			@Override
			public Map<EntityRef, Entity> loadAll(Set<EntityRef> refs, EntityLoadContext context) {
				Map<String, Object> attributes = body.apply(context);
				Map<EntityRef, Entity> result = new java.util.LinkedHashMap<>();
				refs.forEach(ref -> result.put(ref, new Entity(ref, attributes, Set.of())));
				return result;
			}
		};
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("latch not released");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
