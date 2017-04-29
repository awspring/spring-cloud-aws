/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.autoconfigure.actuate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.aws.actuate.metrics.CloudWatchMetricWriter;

/**
 * Configuration properties for {@link CloudWatchMetricWriter}.
 *
 * @author Simon Buettner
 * @author Agim Emruli
 */
@ConfigurationProperties(prefix = "cloud.aws.cloudwatch")
public class CloudWatchMetricProperties {

    /**
     * The namespace which will be used when sending
     * metrics to CloudWatch. This property is needed and must not be null.
     */
    private String namespace = "";

    /**
     * The maximum number of elements the {@link CloudWatchMetricWriter}
     * will buffer before sending to CloudWatch.
     */
    private int maxBuffer = Integer.MAX_VALUE;

    /**
     * The delay of the background task which sends metrics to
     * CloudWatch. A higher delay leads to more buffering and less but larger requests.
     * A lower delay leads to a smaller buffer but more requests with less payload.
     */
    private long fixedDelayBetweenRuns = 1000;

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getMaxBuffer() {
        return this.maxBuffer;
    }

    public void setMaxBuffer(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    public long getFixedDelayBetweenRuns() {
        return this.fixedDelayBetweenRuns;
    }

    public void setFixedDelayBetweenRuns(long fixedDelayBetweenRuns) {
        this.fixedDelayBetweenRuns = fixedDelayBetweenRuns;
    }
}
