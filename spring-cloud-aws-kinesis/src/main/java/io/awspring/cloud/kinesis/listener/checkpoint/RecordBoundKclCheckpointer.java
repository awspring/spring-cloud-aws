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
package io.awspring.cloud.kinesis.listener.checkpoint;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link KclCheckpointer} bound to a single record, handed to a
 * {@link io.awspring.cloud.kinesis.listener.KclListenerMode#SINGLE_RECORD SINGLE_RECORD} listener so that
 * {@link #checkpoint()} commits exactly that record's position rather than the latest position of the whole batch. This
 * is what makes manual checkpointing safe for a single-record listener: checkpointing the first record of a batch does
 * not commit records that have not been delivered yet.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
class RecordBoundKclCheckpointer implements KclCheckpointer {

	private final KclCheckpointer delegate;

	private final Message<?> record;

	RecordBoundKclCheckpointer(KclCheckpointer delegate, Message<?> record) {
		Assert.notNull(delegate, "delegate must not be null");
		Assert.notNull(record, "record must not be null");
		this.delegate = delegate;
		this.record = record;
	}

	@Override
	public void checkpoint() {
		this.delegate.checkpoint(this.record);
	}

	@Override
	public void checkpoint(Message<?> message) {
		this.delegate.checkpoint(message);
	}

}
