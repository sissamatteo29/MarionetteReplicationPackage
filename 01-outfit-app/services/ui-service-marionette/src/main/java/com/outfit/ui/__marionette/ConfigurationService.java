package com.outfit.ui.__marionette;

import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

    private ConfigurationReader reader;

    public ConfigurationService(ConfigurationReader reader) {
        this.reader = reader;
    }

    public String readConfiguration() {
        return reader.readMarionetteConfiguration();
    }
}
