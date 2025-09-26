package org.marionette.controlplane.usecases.inbound.abntest.engine;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SystemBehaviourConfiguration;
import org.marionette.controlplane.usecases.inbound.abntest.domain.VariationPoint;

public class SystemConfigurationsGeneratorLogger {

    /**
     * Logs the start of generation with input summary
     */
    public static void logGenerationStart(List<VariationPoint> variationPoints) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔧 SYSTEM BEHAVIOR CONFIGURATION GENERATION - STARTED");
        System.out.println("=".repeat(80));

        // Calculate expected number of configurations
        long expectedConfigurations = variationPoints.stream()
                .mapToLong(vp -> vp.behaviours().getBehaviours().size())
                .reduce(1L, (a, b) -> a * b);

        System.out.println("📊 Generation Statistics:");
        System.out.println("   • Variation Points: " + variationPoints.size());
        System.out.println("   • Expected Configurations: " + expectedConfigurations);

        if (expectedConfigurations > 1000) {
            System.out.println("   ⚠️  WARNING: Large number of configurations will be generated!");
        }

        System.out.println();
        System.out.println("📍 Variation Points Overview:");

        for (int i = 0; i < variationPoints.size(); i++) {
            VariationPoint vp = variationPoints.get(i);
            System.out.printf("   [%d] %s.%s.%s%n",
                    i + 1,
                    vp.serviceName(),
                    vp.className(),
                    vp.methodName());

            String behaviors = vp.behaviours().getBehaviours().stream()
                    .map(BehaviourId::getBehaviourId)
                    .collect(Collectors.joining(", "));

            System.out.printf("       Behaviors (%d): %s%n",
                    vp.behaviours().getBehaviours().size(),
                    behaviors);
        }

        System.out.println("=".repeat(80));
        System.out.println("🚀 Generating configurations...");
        System.out.println();
    }

    /**
     * Logs the final results with all generated configurations
     */
    public static void logGenerationResults(List<VariationPoint> variationPoints,
            List<SystemBehaviourConfiguration> configurations) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ SYSTEM BEHAVIOR CONFIGURATION GENERATION - COMPLETED");
        System.out.println("=".repeat(80));

        System.out.println("📈 Generation Summary:");
        System.out.println("   • Variation Points: " + variationPoints.size());
        System.out.println("   • Generated Configurations: " + configurations.size());
        System.out.println("   • Memory Usage: ~" + estimateMemoryUsage(configurations) + " KB");

        if (configurations.isEmpty()) {
            System.out.println("\n❌ No configurations generated (this might indicate an issue)");
            return;
        }

        System.out.println("\n📋 GENERATED CONFIGURATIONS:");
        System.out.println("-".repeat(80));

        for (int i = 0; i < configurations.size(); i++) {
            logSingleConfiguration(i + 1, configurations.get(i), variationPoints);

            // Add separator between configurations (except for the last one)
            if (i < configurations.size() - 1) {
                System.out.println("   " + "·".repeat(60));
            }
        }

        System.out.println("-".repeat(80));

        // Summary statistics
        logConfigurationStatistics(configurations, variationPoints);

        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Logs when no variation points are provided
     */
    private static void logEmptyInput() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔧 SYSTEM BEHAVIOR CONFIGURATION GENERATION");
        System.out.println("=".repeat(80));
        System.out.println("📝 Input: No variation points provided");
        System.out.println("📋 Result: Single empty configuration returned");
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Logs a single configuration in a readable format
     */
    private static void logSingleConfiguration(int configNumber, SystemBehaviourConfiguration config,
            List<VariationPoint> variationPoints) {
        System.out.printf("   🎯 Configuration #%d:%n", configNumber);

        Map<VariationPoint, BehaviourId> selections = config.getBehaviourSelections();

        // Show selections in the same order as variation points for consistency
        for (VariationPoint vp : variationPoints) {
            BehaviourId selectedBehavior = selections.get(vp);

            if (selectedBehavior != null) {
                System.out.printf("      ├─ %s.%s.%s → %s%n",
                        vp.serviceName(),
                        vp.className(),
                        vp.methodName(),
                        selectedBehavior.getBehaviourId());
            } else {
                System.out.printf("      ├─ %s.%s.%s → ❌ NO SELECTION%n",
                        vp.serviceName(),
                        vp.className(),
                        vp.methodName());
            }
        }
    }

    /**
     * Logs interesting statistics about the generated configurations
     */
    private static void logConfigurationStatistics(List<SystemBehaviourConfiguration> configurations,
            List<VariationPoint> variationPoints) {
        System.out.println("📊 Configuration Statistics:");

        // Count usage of each behavior across all configurations
        Map<String, Long> behaviorUsageCount = configurations.stream()
                .flatMap(config -> config.getBehaviourSelections().values().stream())
                .collect(Collectors.groupingBy(
                        BehaviourId::getBehaviourId,
                        Collectors.counting()));

        System.out.println("   • Behavior Usage Frequency:");
        behaviorUsageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> System.out.printf("      - %s: %d times (%.1f%%)%n",
                        entry.getKey(),
                        entry.getValue(),
                        (entry.getValue() * 100.0) / configurations.size()));

        // Check for duplicate configurations (shouldn't happen, but good to verify)
        long uniqueConfigurations = configurations.stream()
                .distinct()
                .count();

        if (uniqueConfigurations != configurations.size()) {
            System.out.printf("   ⚠️  WARNING: Found %d duplicate configurations!%n",
                    configurations.size() - uniqueConfigurations);
        } else {
            System.out.println("   ✅ All configurations are unique");
        }

        // Completeness check
        long completeConfigurations = configurations.stream()
                .mapToLong(config -> config.getBehaviourSelections().size())
                .filter(size -> size == variationPoints.size())
                .count();

        if (completeConfigurations != configurations.size()) {
            System.out.printf("   ⚠️  WARNING: %d configurations are incomplete!%n",
                    configurations.size() - completeConfigurations);
        } else {
            System.out.println("   ✅ All configurations are complete");
        }
    }

    /**
     * Estimates memory usage of the configurations (rough approximation)
     */
    private static long estimateMemoryUsage(List<SystemBehaviourConfiguration> configurations) {
        if (configurations.isEmpty())
            return 0;

        // Very rough estimation:
        // - Each configuration object: ~100 bytes
        // - Each selection entry: ~200 bytes (objects + strings)
        long avgSelectionsPerConfig = configurations.stream()
                .mapToLong(config -> config.getBehaviourSelections().size())
                .sum() / configurations.size();

        return (100 + avgSelectionsPerConfig * 200) * configurations.size() / 1024;
    }
}