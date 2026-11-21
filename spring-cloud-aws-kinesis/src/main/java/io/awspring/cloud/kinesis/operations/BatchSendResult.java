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
package io.awspring.cloud.kinesis.operations;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Outcome of a batch send, carrying every request in its original request order through {@link #entries()}. Each
 * {@link Entry} reports whether that request succeeded and holds its {@link SendResult} or {@link FailedRecord}, so a
 * caller can tell which request failed even when successes and failures interleave across the {@code PutRecords} calls a
 * batch was split into. {@link #successful()} and {@link #failed()} are views derived from the entries, both in request
 * order.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public record BatchSendResult(List<Entry> entries) {

	public BatchSendResult {
		entries = List.copyOf(entries);
	}

	/**
	 * The successful results in request order.
	 * @return the successful results.
	 */
	public List<SendResult> successful() {
		List<SendResult> successful = new ArrayList<>();
		for (Entry entry : this.entries) {
			SendResult result = entry.result();
			if (result != null) {
				successful.add(result);
			}
		}
		return List.copyOf(successful);
	}

	/**
	 * The failed records in request order.
	 * @return the failed records.
	 */
	public List<FailedRecord> failed() {
		List<FailedRecord> failed = new ArrayList<>();
		for (Entry entry : this.entries) {
			FailedRecord failure = entry.failure();
			if (failure != null) {
				failed.add(failure);
			}
		}
		return List.copyOf(failed);
	}

	public boolean hasFailures() {
		for (Entry entry : this.entries) {
			if (!entry.isSuccessful()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * One request of the batch in its request-order position, holding either its {@link SendResult} when it succeeded or
	 * its {@link FailedRecord} when it failed. A request retried into a later success is reported once, as successful.
	 *
	 * @param index the position of the request in the batch, starting at {@code 0}.
	 * @param request the original request.
	 * @param result the result when the request succeeded, otherwise {@code null}.
	 * @param failure the failure when the request failed, otherwise {@code null}.
	 */
	public record Entry(int index, SendRequest request, @Nullable SendResult result, @Nullable FailedRecord failure) {

		public boolean isSuccessful() {
			return this.result != null;
		}

	}

	public record FailedRecord(Object payload, String partitionKey, String errorCode, String errorMessage) {
	}

}
