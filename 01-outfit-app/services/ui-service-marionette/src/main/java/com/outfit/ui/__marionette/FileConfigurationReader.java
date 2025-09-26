package com.outfit.ui.__marionette;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class FileConfigurationReader implements ConfigurationReader {

    @Override
    public String readMarionetteConfiguration() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("marionette.xml")) {
            if (inputStream == null) {
                return "Configuration file 'marionette.xml' not found in classpath";
            }
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Successfully read all bytes for the marionette configuration: ");
            System.out.println(content);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
            return "Impossible to read the configuration file";
        }
    }
}
