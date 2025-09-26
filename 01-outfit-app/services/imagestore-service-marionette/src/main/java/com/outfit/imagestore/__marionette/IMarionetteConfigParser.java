package com.outfit.imagestore.__marionette;

public interface IMarionetteConfigParser {

    public Object parseConfigFile(String pathToConfigFile) throws IOParserException, MarionetteConfigSyntaxParsingException, Exception;
}
