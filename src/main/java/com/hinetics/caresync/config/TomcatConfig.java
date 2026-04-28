package com.hinetics.caresync.config; // Updated to your package

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            // This is the direct fix for your FileCountLimitExceededException
            // Raising it to 1000 ensures even the most complex Outlook signatures pass.
            connector.setMaxPartCount(1000);

            // maxPostSize: Total size of the POST request.
            // 2GB (from your snippet) is very safe, but ensure your server has the RAM.
            connector.setProperty("maxPostSize", String.valueOf(2L * 1024 * 1024 * 1024));

            // maxSwallowSize: The amount of data Tomcat will "swallow" (read and discard)
            // if an error occurs, preventing the connection from hanging.
            connector.setProperty("maxSwallowSize", String.valueOf(2L * 1024 * 1024 * 1024));

            // Added maxParameterCount to keep everything in sync
            connector.setMaxParameterCount(1000);
        });
    }
}