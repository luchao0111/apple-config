package com.appleframework.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appleframework.config.core.Constants;
import com.appleframework.config.core.PropertyConfigurer;
import com.appleframework.config.core.factory.BaseConfigurerFactory;
import com.appleframework.config.core.factory.ConfigurerFactory;
import com.appleframework.config.core.util.StringUtils;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;

public class PropertyConfigurerFactory extends BaseConfigurerFactory implements ConfigurerFactory {

	private static Logger logger = LoggerFactory.getLogger(PropertyConfigurerFactory.class);
	
	private static String KEY_DEPLOY_APP_ID      = "app.id";
	private static String KEY_DEPLOY_META_URL    = "apollo.meta";
	private static String KEY_DEPLOY_NAMESPACES  = "apollo.bootstrap.namespaces";
	private static String KEY_DEPLOY_REFRESH_INT = "apollo.refreshInterval";
	
	private Map<String, Config> configMap = new HashMap<String, Config>();
		
	public PropertyConfigurerFactory() {
		
	}
	
	public PropertyConfigurerFactory(Properties props) {
		convertLocalProperties(props);
	}
	
	public void init() {
		
		Version.logVersion();
				
		initSystemProperties();

		initEventListener();
		
		initApolloConfig();
	}
	
	private void initApolloConfig() {
		
		if (!isLoadRemote()) {
			return;
		}
		
		//app.id
		String appId = this.getDeployAppId();
		if(null == appId) {
			appId = PropertyConfigurer.getString(KEY_DEPLOY_APP_ID);
			if(null != appId) {
				this.setDeployAppId(appId);
			}
			else {
				logger.warn("app.id is not set on this project");
			}
		}
		logger.warn("配置项：app.id=" + this.getDeployAppId());

		//env
		String env = this.getDeployEnv();
		if(null == env) {
			env = PropertyConfigurer.getString(Constants.KEY_ENV);
			if(null != env) {
				this.setDeployEnv(env);
			}
			else {
				logger.warn("env is not set on this project");
			}
		}
		logger.warn("配置项：env=" + this.getDeployEnv());
		
		//apollo.meta
		String meta = this.getDeployMeta();
		if(null == meta) {
			meta = PropertyConfigurer.getString(KEY_DEPLOY_META_URL);
			if(null != meta) {
				this.setDeployMeta(meta);
			}
			else {
				logger.warn("apollo.meta is not set on this project");
			}
		}
		logger.warn("配置项：apollo.meta=" + this.getDeployMeta());
		
		//apollo.refreshInterval
		String refreshInterval = this.getRefreshInterval();
		if(null == refreshInterval) {
			refreshInterval = PropertyConfigurer.getString(KEY_DEPLOY_REFRESH_INT);
			if(null != refreshInterval) {
				this.setRefreshInterval(refreshInterval);
			}
			else {
				this.setRefreshInterval("1");
			}
		}
		logger.warn("配置项：apollo.refreshInterval=" + this.getRefreshInterval());
				
		String namespaces = this.getDeployNamespaces();
		
		ConfigChangeListener changeListener = new ConfigChangeListener() {
			@Override
			public void onChange(ConfigChangeEvent changeEvent) {
		        logger.info("Changes for namespace {}", changeEvent.getNamespace());
		        logger.warn("已改动的配置：");
		        for (String key : changeEvent.changedKeys()) {
		        	ConfigChange change = changeEvent.getChange(key);
		        	logger.info("Change - key: {}, oldValue: {}, newValue: {}, changeType: {}",
		        			change.getPropertyName(), change.getOldValue(), change.getNewValue(),
		        			change.getChangeType());
		        	// 客户端处理数据的逻辑
					System.out.println(change.getPropertyName() + "=" + change.getNewValue());
					try {
						PropertyConfigurer.setProperty(change.getPropertyName(), change.getNewValue());
						PropertyConfigurer.setProperty(change.getNamespace() + "." + change.getPropertyName(), change.getNewValue());
						PropertyConfigurer.setProperty(change.getNamespace(), change.getPropertyName(), change.getNewValue());
					} catch (Exception e) {
						logger.error(e.getMessage());
						return;
					}					
					//事件触发
					notifyPropertiesChanged(PropertyConfigurer.getPropsMap());
		        }
			}
		};
		Config applicationConfig = ConfigService.getAppConfig();
		applicationConfig.addChangeListener(changeListener);
		configMap.put("application", applicationConfig);
		
		if (!StringUtils.isEmpty(namespaces)) {
			String[] namespaceArr = namespaces.trim().split(",");
			for (String namespace : namespaceArr) {
				if(!namespace.equals("application")) {
					Config namespaceConfig = ConfigService.getConfig(namespace);
					namespaceConfig.addChangeListener(changeListener);
					configMap.put(namespace, namespaceConfig);
				}
			}
		}
	}
	
	public Properties getRemoteProperties(String namespace) {
		Properties properties = new Properties();
		Config config = ConfigService.getConfig(namespace);
		for (String key : config.getPropertyNames()) {
			String value = config.getProperty(key, null);
			if(null != value) {
				properties.put(key, value);
			}
		}
		return properties;
	}
	
	@Override
	public Map<String, Properties> getAllRemoteProperties() {
		Map<String, Properties> propsMap = new HashMap<String, Properties>();
		if (!isLoadRemote() || configMap.size() == 0) {
			return propsMap;
		}
		try {
			for (Map.Entry<String, Config> map : configMap.entrySet()) {
		        String namespace = map.getKey();
		        logger.warn("命名空间" + namespace + "配置项内容: ");
		        Config config = map.getValue();
		        Set<String> propertyNames = config.getPropertyNames();
		        Properties properties = new Properties();
				for (String key : propertyNames) {
					String value = config.getProperty(key, null);
					if(null != value) {
						properties.put(key, value);
						System.out.println(key + "=" + value);
					}
				}
				propsMap.put(namespace, properties);
				System.out.println();
		    }
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return propsMap;
	}
	
	@Override
	public String getRemoteConfigInfo(String namespace) {
		return null;
	}

	private String getDeployEnv() {
		return System.getProperty(Constants.KEY_ENV);
	}
		
	private String getDeployMeta() {
		return System.getProperty(KEY_DEPLOY_META_URL);		
	}
	
	private String getDeployNamespaces() {
		String namespaces = System.getProperty(KEY_DEPLOY_NAMESPACES);
		if(null == namespaces) {
			namespaces = PropertyConfigurer.getString(KEY_DEPLOY_NAMESPACES);
		}
		return namespaces;
	}
	
	private String getRefreshInterval() {
		return System.getProperty(KEY_DEPLOY_REFRESH_INT);
	}
	
	//set
	private String setDeployEnv(String env) {
		return System.setProperty(Constants.KEY_ENV, env);
	}
	
	private String setDeployAppId(String appId) {
		return System.setProperty(KEY_DEPLOY_APP_ID, appId);
	}
	
	private String setDeployMeta(String meta) {
		return System.setProperty(KEY_DEPLOY_META_URL, meta);		
	}
		
	private String setRefreshInterval(String rRefreshInterval) {
		return System.setProperty(KEY_DEPLOY_REFRESH_INT, rRefreshInterval);
	}

	@Override
	public void close() {
	}
	
	private String getDeployAppId() {
		String appId = System.getProperty(KEY_DEPLOY_APP_ID);
		if(null == appId) {
			appId = System.getProperty("application.name");
			if(null == appId) {
				appId = System.getProperty("spring.application.name");
			}
		}
		return appId;
	}
	
}