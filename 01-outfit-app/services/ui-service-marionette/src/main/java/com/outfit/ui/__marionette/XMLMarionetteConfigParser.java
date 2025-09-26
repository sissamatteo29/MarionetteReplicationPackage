package com.outfit.ui.__marionette;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.SAXException;
import java.io.InputStream;

public class XMLMarionetteConfigParser implements IMarionetteConfigParser {

    @Override
    public Object parseConfigFile(String pathToConfigFile) throws IOParserException, MarionetteConfigSyntaxParsingException, Exception {
        try {
            return tryLoadingFromClassPath(pathToConfigFile);
        } catch (IOException e) {
            throw new IOParserException("Impossible to read the configuration file " + pathToConfigFile);
        } catch (SAXException e) {
            throw new MarionetteConfigSyntaxParsingException("There was a problem parsing the configuration file " + pathToConfigFile);
        } catch (Exception e) {
            throw new Exception("Problem with parser configuration");
        }
    }

    private Object tryLoadingFromClassPath(String pathToConfigFile) throws IOParserException, MarionetteConfigSyntaxParsingException, Exception {
        System.out.println("Attempting to load " + pathToConfigFile + " from classpath...");
        // Load from classpath
        InputStream configStream = this.getClass().getClassLoader().getResourceAsStream(pathToConfigFile);
        if (configStream == null) {
            throw new IOParserException("Configuration file " + pathToConfigFile + " not found in classpath");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(configStream);
    }
}
