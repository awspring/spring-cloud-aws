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

package org.elasticspring.context.config.xml;

import org.elasticspring.context.config.AmazonS3FactoryBean;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.elasticspring.context.support.io.ResourceLoaderBeanPostProcessor;
import org.elasticspring.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.elasticspring.core.io.s3.encryption.KeyPairFactoryBean;
import org.elasticspring.core.io.s3.encryption.SecretKeyFactoryBean;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public class SimpleStorageLoaderBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	private static final String AMAZON_S3_BEAN_NAME = "AMAZON_S3";

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		if (!parserContext.getRegistry().containsBeanDefinition(AMAZON_S3_BEAN_NAME)) {
			buildAmazonS3Definition(element, parserContext);
		}

		builder.addConstructorArgReference(AMAZON_S3_BEAN_NAME);

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(ResourceLoaderBeanPostProcessor.class);
		beanDefinitionBuilder.addConstructorArgReference(PathMatchingSimpleStorageResourcePatternResolver.class.getName());
		AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
		String beanName = parserContext.getReaderContext().generateBeanName(beanDefinition);
		parserContext.getRegistry().registerBeanDefinition(beanName, beanDefinition);
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		return PathMatchingSimpleStorageResourcePatternResolver.class.getName();
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return PathMatchingSimpleStorageResourcePatternResolver.class;
	}

	private static void buildAmazonS3Definition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder amazonsS3Builder = BeanDefinitionBuilder.rootBeanDefinition(AmazonS3FactoryBean.class);

		Element clientEncryption = DomUtils.getChildElementByTagName(element, "client-encryption");
		if (clientEncryption != null) {
			String anonymousAttribute = clientEncryption.getAttribute("anonymous");
			boolean isAnonymous = StringUtils.hasText(anonymousAttribute) && "true".equals(anonymousAttribute);
			if (isAnonymous) {
				amazonsS3Builder.addPropertyValue("anonymous", true);
			} else {
				amazonsS3Builder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);
			}

			boolean encryptionMaterialSet = false;
			List<Element> elements = DomUtils.getChildElements(clientEncryption);
			for (Element encryptionKeyElement : elements) {
				if ("key-pair".equals(encryptionKeyElement.getLocalName())) {
					parseKeyPair(parserContext, amazonsS3Builder, encryptionKeyElement);
					encryptionMaterialSet = true;
				}

				if ("secret-key".equals(encryptionKeyElement.getLocalName())) {
					parseSecretKey(parserContext, amazonsS3Builder, encryptionKeyElement);
					encryptionMaterialSet = true;
				}
			}

			if (isAnonymous && !encryptionMaterialSet) {
				parserContext.getReaderContext().error("When attribute 'anonymous' is set to 'true' either 'key-pair' or 'secret-key' must be set.", clientEncryption);
			}
		} else {
			amazonsS3Builder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);
		}

		parserContext.getRegistry().registerBeanDefinition(AMAZON_S3_BEAN_NAME, amazonsS3Builder.getBeanDefinition());

	}

	private static void parseKeyPair(ParserContext parserContext, BeanDefinitionBuilder amazonS3FactoryBeanBuilder, Element encryptionKeyElement) {
		boolean keyPairRefSet = false;
		String keyPairRef = encryptionKeyElement.getAttribute("ref");
		if (StringUtils.hasText(keyPairRef)) {
			amazonS3FactoryBeanBuilder.addPropertyReference("keyPair", keyPairRef);
			keyPairRefSet = true;
		}

		String privateKeyResource = encryptionKeyElement.getAttribute("private-key-resource");
		String publicKeyResource = encryptionKeyElement.getAttribute("public-key-resource");
		boolean keyPairResourceSet = false;
		if (StringUtils.hasText(privateKeyResource) && StringUtils.hasText(publicKeyResource)) {
			BeanDefinitionBuilder keyPairFactoryBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(KeyPairFactoryBean.class);
			keyPairFactoryBeanBuilder.addConstructorArgValue(privateKeyResource);
			keyPairFactoryBeanBuilder.addConstructorArgValue(publicKeyResource);
			amazonS3FactoryBeanBuilder.addPropertyValue("keyPair", keyPairFactoryBeanBuilder.getBeanDefinition());
			keyPairResourceSet = true;
		}

		if (keyPairRefSet && keyPairResourceSet) {
			parserContext.getReaderContext().error("'ref' and 'public-key-resource' with 'private-key-resource' are not allowed together in the same 'key-pair' element.", encryptionKeyElement);
		}

		if (!keyPairRefSet && !keyPairResourceSet) {
			parserContext.getReaderContext().error("Either attribute 'ref' or 'private-key-resource' with 'public-key-resource' must be defined on element 'key-pair'.", encryptionKeyElement);
		}
	}

	private static void parseSecretKey(ParserContext parserContext, BeanDefinitionBuilder amazonS3ClientFactoryBeanBuilder, Element encryptionKeyElement) {

		boolean secretKeyRefSet = false;
		String secretKeyRef = encryptionKeyElement.getAttribute("ref");
		if (StringUtils.hasText(secretKeyRef)) {
			amazonS3ClientFactoryBeanBuilder.addPropertyReference("secretKey", secretKeyRef);
			secretKeyRefSet = true;
		}

		String password = encryptionKeyElement.getAttribute("password");
		String salt = encryptionKeyElement.getAttribute("salt");
		boolean secretKeyResourceSet = false;
		if (StringUtils.hasText(password) && StringUtils.hasText(salt)) {
			BeanDefinitionBuilder secretKeyFactoryBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(SecretKeyFactoryBean.class);
			secretKeyFactoryBeanBuilder.addConstructorArgValue(password);
			secretKeyFactoryBeanBuilder.addConstructorArgValue(salt);
			amazonS3ClientFactoryBeanBuilder.addPropertyValue("secretKey", secretKeyFactoryBeanBuilder.getBeanDefinition());
			secretKeyResourceSet = true;
		}

		if (secretKeyRefSet && secretKeyResourceSet) {
			parserContext.getReaderContext().error("'ref' and 'password' with 'salt' are not allowed together in the same 'secret-key' element.", encryptionKeyElement);
		}

		if (!secretKeyRefSet && !secretKeyResourceSet) {
			parserContext.getReaderContext().error("Either attribute 'ref' or 'password' with 'salt' must be defined on element 'secret-key'.", encryptionKeyElement);
		}
	}
}