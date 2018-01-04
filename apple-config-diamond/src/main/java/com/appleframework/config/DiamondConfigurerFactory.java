package com.appleframework.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.appleframework.config.core.PropertyConfigurer;

public class DiamondConfigurerFactory extends PropertyConfigurerFactory {

	private static Logger logger = Logger.getLogger(DiamondConfigurerFactory.class);
	
	public DiamondConfigurerFactory() {
		
	}
	
	public DiamondConfigurerFactory(Properties props) {
		convertLocalProperties(props);
	}
	
	public void init() {
		
		// read system.properties
		URL resource = Thread.currentThread().getContextClassLoader().getResource("system.properties");
		if (resource != null) {
			Properties sysConfig = new Properties();
			try {
				sysConfig.load(new FileReader(new File(resource.getPath())));
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			PropertyConfigurer.merge(sysConfig);
		} else {
			logger.warn("[system.properties] is not exist !");
		}
		
		// read application.properties
		resource = Thread.currentThread().getContextClassLoader().getResource("application.properties");
		if (resource != null) {
			Properties sysConfig = new Properties();
			try {
				sysConfig.load(new FileReader(new File(resource.getPath())));
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			PropertyConfigurer.merge(sysConfig);
		} else {
			logger.warn("[application.properties] is not exist !");
		}
		
		super.init();
		
		Properties remoteProperties = super.getAllRemoteProperties();
		if (remoteProperties != null) {
			Set<Entry<Object, Object>> entrySet = remoteProperties.entrySet();
			for (Entry<Object, Object> entry : entrySet) {
				// local configurer first
				if (super.isRemoteFirst() == false && PropertyConfigurer.containsKey(entry.getKey().toString())) {
					logger.info("config[" + entry.getKey() + "] exists in location,skip~");
					continue;
				}
				PropertyConfigurer.add(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		super.onLoadFinish(remoteProperties);
	}	
	
}