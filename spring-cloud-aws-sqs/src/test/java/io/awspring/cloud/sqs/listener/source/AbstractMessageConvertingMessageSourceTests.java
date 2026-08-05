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
package io.awspring.cloud.sqs.listener.source;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.support.converter.MessageConversionContext;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AbstractMessageConvertingMessageSource}.
 *
 * @author Bruno Augusto Garcia
 */
class AbstractMessageConvertingMessageSourceTests {

	@Test
	void shouldDelegatePayloadTypeOnlySetterToConversionHintAwareHook() {
		ConversionHintHookRecordingMessageSource source = new ConversionHintHookRecordingMessageSource();
		source.configure(SqsContainerOptions.builder().build());

		source.setPayloadDeserializationType(String.class);

		assertThat(source.payloadType).isEqualTo(String.class);
		assertThat(source.conversionHint).isNull();
		assertThat(source.context).isSameAs(source.getMessageConversionContext());
	}

	@Test
	void shouldInvokeLegacyHookFromConversionHintAwareSetter() {
		LegacyHookRecordingMessageSource source = new LegacyHookRecordingMessageSource();
		source.configure(SqsContainerOptions.builder().build());
		Object conversionHint = new Object();

		source.setPayloadDeserializationType(String.class, conversionHint);

		assertThat(source.payloadType).isEqualTo(String.class);
		assertThat(source.context).isSameAs(source.getMessageConversionContext());
	}

	private abstract static class TestMessageSource extends AbstractMessageConvertingMessageSource<Object, Object> {

		@Override
		public void setMessageSink(MessageSink<Object> messageSink) {
		}

	}

	private static class ConversionHintHookRecordingMessageSource extends TestMessageSource {

		private Class<?> payloadType;

		private Object conversionHint;

		private MessageConversionContext context;

		@Override
		protected void doConfigurePayloadTypeOnContext(Class<?> payloadType, Object conversionHint,
				MessageConversionContext context) {
			this.payloadType = payloadType;
			this.conversionHint = conversionHint;
			this.context = context;
		}

	}

	private static class LegacyHookRecordingMessageSource extends TestMessageSource {

		private Class<?> payloadType;

		private MessageConversionContext context;

		@Override
		protected void doConfigurePayloadTypeOnContext(Class<?> payloadType, MessageConversionContext context) {
			this.payloadType = payloadType;
			this.context = context;
		}

	}

}
