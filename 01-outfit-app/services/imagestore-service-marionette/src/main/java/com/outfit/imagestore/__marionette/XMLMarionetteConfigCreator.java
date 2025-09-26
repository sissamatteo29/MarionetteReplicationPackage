package com.outfit.imagestore.__marionette;

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XMLMarionetteConfigCreator implements IMarionetteConfigCreator {

    @Override
    public MarionetteConfig createConfigObject(Object handle) {
        // Assume the xml library thanks to abstract factory
        Document castHandle = (Document) handle;
        Element root = castHandle.getDocumentElement();
        MarionetteConfig config = new MarionetteConfig();
        parseGlobalConfigs(root, config);
        parseAllClassConfigs(root, config);
        return config;
    }

    private void parseGlobalConfigs(Element root, MarionetteConfig config) {
        config.microserviceName = getText(root, "microserviceName");
        config.srcRoot = getText(root, "srcRoot");
        config.injectCodeRoot = getText(root, "injectCodeRoot");
    }

    private void parseAllClassConfigs(Element root, MarionetteConfig config) {
        config.marionetteClasses = new ArrayList<>();
        Element wrapperForMarionetteClasses = (Element) root.getElementsByTagName("marionetteClasses").item(0);
        NodeList classNodes = wrapperForMarionetteClasses.getElementsByTagName("marionetteClass");
        for (int i = 0; i < classNodes.getLength(); i++) {
            MarionetteClassConfig classConfig = new MarionetteClassConfig();
            parseOriginalClassConfig((Element) classNodes.item(i), classConfig);
            parseAllVariantsConfigs((Element) classNodes.item(i), classConfig);
            config.marionetteClasses.add(classConfig);
        }
    }

    private void parseOriginalClassConfig(Element singleClassRootEl, MarionetteClassConfig classConfig) {
        GenericClassConfig originalClassConfig = new GenericClassConfig();
        Element originalXmlClassConfig = (Element) singleClassRootEl.getElementsByTagName("originalClass").item(0);
        parseSingleClassConfig(originalXmlClassConfig, originalClassConfig);
        classConfig.originalClass = originalClassConfig;
    }

    private void parseAllVariantsConfigs(Element singleClassRootEl, MarionetteClassConfig classConfig) {
        Element wrapperVariantClasses = (Element) singleClassRootEl.getElementsByTagName("variantClasses").item(0);
        NodeList variantNodes = wrapperVariantClasses.getElementsByTagName("variantClass");
        classConfig.variantClasses = new ArrayList<>();
        for (int i = 0; i < variantNodes.getLength(); i++) {
            GenericClassConfig variantClassConfig = new GenericClassConfig();
            parseSingleClassConfig((Element) variantNodes.item(i), variantClassConfig);
            classConfig.variantClasses.add(variantClassConfig);
        }
    }

    private void parseSingleClassConfig(Element xmlClassConfig, GenericClassConfig classConfig) {
        classConfig.path = getText(xmlClassConfig, "path");
        parseAllBehaviours(xmlClassConfig, classConfig);
    }

    private void parseAllBehaviours(Element xmlClassConfig, GenericClassConfig classConfig) {
        classConfig.behaviours = new ArrayList<>();
        Element wrapperForBehaviours = (Element) xmlClassConfig.getElementsByTagName("behaviours").item(0);
        NodeList behaviourNodes = wrapperForBehaviours.getElementsByTagName("behaviour");
        for (int i = 0; i < behaviourNodes.getLength(); i++) {
            BehaviourConfig behaviourConfig = new BehaviourConfig();
            parseBehaviour((Element) behaviourNodes.item(i), behaviourConfig);
            classConfig.behaviours.add(behaviourConfig);
        }
    }

    private void parseBehaviour(Element behaviourEl, BehaviourConfig behaviourConfig) {
        behaviourConfig.name = getText(behaviourEl, "name");
        behaviourConfig.id = getText(behaviourEl, "id");
    }

    private static String getText(Element root, String tag) {
        NodeList list = root.getElementsByTagName(tag);
        if (list.getLength() == 0)
            return null;
        return list.item(0).getTextContent().trim();
    }
}
