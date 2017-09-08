package com.appleframework.config.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.appleframework.config.core.util.ObjectUtils;
import com.appleframework.config.core.util.StringUtils;

public class PropertyConfigurer {
	
	private static Logger logger = Logger.getLogger(PropertyConfigurer.class);
	
	private static Properties props = new Properties();
	
	public static Properties getProps() {
		return props;
	}

	public static void load(StringReader reader){
		try {
			props.load(reader);
		} catch (IOException e) {			
			logger.error(e);
		}
	}
	
	public static void load(InputStream inputStream){
		try {
			props.load(inputStream);
		} catch (IOException e) {			
			logger.error(e);
		}
	}
	
	public static void load(Properties defaultProps){
		convertProperties(defaultProps);
	}
	
	public static void setProperty(String key, String value) {
		try {
			props.setProperty(key, value);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public static void put(Object key, Object value) {
		try {
			props.put(key, value);
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	/**
	 * Convert the given merged properties, converting property values
	 * if necessary. The result will then be processed.
	 * <p>The default implementation will invoke {@link #convertPropertyValue}
	 * for each property value, replacing the original with the converted value.
	 * @param defaultProps the Properties to convert
	 * @see #processProperties
	 */
	public static void convertProperties(Properties defaultProps) {
		Enumeration<?> propertyNames = defaultProps.propertyNames();
		while (propertyNames.hasMoreElements()) {
			String propertyName = (String) propertyNames.nextElement();
			String propertyValue = defaultProps.getProperty(propertyName);
			if (ObjectUtils.isNotEmpty(propertyName)) {
				props.setProperty(propertyName, propertyValue);
			}
		}
	}

	public static Object getProperty(String key) {
		return props.get(key);
	}
	
	public static String getValue(String key) {
		Object object = getProperty(key);
		if(null != object) {
			return (String)object;
		}
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static String getValue(String key, String defaultValue) {
		Object object = getProperty(key);
		if(null != object) {
			return (String)object;
		}
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultValue;
		}
	}
	
	public static String getString(String key) {
		Object object = getProperty(key);
		if(null != object) {
			return (String)object;
		}
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static String getString(String key, String defaultString) {
		Object object = getProperty(key);
		if(null != object) {
			return (String)object;
		}
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultString;
		}
	}
	
	public static Long getLong(String key) {
		Object object = getProperty(key);
		if(null != object)
			return Long.parseLong(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static Long getLong(String key, long defaultLong) {
		Object object = getProperty(key);
		if(null != object)
			return Long.parseLong(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultLong;
		}
	}
	
	public static Integer getInteger(String key) {
		Object object = getProperty(key);
		if(null != object) {
			return Integer.parseInt(object.toString());
		}
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static Integer getInteger(String key, int defaultInt) {
		Object object = getProperty(key);
		if(null != object) {
			return Integer.parseInt(object.toString());
		}
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultInt;
		}
	}
	
	public static String getString(String key, Object[] array) {
		String message = getValue(key);
		if(null != message) {
			return MessageFormat.format(message, array);  
		}
		else {
			return null;
		}
	}
	
	public static String getValue(String key, Object... array) {
		String message = getValue(key);
		if(null != message) {
			return MessageFormat.format(message, array);  
		}
		else {
			return null;
		}
	}
	
	public static Boolean getBoolean(String key) {
		Object object = getProperty(key);
		if(null != object)
			return Boolean.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static Boolean getBoolean(String key, boolean defaultBoolean) {
		Object object = getProperty(key);
		if(null != object)
			return Boolean.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultBoolean;
		}
	}
	
	public static Double getDouble(String key) {
		Object object = getProperty(key);
		if(null != object)
			return Double.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static Double getDouble(String key, double defaultDouble) {
		Object object = getProperty(key);
		if(null != object)
			return Double.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultDouble;
		}
	}
	
	public static Short getShort(String key) {
		Object object = getProperty(key);
		if(null != object)
			return Short.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static Short getShort(String key, short defaultShort) {
		Object object = getProperty(key);
		if(null != object)
			return Short.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultShort;
		}
	}
	
	public static Float getFloat(String key) {
		Object object = getProperty(key);
		if(null != object)
			return Float.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return null;
		}
	}
	
	public static Float getFloat(String key, float defaultFloat) {
		Object object = getProperty(key);
		if(null != object)
			return Float.valueOf(object.toString());
		else {
			logger.warn("配置项为" + key + "的配置未在配置中心或项目中添加或设置的内容为空");
			return defaultFloat;
		}
	}
	
	public synchronized static void merge(Properties properties){
		if (properties == null || properties.isEmpty())
			return;
		props.putAll(properties);
	}
	
	public synchronized static void add(String key, String value) {
		if (StringUtils.isEmptyString(key) || StringUtils.isEmptyString(value))
			return;
		props.put(key, value);
	}
	
	public static boolean containsProperty(String key) {
		return props.containsKey(key);
	}
	
	public static boolean containsKey(String key) {
		return props.containsKey(key);
	}
	
}