package com.appleframework.config;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appleframework.config.core.Constants;
import com.appleframework.config.core.EnvConfigurer;
import com.appleframework.config.core.PropertyConfigurer;
import com.appleframework.config.core.factory.BaseConfigurerFactory;
import com.appleframework.config.core.factory.ConfigurerFactory;
import com.appleframework.config.core.util.StringUtils;
import com.taobao.diamond.manager.DiamondManager;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.impl.DefaultDiamondManager;

public class PropertyConfigurerFactory extends BaseConfigurerFactory implements ConfigurerFactory {

	private static Logger logger = LoggerFactory.getLogger(PropertyConfigurerFactory.class);
	
	private static String KEY_DEPLOY_GROUP     = "deploy.group";
	private static String KEY_DEPLOY_DATAID    = "deploy.dataId";
	private static String KEY_DEPLOY_CONF_HOST = "deploy.confHost";
	
	private DiamondManager manager;
	
	public PropertyConfigurerFactory() {
		
	}
	
	public PropertyConfigurerFactory(Properties props) {
		convertLocalProperties(props);
	}
	
	public void init() {
		
		Version.logVersion();
				
		initSystemProperties();

		initEventListener();
		
		initDiamondManager();
	}
	
	private void initDiamondManager() {
		
		if (!isLoadRemote()) {
			return;
		}
		
		String group = PropertyConfigurer.getString(KEY_DEPLOY_GROUP);
		String dataId = PropertyConfigurer.getString(KEY_DEPLOY_DATAID);
		
		String confHost = PropertyConfigurer.getString(KEY_DEPLOY_CONF_HOST);
		if (null != confHost) {
			com.taobao.diamond.common.Constants.DEFAULT_DOMAINNAME = confHost;
			com.taobao.diamond.common.Constants.DAILY_DOMAINNAME   = confHost;
		}

		if(null == group) {
			group = this.getDeployEnv();
		}
		if(null == dataId) {
			dataId = this.getApplicationName();
		}
			
		logger.warn("配置项：group=" + group);
		logger.warn("配置项：dataId=" + dataId);

		if (!StringUtils.isEmpty(group) && !StringUtils.isEmpty(dataId)) {
			
			ManagerListener springMamagerListener = new ManagerListener() {

				public Executor getExecutor() {
					return null;
				}

				public void receiveConfigInfo(String configInfo) {
					// 客户端处理数据的逻辑
					logger.warn("已改动的配置：\n" + configInfo);
					try {
						PropertyConfigurer.load(configInfo);
					} catch (Exception e) {
						logger.error(e.getMessage());
						return;
					}					
					//事件触发
					notifyPropertiesChanged(PropertyConfigurer.getProps());
				}
			};
			manager = new DefaultDiamondManager(group, dataId, springMamagerListener);
		}
	}
	
	public Properties getRemoteProperties() {
		Properties properties = new Properties();
		if (!isLoadRemote() || null == manager) {
			return properties;
		}
		try {
			String configInfo = manager.getAvailableConfigureInfomation(30000);
			logger.warn("配置项内容: \n" + configInfo);
			if (!StringUtils.isEmpty(configInfo)) {
				properties.load(new StringReader(configInfo));
			} else {
				logger.error("在配置管理中心找不到配置信息");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return properties;
	}
	
	@Override
	public String getRemoteConfigInfo(String namespace) {
		if (!isLoadRemote() || null == manager) {
			return null;
		}
		try {
			String configInfo = manager.getAvailableConfigureInfomation(30000);
			logger.warn("配置项内容: \n" + configInfo);
			return configInfo;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;
	}
	
	public Properties getRemoteProperties(String namespace) {
		return this.getRemoteProperties();
	}

	@Override
	public Map<String, Properties> getAllRemoteProperties() {
		Properties props = this.getRemoteProperties(null);
		Map<String, Properties> propsMap = new HashMap<String, Properties>();
		propsMap.put(Constants.KEY_NAMESPACE, props);
		return propsMap;
	}
	
	private String getDeployEnv() {
		String env = System.getProperty(Constants.KEY_DEPLOY_ENV);
		if (StringUtils.isEmpty(env)) {
			env = System.getProperty(Constants.KEY_ENV);
			if (StringUtils.isEmpty(env)) {
				env = EnvConfigurer.env;
				if (StringUtils.isEmpty(env)) {
					env = PropertyConfigurer.getString(Constants.KEY_DEPLOY_ENV);
				}
			}
		}
		return env;
	}

	@Override
	public void close() {
		if(null != manager) {
			manager.close();
		}
	}
	
	private String getApplicationName() {
		String appName = PropertyConfigurer.getString("spring.application.name");
		if(null == appName) {
			appName = PropertyConfigurer.getString("application.name");
		}
		return appName;
	}

	

	
}