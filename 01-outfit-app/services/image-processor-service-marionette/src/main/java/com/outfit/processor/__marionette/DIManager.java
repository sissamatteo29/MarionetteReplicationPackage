package com.outfit.processor.__marionette;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DIManager {

    @Bean
    public IMarionetteConfigLoaderFactory marionetteConfigLoaderFactory() {
        return new XMLMarionetteConfigLoaderFactory();
    }

    @Bean
    public IMarionetteConfigParser marionetteConfigParser(IMarionetteConfigLoaderFactory loaderFactory) {
        return loaderFactory.generateParser();
    }

    @Bean
    public IMarionetteConfigCreator marionetteConfigCreator(IMarionetteConfigLoaderFactory loaderFactory) {
        return loaderFactory.generateConfigCreator();
    }

    @Bean
    public MarionetteParser marionetteParser(IMarionetteConfigLoaderFactory loaderFactory) {
        return new MarionetteParser("marionette.xml", loaderFactory);
    }

    @Bean
    public ConfigurationReader configurationReader() {
        return new FileConfigurationReader();
    }

    @Bean
    public ConfigurationService configurationService(ConfigurationReader configurationReader) {
        return new ConfigurationService(configurationReader);
    }

    @Bean
    public BehaviourController behaviourController(ConfigurationService configurationService) {
        return new BehaviourController(configurationService);
    }
}
