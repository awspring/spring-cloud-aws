/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.context.config.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.replaceDefaultCredentialsProvider;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} implementation which
 * parses the &lt;context-credentials/&gt; Element.
 *
 * @author Agim Emruli
 * @since 1.0
 */
class ContextCredentialsBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	// @checkstyle:off
	private static final String STATIC_CREDENTIALS_PROVIDER_BEAN_CLASS_NAME = "com.amazonaws.auth.AWSStaticCredentialsProvider";

	// @checkstyle:on

	// @checkstyle:off
	private static final String INSTANCE_CREDENTIALS_PROVIDER_BEAN_CLASS_NAME = "com.amazonaws.auth.InstanceProfileCredentialsProvider";

	// @checkstyle:on

	// @checkstyle:off
	private static final String PROFILE_CREDENTIALS_PROVIDER_BEAN_CLASS_NAME = "com.amazonaws.auth.profile.ProfileCredentialsProvider";

	// @checkstyle:on

	private static final String ACCESS_KEY_ATTRIBUTE_NAME = "access-key";

	private static final String SECRET_KEY_ATTRIBUTE_NAME = "secret-key";

	private static BeanDefinition getCredentialsProvider(
			String credentialsProviderClassName, Object... constructorArg) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(credentialsProviderClassName);
		for (Object o : constructorArg) {
			beanDefinitionBuilder.addConstructorArgValue(o);
		}

		return beanDefinitionBuilder.getBeanDefinition();
	}

	/**
	 * Creates a bean definition for the credentials object. This methods creates a bean
	 * definition instead of the direct implementation to allow property place holder to
	 * change any place holder used for the access or secret key.
	 * @param credentialsProviderElement - The element that contains the credentials
	 * attributes ACCESS_KEY_ATTRIBUTE_NAME and SECRET_KEY_ATTRIBUTE_NAME
	 * @param parserContext - Used to report any errors if there is no
	 * ACCESS_KEY_ATTRIBUTE_NAME or SECRET_KEY_ATTRIBUTE_NAME available with a valid value
	 * @return - the bean definition with an
	 * {@link com.amazonaws.auth.BasicAWSCredentials} class
	 */
	private static BeanDefinition getCredentials(Element credentialsProviderElement,
			ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition("com.amazonaws.auth.BasicAWSCredentials");
		builder.addConstructorArgValue(getAttributeValue(ACCESS_KEY_ATTRIBUTE_NAME,
				credentialsProviderElement, parserContext));
		builder.addConstructorArgValue(getAttributeValue(SECRET_KEY_ATTRIBUTE_NAME,
				credentialsProviderElement, parserContext));
		return builder.getBeanDefinition();
	}

	private static List<String> getProfileConfiguration(Element element) {
		List<String> constructorArguments = new ArrayList<>(2);
		if (StringUtils.hasText(element.getAttribute("profilePath"))) {
			constructorArguments.add(element.getAttribute("profilePath"));
		}

		if (StringUtils.hasText(element.getAttribute("profileName"))) {
			constructorArguments.add(element.getAttribute("profileName"));
		}
		return constructorArguments;
	}

	/**
	 * Returns the attribute value and reports an error if the attribute value is null or
	 * empty. Normally the reported error leads into an exception which will be thrown
	 * through the {@link org.springframework.beans.factory.parsing.ProblemReporter}
	 * implementation.
	 * @param attribute - The name of the attribute which will be valuated
	 * @param element - The element that contains the attribute
	 * @param parserContext - The parser context used to report errors
	 * @return - The attribute value
	 */
	private static String getAttributeValue(String attribute, Element element,
			ParserContext parserContext) {
		String attributeValue = element.getAttribute(attribute);
		if (!StringUtils.hasText(attributeValue)) {
			parserContext.getReaderContext().error(
					"The '" + attribute + "' attribute must not be empty", element);
		}
		return attributeValue;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition,
			ParserContext parserContext) throws BeanDefinitionStoreException {
		return CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return CredentialsProviderFactoryBean.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		if (parserContext.getRegistry().containsBeanDefinition(
				CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME)) {
			parserContext.getReaderContext()
					.error("Multiple <context-credentials/> detected. "
							+ "The <context-credentials/> is only allowed once per application context",
							element);
		}

		List<Element> elements = DomUtils.getChildElements(element);
		ManagedList<BeanDefinition> credentialsProviders = new ManagedList<>(
				elements.size());

		for (Element credentialsProviderElement : elements) {
			if ("simple-credentials".equals(credentialsProviderElement.getLocalName())) {
				credentialsProviders.add(getCredentialsProvider(
						STATIC_CREDENTIALS_PROVIDER_BEAN_CLASS_NAME,
						getCredentials(credentialsProviderElement, parserContext)));
			}

			if ("instance-profile-credentials"
					.equals(credentialsProviderElement.getLocalName())) {
				credentialsProviders.add(getCredentialsProvider(
						INSTANCE_CREDENTIALS_PROVIDER_BEAN_CLASS_NAME));
			}

			if ("profile-credentials".equals(credentialsProviderElement.getLocalName())) {
				credentialsProviders.add(getCredentialsProvider(
						PROFILE_CREDENTIALS_PROVIDER_BEAN_CLASS_NAME,
						getProfileConfiguration(credentialsProviderElement).toArray()));
			}
		}

		builder.addConstructorArgValue(credentialsProviders);

		replaceDefaultCredentialsProvider(parserContext.getRegistry(),
				CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

	}

}
