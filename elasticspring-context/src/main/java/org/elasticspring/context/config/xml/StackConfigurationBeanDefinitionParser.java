package org.elasticspring.context.config.xml;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

/**
 * Parser for the {@code <els-context:stack-configuration />} element.
 *
 * @author Christian Stettler
 */
class StackConfigurationBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	private static final String STACK_RESOURCE_REGISTRY_FACTORY_BEAN_CLASS_NAME = "org.elasticspring.core.env.stack.config.StackResourceRegistryFactoryBean";
	private static final String STATIC_STACK_NAME_PROVIDER_CLASS_NAME = "org.elasticspring.core.env.stack.config.StaticStackNameProvider";

	private static final String STACK_NAME_ATTRIBUTE_NAME = "stack-name";

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String stackName = element.getAttribute(STACK_NAME_ATTRIBUTE_NAME);

		builder.addConstructorArgValue(buildAmazonCloudFormationClientBeanDefinition());
		builder.addConstructorArgValue(buildStaticStackNameProviderBeanDefinitionBuilder(stackName));
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		return element.getAttribute(STACK_NAME_ATTRIBUTE_NAME);
	}

	@Override
	protected String getBeanClassName(Element element) {
		return STACK_RESOURCE_REGISTRY_FACTORY_BEAN_CLASS_NAME;
	}

	private static AbstractBeanDefinition buildStaticStackNameProviderBeanDefinitionBuilder(String stackName) {
		BeanDefinitionBuilder staticStackNameProviderBeanDefinitionBuilder = genericBeanDefinition(STATIC_STACK_NAME_PROVIDER_CLASS_NAME);
		staticStackNameProviderBeanDefinitionBuilder.addConstructorArgValue(stackName);

		return staticStackNameProviderBeanDefinitionBuilder.getBeanDefinition();
	}

	private static AbstractBeanDefinition buildAmazonCloudFormationClientBeanDefinition() {
		BeanDefinitionBuilder amazonCloudFormationClientBeanDefinitionBuilder = genericBeanDefinition(AmazonCloudFormationClient.class);
		amazonCloudFormationClientBeanDefinitionBuilder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

		return amazonCloudFormationClientBeanDefinitionBuilder.getBeanDefinition();
	}

}
