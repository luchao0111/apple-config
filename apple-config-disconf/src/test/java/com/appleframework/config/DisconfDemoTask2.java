package com.appleframework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.appleframework.config.core.PropertyConfigurer;

/**
 *
 * @author liaoqiqi
 * @version 2014-6-17
 */
@Service("disconfDemoTask2")
public class DisconfDemoTask2 {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DisconfDemoTask2.class);

    @Autowired
    private AutoService autoService;

    @Autowired
    private AutoService2 autoService2;

    /**
     *
     */
    public int run() {

        try {

            while (true) {

                //
                // service demo
                //


                Thread.sleep(5000);


                //
                // xml demo
                //

                LOGGER.info("autoservice: {}", autoService.getAuto());

                LOGGER.info("autoservice2: {}", autoService2.getAuto2());

                LOGGER.info("PropertyConfigurer: {}", PropertyConfigurer.getProperty("fastdfs.http.url"));
            }

        } catch (Exception e) {

            LOGGER.error(e.toString(), e);
        }

        return 0;
    }
}
