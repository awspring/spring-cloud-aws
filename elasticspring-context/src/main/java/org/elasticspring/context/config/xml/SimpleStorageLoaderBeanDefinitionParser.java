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
import org.elasticspring.core.io.s3.encryption.SymmetricKeyFactoryBean;
import org.elasticspring.core.io.s3.support.AmazonS3ClientFactory;
import org.elasticspring.core.region.Region;
import org.elasticspring.core.region.StaticRegionProvider;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
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

	private static void addRegionProviderBeanDefinition(Element element, ParserContext parserContext, BeanDefinitionBuilder parent) {
		if (StringUtils.hasText(element.getAttribute("region")) && StringUtils.hasText(element.getAttribute("region-provider-ref"))) {
			parserContext.getReaderContext().error("region and region-provider-ref attribute must not be used together", element);
			return;
		}

		if (StringUtils.hasText(element.getAttribute("region-provider-ref"))) {
			parent.addConstructorArgReference(element.getAttribute("region-provider-ref"));
			return;
		}

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(StaticRegionProvider.class);
		if (StringUtils.hasText(element.getAttribute("region"))) {
			beanDefinitionBuilder.addConstructorArgValue(element.getAttribute("region"));
			parent.addConstructorArgValue(beanDefinitionBuilder.getBeanDefinition());
		} else {
			beanDefinitionBuilder.addConstructorArgValue(Region.US_STANDARD);
		}

		parent.addConstructorArgValue(beanDefinitionBuilder.getBeanDefinition());
	}

	private static void buildAmazonS3Definition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder amazonsS3Builder = BeanDefinitionBuilder.rootBeanDefinition(AmazonS3FactoryBean.class);
		amazonsS3Builder.addConstructorArgValue(buildAmazonS3ClientFactoryDefinition(element, parserContext));
		addRegionProviderBeanDefinition(element, parserContext, amazonsS3Builder);

		parserContext.getRegistry().registerBeanDefinition(AMAZON_S3_BEAN_NAME, amazonsS3Builder.getBeanDefinition());

	}

	private static BeanDefinition buildAmazonS3ClientFactoryDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder amazonS3ClientFactoryBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonS3ClientFactory.class);
		amazonS3ClientFactoryBeanBuilder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

		Element clientEncryption = DomUtils.getChildElementByTagName(element, "client-encryption");
		if (clientEncryption != null) {
			String anonymous = element.getAttribute("anonymous");
			if (StringUtils.hasText(anonymous)) {
				amazonS3ClientFactoryBeanBuilder.addPropertyValue("anonymous", "true".equals(anonymous));
			}

			List<Element> elements = DomUtils.getChildElements(clientEncryption);
			for (Element encryptionKeyElement : elements) {
				if ("key-pair".equals(encryptionKeyElement.getLocalName())) {
					parseKeyPair(parserContext, amazonS3ClientFactoryBeanBuilder, encryptionKeyElement);
				}

				if ("symmetric-key".equals(encryptionKeyElement.getLocalName())) {
					parseSymmetricKey(parserContext, amazonS3ClientFactoryBeanBuilder, encryptionKeyElement);
				}
			}

		}

		return amazonS3ClientFactoryBeanBuilder.getBeanDefinition();
	}

	private static void parseKeyPair(ParserContext parserContext, BeanDefinitionBuilder amazonS3ClientFactoryBeanBuilder, Element encryptionKeyElement) {
		boolean keyPairRefSet = false;
		String keyPairRef = encryptionKeyElement.getAttribute("key-ref");
		if (StringUtils.hasText(keyPairRef)) {
			amazonS3ClientFactoryBeanBuilder.addPropertyReference("keyPair", keyPairRef);
			keyPairRefSet = true;
		}

		String privateKeyResource = encryptionKeyElement.getAttribute("private-key-resource");
		String publicKeyResource = encryptionKeyElement.getAttribute("public-key-resource");
		boolean keyPairResourceSet = false;
		if (StringUtils.hasText(privateKeyResource) && StringUtils.hasText(publicKeyResource)) {
			BeanDefinitionBuilder keyPairFactoryBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(KeyPairFactoryBean.class);
			keyPairFactoryBeanBuilder.addConstructorArgValue(privateKeyResource);
			keyPairFactoryBeanBuilder.addConstructorArgValue(publicKeyResource);
			amazonS3ClientFactoryBeanBuilder.addPropertyValue("keyPair", keyPairFactoryBeanBuilder.getBeanDefinition());
			keyPairResourceSet = true;
		}

		if (keyPairRefSet && keyPairResourceSet) {
			parserContext.getReaderContext().error("'key-ref' and 'key-resource' are not allowed together in the same 'key-pair' element.", encryptionKeyElement);
		}

		if (!keyPairRefSet && !keyPairResourceSet) {
			parserContext.getReaderContext().error("Either attribute 'key-ref' or 'key-resource' must be defined on element 'key-pair'.", encryptionKeyElement);
		}
	}

	private static void parseSymmetricKey(ParserContext parserContext, BeanDefinitionBuilder amazonS3ClientFactoryBeanBuilder, Element encryptionKeyElement) {

		boolean symmetricKeyRefSet = false;
		String symmetricKeyRef = encryptionKeyElement.getAttribute("key-ref");
		if (StringUtils.hasText(symmetricKeyRef)) {
			amazonS3ClientFactoryBeanBuilder.addPropertyReference("symmetricKeyRef", symmetricKeyRef);
			symmetricKeyRefSet = true;
		}

		String password = encryptionKeyElement.getAttribute("password");
		String salt = encryptionKeyElement.getAttribute("salt");
		boolean symmetricKeyResourceSet = false;
		if (StringUtils.hasText(password) && StringUtils.hasText(salt)) {
			BeanDefinitionBuilder symmetricKeyFactoryBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(SymmetricKeyFactoryBean.class);
			symmetricKeyFactoryBeanBuilder.addConstructorArgValue(password);
			symmetricKeyFactoryBeanBuilder.addConstructorArgValue(salt);
			amazonS3ClientFactoryBeanBuilder.addPropertyValue("symmetricKey", symmetricKeyFactoryBeanBuilder.getBeanDefinition());
			symmetricKeyResourceSet = true;
		}

		if (symmetricKeyRefSet && symmetricKeyResourceSet) {
			parserContext.getReaderContext().error("'key-ref' and 'key-resource' are not allowed together in the same 'symmetric-key' element.", encryptionKeyElement);
		}

		if (!symmetricKeyRefSet && !symmetricKeyResourceSet) {
			parserContext.getReaderContext().error("Either attribute 'key-ref' or 'key-resource' must be defined on element 'symmetric-key'.", encryptionKeyElement);
		}
	}
}