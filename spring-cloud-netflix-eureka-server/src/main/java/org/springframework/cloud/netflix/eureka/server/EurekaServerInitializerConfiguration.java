package org.springframework.cloud.netflix.eureka.server;

import javax.servlet.ServletContext;

import com.netflix.eureka.EurekaServerConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaServerStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.context.ServletContextAware;

/**
 * Eureka服务端初始化配置类。
 * SmartLifecycle接口
 *
 * @author Dave Syer
 */
@Configuration(proxyBeanMethods = false)
public class EurekaServerInitializerConfiguration implements ServletContextAware, SmartLifecycle, Ordered {

	private static final Log log = LogFactory.getLog(EurekaServerInitializerConfiguration.class);

	@Autowired
	private EurekaServerConfig eurekaServerConfig;

	private ServletContext servletContext;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private EurekaServerBootstrap eurekaServerBootstrap;

	private boolean running;

	private int order = 1;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 服务端启动方法
	 */
	@Override
	public void start() {
		new Thread(() -> {
			try {
				/**
				 * 调用完EurekaServerBootstrap的contextInitialized方法后，则会输出"Started Eureka Server"
				 * 那么我们就可以想象到，contextInitialized方法中保护的就是Eureka服务启动的逻辑
				 * */
				eurekaServerBootstrap.contextInitialized(EurekaServerInitializerConfiguration.this.servletContext);

				log.info("Started Eureka Server");

				// 因为Eureka Server已经启动了，所以发布Eureka Server注册中心启动事件
				publish(new EurekaRegistryAvailableEvent(getEurekaServerConfig()));

				// 设置Eureka Server是运行中
				EurekaServerInitializerConfiguration.this.running = true;

				// 发布Eureka Server启动事件
				publish(new EurekaServerStartedEvent(getEurekaServerConfig()));
			}
			catch (Exception ex) {
				// Help!
				log.error("Could not initialize Eureka servlet context", ex);
			}
		}).start();
	}

	private EurekaServerConfig getEurekaServerConfig() {
		return this.eurekaServerConfig;
	}

	private void publish(ApplicationEvent event) {
		this.applicationContext.publishEvent(event);
	}

	@Override
	public void stop() {
		this.running = false;
		eurekaServerBootstrap.contextDestroyed(this.servletContext);
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	/**
	 * true，则表明会在finishRefresh()方法中调用start()方法
	 * @return
	 */
	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
