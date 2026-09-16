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
package io.awspring.cloud.autoconfigure.config.appconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.appconfig.AppConfigPropertySource;
import io.awspring.cloud.appconfig.RequestContext;
import io.awspring.cloud.autoconfigure.config.reload.ConfigurationUpdateStrategy;
import io.awspring.cloud.autoconfigure.config.reload.PollingAwsPropertySourceChangeDetector;
import io.awspring.cloud.autoconfigure.config.reload.ReloadProperties;
import io.awspring.cloud.autoconfigure.config.reload.ReloadStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestDetails;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestException;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestReason;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.InvalidParameterDetail;
import software.amazon.awssdk.services.appconfigdata.model.InvalidParameterProblem;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

/**
 * Tests session token rotation of {@link AppConfigPropertySource} across
 * {@link PollingAwsPropertySourceChangeDetector#executeCycle()} invocations.
 *
 * @author Matej Nedic
 */
class AppConfigPollingChangeDetectorTests {

	private static final RequestContext CONTEXT = new RequestContext("profile1", "env1", "app1", "app1#profile1#env1");

	private final AppConfigDataClient client = mock(AppConfigDataClient.class);

	private final ConfigurationUpdateStrategy strategy = mock(ConfigurationUpdateStrategy.class);

	private final StandardEnvironment environment = new StandardEnvironment();

	private final List<String> tokensUsed = new ArrayList<>();

	private final AtomicInteger tokenCounter = new AtomicInteger();

	@BeforeEach
	void setUp() {
		when(client.startConfigurationSession(any(StartConfigurationSessionRequest.class)))
				.thenReturn(StartConfigurationSessionResponse.builder().initialConfigurationToken("token-0").build());
	}

	@Test
	void everyCycleUsesTokenReturnedByPreviousCycle() {
		respondWith("key1=value1");

		AppConfigPropertySource propertySource = new AppConfigPropertySource(CONTEXT, client);
		propertySource.init();
		environment.getPropertySources().addFirst(propertySource);

		PollingAwsPropertySourceChangeDetector<AppConfigPropertySource> detector = detector();
		detector.executeCycle();
		detector.executeCycle();
		detector.executeCycle();

		assertThat(tokensUsed).containsExactly("token-0", "token-1", "token-2", "token-3");
		assertThat(tokensUsed).doesNotHaveDuplicates();
		verify(client, times(1)).startConfigurationSession(any(StartConfigurationSessionRequest.class));
		verify(strategy, never()).run();
	}

	@Test
	void firesReloadAndKeepsRotatingTokenWhenConfigurationChanges() {
		respondWith("key1=value1");

		AppConfigPropertySource propertySource = new AppConfigPropertySource(CONTEXT, client);
		propertySource.init();
		environment.getPropertySources().addFirst(propertySource);

		PollingAwsPropertySourceChangeDetector<AppConfigPropertySource> detector = detector();

		respondWith("key1=changed");
		detector.executeCycle();
		verify(strategy, times(1)).run();

		respondWith("key1=changed-again");
		detector.executeCycle();

		assertThat(tokensUsed).containsExactly("token-0", "token-1", "token-2");
		verify(strategy, times(2)).run();
	}

	@Test
	void recoversFromExpiredTokenAndKeepsPolling() {
		respondWith("key1=value1");

		AppConfigPropertySource propertySource = new AppConfigPropertySource(CONTEXT, client);
		propertySource.init();
		environment.getPropertySources().addFirst(propertySource);

		PollingAwsPropertySourceChangeDetector<AppConfigPropertySource> detector = detector();

		when(client.startConfigurationSession(any(StartConfigurationSessionRequest.class))).thenReturn(
				StartConfigurationSessionResponse.builder().initialConfigurationToken("recovered-token").build());
		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenAnswer(invocation -> {
			GetLatestConfigurationRequest request = invocation.getArgument(0);
			tokensUsed.add(request.configurationToken());
			if ("token-1".equals(request.configurationToken())) {
				throw BadRequestException.builder().message("Token not valid")
						.reason(BadRequestReason.INVALID_PARAMETERS)
						.details(
								BadRequestDetails.builder()
										.invalidParameters(Map.of("ConfigurationToken",
												InvalidParameterDetail.builder()
														.problem(InvalidParameterProblem.EXPIRED).build()))
										.build())
						.build();
			}
			return GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromUtf8String("key1=value1"))
					.contentType("text/plain").nextPollConfigurationToken("token-" + tokenCounter.incrementAndGet())
					.build();
		});

		detector.executeCycle();
		detector.executeCycle();

		assertThat(tokensUsed).containsExactly("token-0", "token-1", "recovered-token", "token-2");
		verify(strategy, never()).run();
	}

	private PollingAwsPropertySourceChangeDetector<AppConfigPropertySource> detector() {
		ReloadProperties reloadProperties = new ReloadProperties();
		reloadProperties.setStrategy(ReloadStrategy.REFRESH);
		return new PollingAwsPropertySourceChangeDetector<>(reloadProperties, AppConfigPropertySource.class, strategy,
				mock(TaskScheduler.class), environment);
	}

	private void respondWith(String content) {
		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenAnswer(invocation -> {
			GetLatestConfigurationRequest request = invocation.getArgument(0);
			tokensUsed.add(request.configurationToken());
			return GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromUtf8String(content))
					.contentType("text/plain").nextPollConfigurationToken("token-" + tokenCounter.incrementAndGet())
					.build();
		});
	}
}
