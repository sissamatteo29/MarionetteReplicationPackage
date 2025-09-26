package com.outfit.imagestore.__marionette;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;

public class BehaviourRegistry {

    // For each class, maintain a map between method name (original) and current
    // variant of execution
    public static Map<String, Map<String, RuntimeMethodConfig>> behaviourRegistry = new HashMap<>();

    public static String serviceName;

    static {
        try {
            System.out.println("Initializing BehaviourRegistry...");
            // Try to load from classpath first
            InputStream configStream = BehaviourRegistry.class.getClassLoader().getResourceAsStream("marionette.xml");
            if (configStream == null) {
                System.err.println("Cannot find marionette.xml in classpath, registry will remain empty");
            }
            System.out.println("Found marionette.xml in classpath, parsing...");
            IMarionetteConfigLoaderFactory loaderFactory = new XMLMarionetteConfigLoaderFactory();
            MarionetteParser marionetteParser = new MarionetteParser("marionette.xml", loaderFactory);
            MarionetteConfig marionetteConfig = marionetteParser.parseConfig();
            // Mapping to the ConfigRegistry
            serviceName = marionetteConfig.microserviceName;
            if (marionetteConfig != null && marionetteConfig.marionetteClasses != null) {
                for (MarionetteClassConfig classConfig : marionetteConfig.marionetteClasses) {
                    // Add new entry for the class
                    String className = classConfig.originalClass.path;
                    behaviourRegistry.computeIfAbsent(className, key -> new HashMap<>());
                    for (BehaviourConfig behaviour : classConfig.originalClass.behaviours) {
                        String behaviourName = behaviour.name;
                        String originalBehaviourId = behaviour.id;
                        List<String> availableBehaviours = extractAllBehavioursId(classConfig.variantClasses, behaviourName);
                        availableBehaviours.add(originalBehaviourId);
                        System.out.println("Available behaviours for " + className + "." + behaviourName + ": " + availableBehaviours);
                        behaviourRegistry.get(className).put(behaviourName, new RuntimeMethodConfig(originalBehaviourId, availableBehaviours));
                    }
                }
            }
            System.out.println("BehaviourRegistry initialized successfully with " + behaviourRegistry.size() + " classes");
        } catch (Exception e) {
            System.err.println("Failed to initialize BehaviourRegistry: " + e.getMessage());
            e.printStackTrace();
            // Don't rethrow - let the class load with empty registry
        }
    }

    private static List<String> extractAllBehavioursId(List<GenericClassConfig> classConfigs, String methodName) {
        List<String> result = new ArrayList<>();
        for (GenericClassConfig genericClassConfig : classConfigs) {
            for (BehaviourConfig behaviourConfig : genericClassConfig.behaviours) {
                if (behaviourConfig.name.equals(methodName)) {
                    result.add(behaviourConfig.id);
                }
            }
        }
        return result;
    }

    public static void addMethod(String className, String methodName, RuntimeMethodConfig methodConfig) {
        behaviourRegistry.computeIfAbsent(className, key -> new HashMap<>()).put(methodName, methodConfig);
        System.out.println("Current behaviour registry: " + behaviourRegistry);
    }

    public static String getBehaviourId(String className, String methodName) {
        return behaviourRegistry.get(className).get(methodName).getCurrentBehaviourId();
    }

    public static void setBehaviour(String className, String methodName, String newBehaviourId) {
        if (behaviourRegistry.containsKey(className)) {
            Map<String, RuntimeMethodConfig> methodMap = behaviourRegistry.get(className);
            if (methodMap.containsKey(methodName)) {
                methodMap.get(methodName).setCurrentBehaviourId(newBehaviourId);
            } else {
                System.out.println("Sorry no method named like that in the class");
            }
        } else {
            System.out.println("Sorry, no class named like that");
        }
    }
}
