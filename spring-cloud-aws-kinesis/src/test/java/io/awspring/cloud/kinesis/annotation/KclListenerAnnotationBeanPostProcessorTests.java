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
package io.awspring.cloud.kinesis.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.kinesis.config.KclBootstrapConfiguration;
import io.awspring.cloud.kinesis.config.KclEndpoint;
import io.awspring.cloud.kinesis.config.KclHandlerMethodEndpoint;
import io.awspring.cloud.kinesis.config.MessageListenerContainerFactory;
import io.awspring.cloud.kinesis.listener.MessageListener;
import io.awspring.cloud.kinesis.listener.MessageListenerContainer;
import io.awspring.cloud.kinesis.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.messaging.handler.annotation.SendTo;
import software.amazon.kinesis.common.InitialPositionInStream;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclListenerAnnotationBeanPostProcessorTests {

	@Test
	@DisplayName("bootstrap discovers @KclListener, resolves the endpoint and registers a container")
	void discoversAnnotatedMethodAndRegistersContainer() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			CapturingContainerFactory factory = context.getBean(CapturingContainerFactory.class);
			assertThat(factory.endpoints).hasSize(1);
			KclEndpoint endpoint = factory.endpoints.get(0);
			assertThat(endpoint.getId()).isEqualTo("order-listener");
			assertThat(endpoint.getStreamNames()).containsExactly("orders");
			assertThat(endpoint.getApplicationName()).isEqualTo("order-processor");
			assertThat(endpoint).isInstanceOf(KclHandlerMethodEndpoint.class);
			assertThat(((KclHandlerMethodEndpoint) endpoint).getHandlerMethodFactory()).isNotNull();

			MessageListenerContainerRegistry registry = context.getBean(MessageListenerContainerRegistry.class);
			assertThat(registry.getContainerById("order-listener")).isNotNull();
			assertThat(registry.getListenerContainers()).hasSize(1);
		}
	}

	@Test
	@DisplayName("blank applicationName defaults to the container id")
	void applicationNameDefaultsToId() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DefaultAppNameConfig.class)) {
			CapturingContainerFactory factory = context.getBean(CapturingContainerFactory.class);
			assertThat(factory.endpoints).hasSize(1);
			KclEndpoint endpoint = factory.endpoints.get(0);
			assertThat(endpoint.getApplicationName()).isEqualTo(endpoint.getId());
		}
	}

	@Test
	@DisplayName("generates a default container id when none is provided")
	void generatesDefaultIdWhenNotProvided() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NoIdConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getId()).startsWith("io.awspring.cloud.kinesis.KclListenerEndpointContainer#");
			assertThat(endpoint.getApplicationName()).isEqualTo(endpoint.getId());
		}
	}

	@Test
	@DisplayName("resolves checkpoint and retrieval modes from the annotation")
	void resolvesCheckpointAndRetrievalMode() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModesConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getCheckpointMode()).isEqualTo(KclCheckpointMode.RECORD);
			assertThat(endpoint.getRetrievalMode()).isEqualTo(RetrievalMode.ENHANCED_FAN_OUT);
		}
	}

	@Test
	@DisplayName("defaults the initial position in stream to TRIM_HORIZON")
	void initialPositionDefaultsToTrimHorizon() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getInitialPositionInStream()).isEqualTo(InitialPositionInStream.TRIM_HORIZON);
		}
	}

	@Test
	@DisplayName("resolves LATEST as the initial position in stream from the annotation")
	void resolvesLatestInitialPosition() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				LatestPositionConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getInitialPositionInStream()).isEqualTo(InitialPositionInStream.LATEST);
		}
	}

	@Test
	@DisplayName("resolves the initial position in stream from a property placeholder")
	void resolvesInitialPositionFromPlaceholder() {
		try (AnnotationConfigApplicationContext context = contextWith(PlaceholderPositionConfig.class,
				"app.initial-position", "LATEST")) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getInitialPositionInStream()).isEqualTo(InitialPositionInStream.LATEST);
		}
	}

	@Test
	@DisplayName("an empty initial position in stream falls back to the factory configuration")
	void emptyInitialPositionFallsBackToFactory() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EmptyPositionConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getInitialPositionInStream()).isNull();
		}
	}

	@Test
	@DisplayName("AT_TIMESTAMP is rejected on the annotation since it cannot carry a timestamp")
	void rejectsAtTimestampInitialPosition() {
		assertThatThrownBy(() -> new AnnotationConfigApplicationContext(AtTimestampPositionConfig.class))
				.hasRootCauseInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("AT_TIMESTAMP is not supported on @KclListener");
	}

	@Test
	@DisplayName("resolves the per-stream identities (consumer, lease table, metrics namespace) from the annotation")
	void resolvesPerStreamIdentities() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				PerStreamIdentityConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getConsumerArn()).isEqualTo("arn:aws:kinesis:eu-west-1:123456789012:stream/orders");
			assertThat(endpoint.getConsumerName()).isNull();
			assertThat(endpoint.getLeaseTableName()).isEqualTo("orders-leases");
			assertThat(endpoint.getMetricsNamespace()).isEqualTo("orders-metrics");
		}
	}

	@Test
	@DisplayName("a consumerName that is not an ARN is resolved as a consumer name")
	void resolvesConsumerNameAsName() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ConsumerNameConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getConsumerName()).isEqualTo("orders-consumer");
			assertThat(endpoint.getConsumerArn()).isNull();
		}
	}

	@Test
	@DisplayName("leaves the per-stream identities null when not specified")
	void leavesPerStreamIdentitiesNullWhenNotSpecified() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getConsumerArn()).isNull();
			assertThat(endpoint.getConsumerName()).isNull();
			assertThat(endpoint.getLeaseTableName()).isNull();
			assertThat(endpoint.getMetricsNamespace()).isNull();
		}
	}

	@Test
	@DisplayName("leaves checkpoint and retrieval modes null when not specified")
	void leavesModesNullWhenNotSpecified() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getCheckpointMode()).isNull();
			assertThat(endpoint.getRetrievalMode()).isNull();
			assertThat(endpoint.getReplyStream()).isNull();
		}
	}

	@Test
	@DisplayName("a listener declaring several streams yields one endpoint consuming all of them")
	void resolvesMultipleStreams() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MultiStreamConfig.class)) {
			CapturingContainerFactory factory = context.getBean(CapturingContainerFactory.class);
			assertThat(factory.endpoints).hasSize(1);
			assertThat(factory.endpoints.get(0).getStreamNames()).containsExactly("orders", "shipments");
			assertThat(context.getBean(MessageListenerContainerRegistry.class).getListenerContainers()).hasSize(1);
		}
	}

	@Test
	@DisplayName("selects the container factory named by the annotation")
	void usesNamedFactoryWhenSpecified() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NamedFactoryConfig.class)) {
			CapturingContainerFactory primary = context.getBean("primaryFactory", CapturingContainerFactory.class);
			CapturingContainerFactory secondary = context.getBean("secondaryFactory", CapturingContainerFactory.class);
			assertThat(primary.endpoints).isEmpty();
			assertThat(secondary.endpoints).hasSize(1);
			assertThat(secondary.endpoints.get(0).getFactoryBeanName()).isEqualTo("secondaryFactory");
		}
	}

	@Test
	@DisplayName("resolves the @SendTo destination into the endpoint reply stream")
	void resolvesSendToReplyStream() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SendToConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getReplyStream()).isEqualTo("out-stream");
		}
	}

	@Test
	@DisplayName("resolves property placeholders in annotation attributes")
	void resolvesPlaceholders() {
		try (AnnotationConfigApplicationContext context = contextWith(PlaceholderConfig.class, "app.stream",
				"resolved-stream")) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getStreamNames()).containsExactly("resolved-stream");
		}
	}

	@Test
	@DisplayName("resolves streamName provided via the value() alias")
	void resolvesStreamNameViaValueAlias() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ValueAliasConfig.class)) {
			KclEndpoint endpoint = context.getBean(CapturingContainerFactory.class).endpoints.get(0);
			assertThat(endpoint.getStreamNames()).containsExactly("aliased-stream");
		}
	}

	@Test
	@DisplayName("registers a container per annotated method on the same bean")
	void registersMultipleListenersFromSameBean() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MultiConfig.class)) {
			CapturingContainerFactory factory = context.getBean(CapturingContainerFactory.class);
			assertThat(factory.endpoints).hasSize(2);
			assertThat(factory.endpoints).extracting(KclEndpoint::getId).containsExactlyInAnyOrder("first", "second");
			MessageListenerContainerRegistry registry = context.getBean(MessageListenerContainerRegistry.class);
			assertThat(registry.getListenerContainers()).hasSize(2);
		}
	}

	@Test
	@DisplayName("beans without @KclListener register no containers")
	void nonAnnotatedBeanRegistersNothing() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NoListenerConfig.class)) {
			assertThat(context.getBean(CapturingContainerFactory.class).endpoints).isEmpty();
			assertThat(context.getBean(MessageListenerContainerRegistry.class).getListenerContainers()).isEmpty();
		}
	}

	private static AnnotationConfigApplicationContext contextWith(Class<?> configClass, String key, String value) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(key, value)));
		context.register(configClass);
		context.refresh();
		return context;
	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class TestConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		OrderListener orderListener() {
			return new OrderListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class DefaultAppNameConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		NoAppNameListener noAppNameListener() {
			return new NoAppNameListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class NoIdConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		NoIdListener noIdListener() {
			return new NoIdListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class ModesConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		ModesListener modesListener() {
			return new ModesListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class NamedFactoryConfig {

		@Bean
		CapturingContainerFactory primaryFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		CapturingContainerFactory secondaryFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		NamedFactoryListener namedFactoryListener() {
			return new NamedFactoryListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class SendToConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		SendToListener sendToListener() {
			return new SendToListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class PlaceholderConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		PlaceholderListener placeholderListener() {
			return new PlaceholderListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class ValueAliasConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		ValueAliasListener valueAliasListener() {
			return new ValueAliasListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class MultiConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		MultiListener multiListener() {
			return new MultiListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class NoListenerConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		PlainBean plainBean() {
			return new PlainBean();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class LatestPositionConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		LatestPositionListener latestPositionListener() {
			return new LatestPositionListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class PlaceholderPositionConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		PlaceholderPositionListener placeholderPositionListener() {
			return new PlaceholderPositionListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class EmptyPositionConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		EmptyPositionListener emptyPositionListener() {
			return new EmptyPositionListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class AtTimestampPositionConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		AtTimestampPositionListener atTimestampPositionListener() {
			return new AtTimestampPositionListener();
		}

	}

	static class LatestPositionListener {

		@KclListener(id = "latest-position", streamNames = "orders", initialPositionInStream = "LATEST")
		void handle(String payload) {
		}

	}

	static class PlaceholderPositionListener {

		@KclListener(id = "placeholder-position", streamNames = "orders", initialPositionInStream = "${app.initial-position}")
		void handle(String payload) {
		}

	}

	static class EmptyPositionListener {

		@KclListener(id = "empty-position", streamNames = "orders", initialPositionInStream = "")
		void handle(String payload) {
		}

	}

	static class AtTimestampPositionListener {

		@KclListener(id = "at-timestamp-position", streamNames = "orders", initialPositionInStream = "AT_TIMESTAMP")
		void handle(String payload) {
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class PerStreamIdentityConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		PerStreamIdentityListener perStreamIdentityListener() {
			return new PerStreamIdentityListener();
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class ConsumerNameConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		ConsumerNameListener consumerNameListener() {
			return new ConsumerNameListener();
		}

	}

	static class PerStreamIdentityListener {

		@KclListener(id = "per-stream", streamNames = "orders", retrievalMode = "ENHANCED_FAN_OUT", consumerName = "arn:aws:kinesis:eu-west-1:123456789012:stream/orders", leaseTableName = "orders-leases", metricsNamespace = "orders-metrics")
		void handle(String payload) {
		}

	}

	static class ConsumerNameListener {

		@KclListener(id = "consumer-name", streamNames = "orders", retrievalMode = "ENHANCED_FAN_OUT", consumerName = "orders-consumer")
		void handle(String payload) {
		}

	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class MultiStreamConfig {

		@Bean
		CapturingContainerFactory containerFactory() {
			return new CapturingContainerFactory();
		}

		@Bean
		MultiStreamListener multiStreamListener() {
			return new MultiStreamListener();
		}

	}

	static class MultiStreamListener {

		@KclListener(id = "multi-stream", streamNames = { "orders", "shipments" })
		void handle(String payload) {
		}

	}

	static class NoIdListener {

		@KclListener(streamNames = "events")
		void handle(String payload) {
		}

	}

	static class ModesListener {

		@KclListener(id = "modes", streamNames = "orders", checkpointMode = "RECORD", retrievalMode = "ENHANCED_FAN_OUT")
		void handle(String payload) {
		}

	}

	static class NamedFactoryListener {

		@KclListener(id = "named", streamNames = "orders", factory = "secondaryFactory")
		void handle(String payload) {
		}

	}

	static class SendToListener {

		@KclListener(id = "send-to", streamNames = "orders")
		@SendTo("out-stream")
		String handle(String payload) {
			return payload;
		}

	}

	static class PlaceholderListener {

		@KclListener(id = "placeholder", streamNames = "${app.stream}")
		void handle(String payload) {
		}

	}

	static class ValueAliasListener {

		@KclListener(value = "aliased-stream", id = "value-alias")
		void handle(String payload) {
		}

	}

	static class MultiListener {

		@KclListener(id = "first", streamNames = "first-stream")
		void handleFirst(String payload) {
		}

		@KclListener(id = "second", streamNames = "second-stream")
		void handleSecond(String payload) {
		}

	}

	static class PlainBean {

		void notAListener(String payload) {
		}

	}

	static class OrderListener {

		@KclListener(id = "order-listener", streamNames = "orders", applicationName = "order-processor")
		void handle(String payload) {
		}

	}

	static class NoAppNameListener {

		@KclListener(id = "no-app", streamNames = "events")
		void handle(String payload) {
		}

	}

	static class CapturingContainerFactory implements MessageListenerContainerFactory {

		private final List<KclEndpoint> endpoints = new ArrayList<>();

		@Override
		public MessageListenerContainer createContainer(KclEndpoint endpoint) {
			this.endpoints.add(endpoint);
			return new StubContainer(endpoint.getId());
		}

	}

	static class StubContainer implements MessageListenerContainer {

		private String id;

		private boolean running;

		StubContainer(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public void setId(String id) {
			this.id = id;
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Nullable
		MessageListener getMessageListener() {
			return null;
		}

	}

}
