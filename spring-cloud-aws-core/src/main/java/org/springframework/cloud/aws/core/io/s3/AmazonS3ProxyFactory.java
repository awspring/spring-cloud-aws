/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.aws.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.util.StringInputStream;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Proxy to wrap an {@link AmazonS3} handler and handle redirects wrapped inside {@link AmazonS3Exception}.
 *
 * @author Greg Turnquist
 * @since 1.1
 */
public class AmazonS3ProxyFactory {

    private static final Logger log = LoggerFactory.getLogger(AmazonS3ProxyFactory.class);

    /**
     * Take any {@link AmazonS3} and wrap it in a Spring AOP proxy to handle redirects.
     *
     * @param amazonS3
     * @return
     */
    static AmazonS3 createProxy(AmazonS3 amazonS3) {

        /**
         * Is this already a proxy? If so, don't wrap it again.
         */
        if (AopUtils.isAopProxy(amazonS3)) {

            Advised advised = (Advised) amazonS3;

            /**
             * Already advised by {@link AmazonRedirectInterceptor}?
             */
            for (Advisor advisor : advised.getAdvisors()) {
                if (ClassUtils.isAssignableValue(AmazonRedirectInterceptor.class, advisor.getAdvice())) {
                    return amazonS3;
                }
            }

            /**
             * If not, then add it.
             */
            try {
                advised.addAdvice(new AmazonRedirectInterceptor((AmazonS3) advised.getTargetSource().getTarget()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return amazonS3;
        }

        ProxyFactory factory = new ProxyFactory(amazonS3);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new AmazonRedirectInterceptor(amazonS3));

        return (AmazonS3) factory.getProxy();
    }

    /**
     * Listen for {@link AmazonS3} exceptions, handle the redirect, and try again.
     * Otherwise, rethrow the error.
     * NOTE: This has the side effect of updating the S3 client for subsequent calls.
     */
    static class AmazonRedirectInterceptor implements MethodInterceptor {

        private final AmazonS3 amazonS3;

        public AmazonRedirectInterceptor(AmazonS3 amazonS3) {
            this.amazonS3 = amazonS3;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {

            try {
                return invocation.proceed();
            } catch (AmazonS3Exception e) {
                if (301 == e.getStatusCode()) {
                    handleAmazonS3Redirect(this.amazonS3, e);
                    return invocation.proceed();
                } else {
                    throw e;
                }
            }
        }

        /**
         * Handle a 301 Redirect from Amazon S3 by parsing the endpoint.
         *
         * @param amazonS3
         * @param e
         */
        private void handleAmazonS3Redirect(AmazonS3 amazonS3, AmazonS3Exception e) {

            try {
                Document errorResponseDoc = DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new StringInputStream(e.getErrorResponseXml()));

                XPathExpression endpointXpathExtr = XPathFactory.newInstance().newXPath().compile("/Error/Endpoint");

                amazonS3.setEndpoint(endpointXpathExtr.evaluate(errorResponseDoc));
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }

        }
    }

}
