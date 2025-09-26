package com.outfit.processor.__marionette;

public class MarionetteParser {

    private final String pathToFile;

    private final IMarionetteConfigLoaderFactory loaderFactory;

    public MarionetteParser(String pathToFile, IMarionetteConfigLoaderFactory loaderFactory) {
        this.pathToFile = pathToFile;
        this.loaderFactory = loaderFactory;
    }

    public MarionetteConfig parseConfig() throws GenericMarionetteConfigParsingException {
        IMarionetteConfigParser parser = loaderFactory.generateParser();
        IMarionetteConfigCreator configCreator = loaderFactory.generateConfigCreator();
        return tryParsing(parser, configCreator);
    }

    private MarionetteConfig tryParsing(IMarionetteConfigParser parser, IMarionetteConfigCreator configCreator) throws GenericMarionetteConfigParsingException {
        try {
            return configCreator.createConfigObject(parser.parseConfigFile(pathToFile));
        } catch (Exception e) {
            throw new GenericMarionetteConfigParsingException("There was a problem parsing configuration file " + pathToFile);
        }
    }
}
