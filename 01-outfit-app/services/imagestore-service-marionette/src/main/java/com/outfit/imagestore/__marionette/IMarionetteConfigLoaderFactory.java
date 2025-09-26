package com.outfit.imagestore.__marionette;

public interface IMarionetteConfigLoaderFactory {

    public IMarionetteConfigParser generateParser();

    public IMarionetteConfigCreator generateConfigCreator();
}
