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
 * {@link Enum} class holding all available Amazon AWS regions that are available. A region hosts one or more Amazon
 * AWS services which can be used by the application. This enums does no assume anything about the services itself.
 * Available services on a region are represented as {@link ServiceEndpoint}s that are used by the application.
 * <p/>
 * All available regions are also available
 * <href a="http://aws.amazon.com/de/about-aws/globalinfrastructure/regional-product-services/">online</href>
 * <p/>
 * <b>Note:</b> Currently the Gov Cloud is not supported by ElasticSpring, therefore the region is not available in
 * this enumeration.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public enum Region {
	US_STANDARD,
	NORTHERN_CALIFORNIA,
	OREGON,
	IRELAND,
	SINGAPORE,
	SYDNEY,
	TOKYO,
	SAO_PAULO
}