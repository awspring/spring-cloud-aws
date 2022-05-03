/*
 * Copyright 2013-2021 the original author or authors.
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
package io.awspring.cloud.samples.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@DynamoDbBean
public class BankingInformation {

private UUID cardId;
private UUID userId;
private LocalDate validity;
private String cardName;
private BigDecimal balance;



	@DynamoDbPartitionKey
	public UUID getCardId() {
		return cardId;
	}

	public void setCardId(UUID cardId) {
		this.cardId = cardId;
	}

	@DynamoDbSortKey
	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public LocalDate getValidity() {
		return validity;
	}

	public void setValidity(LocalDate validity) {
		this.validity = validity;
	}

	public String getCardName() {
		return cardName;
	}

	public void setCardName(String cardName) {
		this.cardName = cardName;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}


	public static final class Builder {
		private UUID cardId;
		private UUID userId;
		private LocalDate validity;
		private String cardName;
		private BigDecimal balance;

		private Builder() {
		}

		public static Builder builder() {
			return new Builder();
		}

		public Builder withCardId(UUID cardId) {
			this.cardId = cardId;
			return this;
		}

		public Builder withUserId(UUID userId) {
			this.userId = userId;
			return this;
		}

		public Builder withValidity(LocalDate validity) {
			this.validity = validity;
			return this;
		}

		public Builder withCardName(String cardName) {
			this.cardName = cardName;
			return this;
		}

		public Builder withBalance(BigDecimal balance) {
			this.balance = balance;
			return this;
		}

		public BankingInformation build() {
			BankingInformation bankingInformation = new BankingInformation();
			bankingInformation.setCardId(cardId);
			bankingInformation.setUserId(userId);
			bankingInformation.setValidity(validity);
			bankingInformation.setCardName(cardName);
			bankingInformation.setBalance(balance);
			return bankingInformation;
		}
	}
}
