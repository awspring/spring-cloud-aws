/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.listener;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;

/**
 *
 */
public abstract class AbstractPollingMessageListenerContainer extends AbstractMessageListenerContainer implements SmartLifecycle {

	private AmazonSQS amazonSQS;
	private TaskScheduler taskScheduler;
	private TaskExecutor taskExecutor;
	private volatile boolean started;

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void setAmazonSQS(AmazonSQS amazonSQS) {
		this.amazonSQS = amazonSQS;
	}


	public boolean isAutoStartup() {
		return true;
	}

	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	public void start() {
		this.started = true;
	}

	public void stop() {

	}

	public boolean isRunning() {
		return this.started;
	}

	public int getPhase() {
		return 0;
	}

	public AmazonSQS getAmazonSQS() {
		return this.amazonSQS;
	}

	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	protected abstract void initialize();

	protected abstract void destroy();
}
