package foo;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class DatasourceBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof DataSource) {
			// We create a "wrapper around the original" as suggested by BeanPostProcessor's javadoc,
			// Note: we do not use ProxyFactory.setProxyTargetClass(true) to use a CGLIB proxy because
			// 1) Calling setProxyTargetClass is not needed in Spring-boot 1.x.
			//    And JMX by default is not a good reason to change that in Spring-boot 2.x.
			// 2) Or the datasource proxy may not be created by the application's code and may not be created by Spring neither
			// 3) Or CGLIB may not be available in this application
			// 4) and the javadoc only suggest to create here a "wrapper around the original",
			// so why would it be needed to use CGLIB here to create a proxy of a standard DataSource ?
			// (the fact that it is a HikariDataSource and that Spring-boot registers into JMX is unknown here
			// by separation of concerns and should be transparent for the application as much as possible)
			final ProxyFactory factory = new ProxyFactory(bean);
			factory.addAdvice(new DataSourceInterceptor());
			return factory.getProxy();
		}
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	private static class DataSourceInterceptor implements MethodInterceptor {
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			long start = System.currentTimeMillis();
			try {
				return invocation.proceed();
			} finally {
				long duration = System.currentTimeMillis() - start;
				System.out.println(
						"************************" + invocation.getMethod().getName() + " (" + duration + " ms)");
			}
		}
	}
}
