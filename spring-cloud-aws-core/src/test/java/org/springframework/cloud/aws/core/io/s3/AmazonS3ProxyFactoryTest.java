/*
 * Copyright 2013-2014 the original author or authors.
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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.object.IsCompatibleType.typeCompatibleWith;
import static org.mockito.Mockito.mock;

/**
 * @author Greg Turnquist
 */
public class AmazonS3ProxyFactoryTest {

    @Test
    public void verifyBasicAdvice() throws Exception {

        AmazonS3 amazonS3 = mock(AmazonS3.class);
        assertThat(AopUtils.isAopProxy(amazonS3), is(false));

        AmazonS3 proxy = AmazonS3ProxyFactory.createProxy(amazonS3);
        assertThat(AopUtils.isAopProxy(proxy), is(true));

        Advised advised = (Advised) proxy;
        assertThat(advised.getAdvisors().length, is(1));
        assertThat(advised.getAdvisors()[0].getAdvice(),
                instanceOf(AmazonS3ProxyFactory.AmazonRedirectInterceptor.class));
        assertThat(AopUtils.isAopProxy(advised.getTargetSource().getTarget()), is(false));
    }

    @Test
    public void verifyDoubleWrappingHandled() throws Exception {

        AmazonS3 amazonS3 = mock(AmazonS3.class);

        AmazonS3 proxy = AmazonS3ProxyFactory.createProxy(AmazonS3ProxyFactory.createProxy(amazonS3));
        assertThat(AopUtils.isAopProxy(proxy), is(true));

        Advised advised = (Advised) proxy;
        assertThat(advised.getAdvisors().length, is(1));
        assertThat(advised.getAdvisors()[0].getAdvice(),
                instanceOf(AmazonS3ProxyFactory.AmazonRedirectInterceptor.class));
        assertThat(AopUtils.isAopProxy(advised.getTargetSource().getTarget()), is(false));
    }

    @Test
    public void verifyPolymorphicHandling() {

        AmazonS3 amazonS3 = mock(AmazonS3.class);
        AmazonS3 proxy1 = AmazonS3ProxyFactory.createProxy(amazonS3);

        assertThat(proxy1.getClass(), typeCompatibleWith(AmazonS3.class));
        assertThat(proxy1.getClass(), not(typeCompatibleWith(AmazonS3Client.class)));

        AmazonS3 amazonS3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();
        AmazonS3 proxy2 = AmazonS3ProxyFactory.createProxy(amazonS3Client);

        assertThat(proxy2.getClass(), typeCompatibleWith(AmazonS3.class));
        assertThat(proxy2.getClass(), typeCompatibleWith(AmazonS3Client.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyAddingRedirectAdviceToExistingProxy() {

        AmazonS3 amazonS3 = mock(AmazonS3.class);

        ProxyFactory factory = new ProxyFactory(amazonS3);
        factory.addAdvice(new TestAdvice());
        AmazonS3 proxy1 = (AmazonS3) factory.getProxy();

        assertThat(((Advised) proxy1).getAdvisors().length, is(1));

        AmazonS3 proxy2 = AmazonS3ProxyFactory.createProxy(proxy1);
        Advised advised = (Advised) proxy2;

        assertThat(advised.getAdvisors().length, is(2));

        List<Class<? extends MethodInterceptor>> advisorClasses = new ArrayList<>();
        for (Advisor advisor : advised.getAdvisors()) {
            advisorClasses.add(((MethodInterceptor) advisor.getAdvice()).getClass());
        }
        assertThat(advisorClasses, hasItems(TestAdvice.class, AmazonS3ProxyFactory.AmazonRedirectInterceptor.class));

    }

    static class TestAdvice implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
            return methodInvocation.proceed();
        }
    }
}
