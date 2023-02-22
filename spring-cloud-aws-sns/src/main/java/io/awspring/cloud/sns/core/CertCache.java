/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sns.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * fetch and cache the publicKey from signingCertUrl address.
 *
 * @author Dino Chiesa (https://github.com/DinoChiesa/Apigee-Java-AWS-SNS-Verifier)
 * @author kazaff
 */
public class CertCache {
	private CertCache() {
	} // uncomment if wanted

	private static final Cache<String, X509Certificate> certCache = Caffeine.newBuilder().maximumSize(20)
			.expireAfterAccess(30, TimeUnit.SECONDS).executor(Runnable::run).recordStats().build();

	public static X509Certificate getCert(String certUri) throws Exception {
		X509Certificate cert = certCache.getIfPresent(certUri);
		if (cert == null) {
			cert = readCertFromUri(certUri);
			certCache.put(certUri, cert);
		}
		cert.checkValidity();
		return cert;
	}

	private static X509Certificate readCertFromUri(String certUri) throws Exception {
		try (InputStream inStream = openFollowRedirects(certUri)) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inStream);
		}
	}

	private static InputStream openFollowRedirects(String uri) throws Exception {
		URL url = new URL(uri);
		HttpURLConnection c = (HttpURLConnection) url.openConnection();
		Set<String> visitedUrls = new HashSet<>();
		boolean doneRedirecting = false;
		while (!doneRedirecting) {
			switch (c.getResponseCode()) {
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
				// Follow redirect if not already visisted
				String newLocation = c.getHeaderField("Location");
				if (visitedUrls.contains(newLocation)) {
					throw new RuntimeException(
							String.format("Infinite redirect loop detected for URL %s", url.toString()));
				}
				visitedUrls.add(newLocation);

				url = new URL(newLocation);
				c = (HttpURLConnection) url.openConnection();
				break;
			default:
				doneRedirecting = true;
				break;
			}
		}

		return c.getInputStream();
	}
}
