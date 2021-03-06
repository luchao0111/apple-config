package com.appleframework.config.springboot;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.appleframework.config.core.PropertyConfigurer;
import com.appleframework.config.core.factory.ConfigurerFactory;
import com.appleframework.config.springboot.utils.YamlLoaderUtils;

public class ExtendPropertySourceLoader implements PropertySourceLoader, PriorityOrdered, DisposableBean  {

	private ConfigurerFactory configurerFactory = null;

	@Override
	public String[] getFileExtensions() {
		return new String[] { "properties", "yml", "yaml" };
	}
	
	private void init(Resource resource, Properties properties) throws IOException {
		PropertyConfigurer.merge(properties);

		//load by spi
		try {
			ServiceLoader<ConfigurerFactory> serviceLoader = ServiceLoader.load(ConfigurerFactory.class);
	        Iterator<ConfigurerFactory> iterator = serviceLoader.iterator();
	        if(iterator.hasNext()){
	        	configurerFactory = iterator.next();
	        }
		} catch (Exception e) {
			//load by class.forName
			try {
				Class<?> clazz = Class.forName("com.appleframework.config.PropertyConfigurerFactory");
				configurerFactory = (ConfigurerFactory) clazz.newInstance();
			} catch (Exception e1) {
				//return null;
			}
		}
		
		configurerFactory.setSpringboot(true);
		configurerFactory.init();
	}
	
	private boolean isPropertiesResource(String name) {
		if(name.indexOf("properties") > -1) {
			return true;
		}
		else {
			return false;
		}
	}

	public PropertySource<?> load(String name, Resource resource, String profile) throws IOException {
		if (null == profile) {
			Properties properties = new Properties();
			if(this.isPropertiesResource(name)) {
				properties = PropertiesLoaderUtils.loadProperties(resource);
			}
			else {
				properties = YamlLoaderUtils.loadProperties(resource);
			}
			init(resource, properties);
			
			Map<String, Properties> remotePropsMap = configurerFactory.getAllRemoteProperties();
			if(null != remotePropsMap && remotePropsMap.size() > 0) {
				for (Map.Entry<String, Properties> prop : remotePropsMap.entrySet()) {
					Set<Entry<Object, Object>> entrySet = prop.getValue().entrySet();
					String namespace = prop.getKey();
					for (Entry<Object, Object> entry : entrySet) {
						// local configurer first
						if (configurerFactory.isRemoteFirst() == false ) {
							if(properties.containsKey(entry.getKey()) || properties.containsKey(namespace + "." + entry.getKey())) {
								continue;
							}
						}
						properties.put(entry.getKey(), entry.getValue());
						properties.put(namespace + "." + entry.getKey(), entry.getValue());
						PropertyConfigurer.add(entry.getKey().toString(), entry.getValue().toString());
						PropertyConfigurer.add(namespace, entry.getKey().toString(), entry.getValue().toString());
					}
			    }
			}

			configurerFactory.onLoadFinish(properties);

			if (!properties.isEmpty()) {
				if(this.isPropertiesResource(name)) {
					return new PropertiesPropertySource(name, properties);
				}
				else {
			        return new YamlPropertySourceLoader().load(resource.getFilename(), resource).get(0);

				}
			}
		}
		return null;
	}
	
	public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
		Properties properties = new Properties();
		if(this.isPropertiesResource(name)) {
			properties = PropertiesLoaderUtils.loadProperties(resource);
		}
		else {
			properties = YamlLoaderUtils.loadProperties(resource);
		}
		
		init(resource, properties);
		
		List<PropertySource<?>> list = new ArrayList<PropertySource<?>>();
		
		Map<String, Properties> remotePropsMap = configurerFactory.getAllRemoteProperties();
		if(null != remotePropsMap && remotePropsMap.size() > 0) {
			for (Map.Entry<String, Properties> prop : remotePropsMap.entrySet()) {
				Set<Entry<Object, Object>> entrySet = prop.getValue().entrySet();
				String namespace = prop.getKey();
				for (Entry<Object, Object> entry : entrySet) {
					// local configurer first
					if (configurerFactory.isRemoteFirst() == false ) {
						if(properties.containsKey(entry.getKey()) || properties.containsKey(namespace + "." + entry.getKey())) {
							continue;
						}
					}
					properties.put(entry.getKey(), entry.getValue());
					properties.put(namespace + "." + entry.getKey(), entry.getValue());
					PropertyConfigurer.add(entry.getKey().toString(), entry.getValue().toString());
					PropertyConfigurer.add(namespace, entry.getKey().toString(), entry.getValue().toString());
				}
		    }
		}

		configurerFactory.onLoadFinish(properties);

		if (!properties.isEmpty()) {
			if(this.isPropertiesResource(name)) {
				list.add(new PropertiesPropertySource(name, properties));
			}
			else {
				list.addAll(new YamlPropertySourceLoader().load(resource.getFilename(), resource));
			}
		}
		return list;
	
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}

	@Override
	public void destroy() throws Exception {
		configurerFactory.close();
	}

}
