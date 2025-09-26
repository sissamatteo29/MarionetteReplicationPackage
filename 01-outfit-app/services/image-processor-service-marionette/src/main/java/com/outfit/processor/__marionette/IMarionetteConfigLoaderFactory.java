package com.outfit.processor.__marionette;

public interface IMarionetteConfigLoaderFactory {

    public IMarionetteConfigParser generateParser();

    public IMarionetteConfigCreator generateConfigCreator();
}
