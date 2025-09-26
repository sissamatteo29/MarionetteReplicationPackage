package com.outfit.imagestore.__marionette;

public class XMLMarionetteConfigLoaderFactory implements IMarionetteConfigLoaderFactory {

    @Override
    public IMarionetteConfigParser generateParser() {
        return new XMLMarionetteConfigParser();
    }

    @Override
    public IMarionetteConfigCreator generateConfigCreator() {
        return new XMLMarionetteConfigCreator();
    }
}
