/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sns.sms.attributes;

import java.util.Map;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

public final class WNS implements ConvertToMessageAttributes {

	private String cachePolicy;
	private String group;
	private String match;
	private String suppressPopUp;
	private String tag;
	private Long ttl;
	private String type;

	public String getCachePolicy() {
		return cachePolicy;
	}

	public void setCachePolicy(String cachePolicy) {
		this.cachePolicy = cachePolicy;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getMatch() {
		return match;
	}

	public void setMatch(String match) {
		this.match = match;
	}

	public String getSuppressPopUp() {
		return suppressPopUp;
	}

	public void setSuppressPopUp(String suppressPopUp) {
		this.suppressPopUp = suppressPopUp;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Long getTtl() {
		return ttl;
	}

	public void setTtl(Long ttl) {
		this.ttl = ttl;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void convertAndPopulate(Map<String, MessageAttributeValue> attributeValueMap) {
		ConvertToMessageAttributes.populateMapWithStringValue(AttributeCodes.WNS_CACHE_POLICY, this.getCachePolicy(),
				attributeValueMap);
		ConvertToMessageAttributes.populateMapWithStringValue(AttributeCodes.WNS_GROUP, this.getGroup(),
				attributeValueMap);
		ConvertToMessageAttributes.populateMapWithStringValue(AttributeCodes.WNS_MATCH, this.getMatch(),
				attributeValueMap);
		ConvertToMessageAttributes.populateMapWithStringValue(AttributeCodes.WNS_TAG, this.getTag(), attributeValueMap);
		ConvertToMessageAttributes.populateMapWithStringValue(AttributeCodes.WNS_TYPE, this.getType(),
				attributeValueMap);
		ConvertToMessageAttributes.populateMapWithStringValue(AttributeCodes.WNS_SUPPRESS_POPUP,
				this.getSuppressPopUp(), attributeValueMap);
		ConvertToMessageAttributes.populateMapWithNumberValue(AttributeCodes.WNS_TTL, this.getTtl(), attributeValueMap);
	}

	public static final class Builder {
		private String cachePolicy;
		private String group;
		private String match;
		private String suppressPopUp;
		private String tag;
		private Long ttl;
		private String type;

		private Builder() {
		}

		public Builder cachePolicy(String cachePolicy) {
			this.cachePolicy = cachePolicy;
			return this;
		}

		public Builder group(String group) {
			this.group = group;
			return this;
		}

		public Builder match(String match) {
			this.match = match;
			return this;
		}

		public Builder suppressPopUp(String suppressPopUp) {
			this.suppressPopUp = suppressPopUp;
			return this;
		}

		public Builder tag(String tag) {
			this.tag = tag;
			return this;
		}

		public Builder ttl(Long ttl) {
			this.ttl = ttl;
			return this;
		}

		public Builder type(String type) {
			this.type = type;
			return this;
		}

		public WNS build() {
			WNS wNS = new WNS();
			wNS.setCachePolicy(cachePolicy);
			wNS.setGroup(group);
			wNS.setMatch(match);
			wNS.setSuppressPopUp(suppressPopUp);
			wNS.setTag(tag);
			wNS.setTtl(ttl);
			wNS.setType(type);
			return wNS;
		}
	}
}
