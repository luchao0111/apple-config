package com.appleframework.config;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.appleframework.config.core.event.ConfigListener;
import com.appleframework.config.core.factory.ConfigurerFactory;
import com.baidu.disconf.client.DisconfMgr;
import com.baidu.disconf.client.addons.properties.IReloadablePropertiesListener;
import com.baidu.disconf.client.addons.properties.ReconfigurableBean;
import com.baidu.disconf.client.addons.properties.ReloadConfigurationMonitor;
import com.baidu.disconf.client.addons.properties.ReloadablePropertiesBase;

/**
 * A properties factory bean that creates a reconfigurable Properties object.
 * When the Properties' reloadConfiguration method is called, and the file has
 * changed, the properties are read again from the file.
 * <p/>
 * ������ reload bean ���壬�����Զ����� resource Ϊ reload config file
 */
@SuppressWarnings("unused")
public class ReloadablePropertiesFactoryBean extends PropertiesFactoryBean implements DisposableBean, ApplicationContextAware {

	private static ApplicationContext applicationContext;

    protected static final Logger log = LoggerFactory.getLogger(ReloadablePropertiesFactoryBean.class);

    private Resource[] locations;
    private long[] lastModified;
    private List<IReloadablePropertiesListener> preListeners;
    
    private ConfigurerFactory configurerFactory;
    
    private Collection<ConfigListener> eventListeners;
    
    private Collection<String> eventListenerClasss;

	private String eventListenerClass;

	private ConfigListener eventListener;
	
	private boolean loadRemote = true;
	
	public boolean isLoadRemote() {
		return loadRemote;
	}

	public void setLoadRemote(boolean loadRemote) {
		this.loadRemote = loadRemote;
	}

	public void setEventListenerClass(String eventListenerClass) {
		this.eventListenerClass = eventListenerClass;
	}

	public void setEventListener(ConfigListener eventListener) {
		this.eventListener = eventListener;
	}

    /**
     * ������Դ�ļ�
     *
     * @param fileNames
     */
    public void setLocation(final String fileNames) {
        List<String> list = new ArrayList<String>();
        list.add(fileNames);
        setLocations(list);
    }

    /**
     */
    public void setLocations(List<String> fileNames) {
        List<Resource> resources = new ArrayList<Resource>();
        for (String filename : fileNames) {
            // trim
            filename = filename.trim();
            String realFileName = getFileName(filename);
            // register to disconf
            DisconfMgr.getInstance().reloadableScan(realFileName);
            // only properties will reload
            String ext = FilenameUtils.getExtension(filename);
			if (ext.equals("properties")) {
				PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
				try {
					Resource[] resourceList = pathMatchingResourcePatternResolver.getResources(filename);
					for (Resource resource : resourceList) {
						resources.add(resource);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        }
        this.locations = resources.toArray(new Resource[resources.size()]);
        lastModified = new long[locations.length];
        super.setLocations(locations);
    }

    /**
     * get file name from resource
     *
     * @param fileName
     *
     * @return
     */
	private String getFileName(String fileName) {
		if (fileName != null) {
			int index = fileName.indexOf(':');
			if (index < 0) {
				return fileName;
			} else {
				fileName = fileName.substring(index + 1);
				index = fileName.lastIndexOf('/');
				if (index < 0) {
					return fileName;
				} else {
					return fileName.substring(index + 1);
				}

			}
		}
		return null;
	}

    protected Resource[] getLocations() {
        return locations;
    }

    /**
     * listener , ����֪ͨ�ص�
     *
     * @param listeners
     */
    public void setListeners(final List<IReloadablePropertiesListener> listeners) {
        // early type check, and avoid aliassing
        this.preListeners = new ArrayList<IReloadablePropertiesListener>();
        for (Object o : listeners) {
            preListeners.add((IReloadablePropertiesListener) o);
        }
    }

    private ReloadablePropertiesBase reloadableProperties;

    /**
     * @return
     *
     * @throws IOException
     */
    @Override
    protected Properties createProperties() throws IOException {
        return (Properties) createMyInstance();
    }

    /**
     * createInstance ������
     *
     * @throws IOException
     */
    protected Object createMyInstance() throws IOException {
        // would like to uninherit from AbstractFactoryBean (but it's final!)
        if (!isSingleton()) {
            throw new RuntimeException("ReloadablePropertiesFactoryBean only works as singleton");
        }

        // set listener
        reloadableProperties = new ReloadablePropertiesImpl();
        if (preListeners != null) {
            reloadableProperties.setListeners(preListeners);
        }

        // reload
        reload(true);

        // add for monitor
        ReloadConfigurationMonitor.addReconfigurableBean((ReconfigurableBean) reloadableProperties);
        
        return reloadableProperties;
    }

    public void destroy() throws Exception {
        reloadableProperties = null;
    }

    /**
     * �����޸�ʱ�����ж��Ƿ�reload
     *
     * @param forceReload
     *
     * @throws IOException
     */
    protected void reload(final boolean forceReload) throws IOException {
        boolean reload = forceReload;
        for (int i = 0; i < locations.length; i++) {
            Resource location = locations[i];
            File file;
            try {
                file = location.getFile();
            } catch (IOException e) {
                // not a file resource
                // may be spring boot
                log.warn(e.toString());
                continue;
            }
            try {
                long l = file.lastModified();
                if (l > lastModified[i]) {
                    lastModified[i] = l;
                    reload = true;
                }
            } catch (Exception e) {
                // cannot access file. assume unchanged.
                if (log.isDebugEnabled()) {
                    log.debug("can't determine modification time of " + file + " for " + location, e);
                }
            }
        }
        if (reload) {
            doReload();
        }
    }

    /**
     * �����µ�ֵ
     *
     * @throws IOException
     */
	private void doReload() throws IOException {
		Properties mergeProperties = mergeProperties();
		reloadableProperties.setProperties(mergeProperties);
		if (null == configurerFactory) {
			configurerFactory = new PropertyConfigurerFactory(mergeProperties);
			configurerFactory.setLoadRemote(loadRemote);
			configurerFactory.setEventListener(eventListener);
			configurerFactory.setEventListenerClass(eventListenerClass);
			configurerFactory.setEventListenerClasss(eventListenerClasss);
			configurerFactory.setEventListeners(eventListeners);
			configurerFactory.init();
		} else {
			configurerFactory.notifyPropertiesChanged(mergeProperties);
		}
	}

    /**
     * @return
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @SuppressWarnings("static-access")
	@Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setEventListeners(Collection<ConfigListener> eventListeners) {
		this.eventListeners = eventListeners;
	}

	public void setEventListenerClasss(Collection<String> eventListenerClasss) {
		this.eventListenerClasss = eventListenerClasss;
	}
    /**
     * �ص��Լ�
     */
    @SuppressWarnings("unchecked")
	class ReloadablePropertiesImpl extends ReloadablePropertiesBase implements ReconfigurableBean {

		private static final long serialVersionUID = 1L;

		// reload myself
        public void reloadConfiguration() throws Exception {
            ReloadablePropertiesFactoryBean.this.reload(false);
        }
    }

}

