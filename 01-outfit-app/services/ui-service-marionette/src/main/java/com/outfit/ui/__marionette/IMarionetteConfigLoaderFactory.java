package com.outfit.ui.__marionette;

public interface IMarionetteConfigLoaderFactory {

    public IMarionetteConfigParser generateParser();

    public IMarionetteConfigCreator generateConfigCreator();
}
