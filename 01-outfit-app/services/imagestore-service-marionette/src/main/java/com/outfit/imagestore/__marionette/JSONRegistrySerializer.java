package com.outfit.imagestore.__marionette;

import java.util.List;
import java.util.Map;

public class JSONRegistrySerializer {

    public static String generateJson(Map<String, Map<String, RuntimeMethodConfig>> registry, String serviceName) {
        StringBuilder json = new StringBuilder("{");
        json.append(formatKeyValuePairString("serviceName", serviceName));
        json.append(formatBeginningArray("classes"));
        for (Map.Entry<String, Map<String, RuntimeMethodConfig>> classConfig : registry.entrySet()) {
            json.append("{");
            json.append(formatKeyValuePairString("name", classConfig.getKey()));
            json.append(formatBeginningArray("methods"));
            for (Map.Entry<String, RuntimeMethodConfig> methodConfig : classConfig.getValue().entrySet()) {
                json.append("{");
                json.append(formatKeyValuePairString("name", methodConfig.getKey()));
                json.append(formatKeyValuePairString("currentBehaviour", methodConfig.getValue().getCurrentBehaviourId()));
                json.append(formatBeginningArray("availableBehaviours"));
                json.append(formatStringArrayContent(methodConfig.getValue().getAvailableBehaviourIds()));
                // Close array
                json.append("]");
                json.append("}");
                json.append(",");
            }
            // Remove last comma
            json.deleteCharAt(json.length() - 1);
            // Close array
            json.append("]");
            json.append("}");
            json.append(",");
        }
        // Remove last comma
        json.deleteCharAt(json.length() - 1);
        // Close array
        json.append("]");
        // Outermost
        json.append("}");
        return json.toString();
    }

    private static String formatStringArrayContent(List<String> content) {
        StringBuilder json = new StringBuilder();
        for (String elem : content) {
            json.append(formatSingleString(elem) + ",");
        }
        return json.substring(0, json.length() - 1);
    }

    private static String formatSingleString(String input) {
        return "\"" + input + "\"";
    }

    private static String formatBeginningArray(String key) {
        return formatSingleString(key) + ":" + "[";
    }

    private static String formatKeyValuePairString(String key, String value) {
        StringBuilder json = new StringBuilder();
        json.append("\"" + key + "\"").append(":").append("\"" + value + "\"").append(",");
        return json.toString();
    }

    public static void main(String[] args) {
        String serviceName = "image-processor-service";
        String className = "com/outfit/processor/controllers/ImageProcessingController.java";
        String methodName = "processImageBytes";
        String currentBehaviour = "default";
        List<String> availableBehaviours = List.of("default", "low_energy");
        RuntimeMethodConfig methodConfig = new RuntimeMethodConfig(currentBehaviour, availableBehaviours);
        System.out.println(generateJson(BehaviourRegistry.behaviourRegistry, BehaviourRegistry.serviceName));
    }
}
