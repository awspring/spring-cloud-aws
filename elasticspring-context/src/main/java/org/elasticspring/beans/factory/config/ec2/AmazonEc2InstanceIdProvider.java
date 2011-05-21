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

package org.elasticspring.beans.factory.config.ec2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;

/**
 *
 */
public class AmazonEC2InstanceIdProvider implements InstanceIdProvider {

	private static final String INSTANCE_ID_DEFAULT_URL = "http://169.254.169.254/latest/meta-data/instance-id";

	private final String instanceIdUrl;

	public AmazonEC2InstanceIdProvider(String instanceIdUrl) {
		this.instanceIdUrl = instanceIdUrl;
	}

	public AmazonEC2InstanceIdProvider() {
		this(INSTANCE_ID_DEFAULT_URL);
	}

	public String getCurrentInstanceId() throws IOException {
		URLConnection con = new URL(this.instanceIdUrl).openConnection();
		if (!(con instanceof HttpURLConnection)) {
			throw new IOException(MessageFormat.format("Service URL [{0}] is not an HTTP URL", this.instanceIdUrl));
		}

		HttpURLConnection httpURLConnection = (HttpURLConnection) con;
		httpURLConnection.setRequestMethod("GET");
		InputStream inputStream = null;
		BufferedReader bufferedReader = null;
		InputStreamReader inputStreamReader = null;
		String instanceId;
		try {
			httpURLConnection.connect();
			inputStream = httpURLConnection.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream);
			bufferedReader = new BufferedReader(inputStreamReader);
			instanceId = bufferedReader.readLine();
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}

			if (inputStreamReader != null) {
				inputStreamReader.close();
			}

			if (inputStream != null) {
				inputStream.close();
			}

			httpURLConnection.disconnect();
		}

		return instanceId;
	}

	
}
