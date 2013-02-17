/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.core.region;

/**
 * Interface which provides all information for an Amazon Webservice to be called by the application. This information
 * include the region of service host (e.g. US, Ireland, Asia-Pacific) as well as the canonical representation of the
 * location (e.g. eu-west-1) and the endpoint for the service. An endpoint defines the exact service url to be used to
 * call the service itself.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public interface ServiceEndpoint {

	/**
	 * Return the region where this particular ServiceEndpoint is hosted.
	 *
	 * @return - the region where the service is hosted
	 */
	Region getRegion();

	/**
	 * Return the service url without the protocol for the particular service. The method return type is not a {@link
	 * java.net.URL} because the service endpoint does not assume the protocol (http/https) used to access the service.
	 *
	 * @return the service url without the protocol (e.g rds.eu-west-1.amazonaws.com) for the rds service running in the
	 *         Region.IRELAND (endpoint eu-west-1) on amazon.
	 */
	String getEndpoint();

	/**
	 * Return the canonical name of the location. In contrast to regions, the canonical does not contain any information
	 * about the physical location of the Region (e.g. us-west-1 / uw-west-2).
	 * <p/>
	 * <b>Note:</b> The location name is service dependent and not globally the same for every region. (e.g. Norther
	 * Virginia has the endpoint name us-east-1 for the Amazon RDS service but not for Amazon S3 or Amazon SimpleDB)
	 *
	 * @return the canonical name for the service endpoint (e.g. eu-west-1 for the Region.IRELAND).
	 */
	String getLocation();
}
