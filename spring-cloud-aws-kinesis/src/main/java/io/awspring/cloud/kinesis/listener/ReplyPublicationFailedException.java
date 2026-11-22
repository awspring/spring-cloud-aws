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
package io.awspring.cloud.kinesis.listener;

import org.springframework.util.Assert;

/**
 * Thrown when a listener method succeeded but publishing its reply failed. The listener result is kept, so
 * {@link #resend()} publishes the same reply again without invoking the listener method a second time.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class ReplyPublicationFailedException extends ListenerExecutionFailedException {

	private final transient Runnable resender;

	public ReplyPublicationFailedException(String message, Throwable cause, Runnable resender) {
		super(message, cause);
		Assert.notNull(resender, "resender must not be null");
		this.resender = resender;
	}

	/**
	 * Publish the cached listener result again. Throws a new {@link ReplyPublicationFailedException} if the publication
	 * fails again.
	 */
	public void resend() {
		this.resender.run();
	}

}
