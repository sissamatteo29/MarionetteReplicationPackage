package org.marionette.controlplane.usecases.inbound.abntest.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.marionette.controlplane.usecases.domain.configsnapshot.SystemConfigurationSnapshot;
import org.marionette.controlplane.usecases.inbound.abntest.domain.GlobalMetricsRegistry;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SingleBehaviourSelection;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SystemBehaviourConfiguration;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.SystemMetricsDataPoint;

public class AbnTestExecutorLogger {
    
    private static final String SEPARATOR_MAJOR = "=".repeat(80);
    private static final String SEPARATOR_MINOR = "-".repeat(60);
    private static final String SEPARATOR_CONFIG = "·".repeat(40);
    
    /**
     * Logs the start of an AB test execution
     */
    public void logTestExecutionStart(List<SystemBehaviourConfiguration> configurations, Duration totalTime) {
        System.out.println("\n" + SEPARATOR_MAJOR);
        System.out.println("🚀 AB TEST EXECUTION STARTED");
        System.out.println(SEPARATOR_MAJOR);
        
        System.out.println("📊 Test Parameters:");
        System.out.printf("   • Total Configurations: %d%n", configurations.size());
        System.out.printf("   • Total Test Duration: %s%n", formatDuration(totalTime));
        System.out.printf("   • Time per Configuration: %s%n", formatDuration(computeTimeSlice(totalTime, configurations.size())));
        System.out.printf("   • Started at: %s%n", Instant.now());
        
        System.out.println("\n📋 Configuration Overview:");
        logConfigurationsSummary(configurations);
        
        System.out.println(SEPARATOR_MAJOR);
        System.out.println("🏁 Beginning configuration cycle...\n");
    }
    
    /**
     * Logs the start of a specific configuration
     */
    public void logConfigurationStart(int configIndex, int totalConfigs, SystemBehaviourConfiguration config, Duration timeSlice) {
        System.out.println(SEPARATOR_CONFIG);
        System.out.printf("⚙️  CONFIGURATION %d/%d%n", configIndex, totalConfigs);
        System.out.println(SEPARATOR_CONFIG);
        
        System.out.printf("🕐 Execution Time: %s%n", formatDuration(timeSlice));
        System.out.printf("📍 Started at: %s%n", Instant.now());
        
        System.out.println("\n🎯 Behavior Selections:");
        logBehaviorSelections(config);
        
        System.out.println();
    }
    
    /**
     * Logs successful configuration application
     */
    public void logConfigurationApplied(int configIndex, SystemConfigurationSnapshot appliedSnapshot) {
        System.out.printf("✅ Configuration %d applied successfully%n", configIndex);
        System.out.printf("   • Services affected: %d%n", appliedSnapshot.services().size());
        
        // Log which services were modified
        appliedSnapshot.services().forEach((serviceName, serviceSnapshot) -> {
            int methodCount = serviceSnapshot.classes().values().stream()
                .mapToInt(classSnapshot -> classSnapshot.methodBehaviors().size())
                .sum();
            System.out.printf("   • %s: %d methods configured%n", serviceName, methodCount);
        });
    }
    
    /**
     * Logs metrics collection
     */
    public void logMetricsCollection(int configIndex, SystemMetricsDataPoint metrics) {
        System.out.printf("📈 Metrics collected for configuration %d%n", configIndex);
        
        if (metrics != null && metrics.serviceMetrics() != null) {
            System.out.printf("   • Services monitored: %d%n", metrics.serviceMetrics().size());
            
            // Log basic metrics summary
            metrics.serviceMetrics().forEach(serviceMetrics -> {
                if (serviceMetrics.metrics() != null) {
                    System.out.printf("   • %s: %d metrics collected%n", 
                        serviceMetrics.serviceConfiguration().serviceName(), 
                        serviceMetrics.metrics().size());
                }
            });
        } else {
            System.out.println("   ⚠️  No metrics data available");
        }
    }
    
    /**
     * Logs configuration completion
     */
    public void logConfigurationComplete(int configIndex, Duration actualDuration, boolean successful) {
        if (successful) {
            System.out.printf("✅ Configuration %d completed successfully%n", configIndex);
        } else {
            System.out.printf("❌ Configuration %d failed%n", configIndex);
        }
        System.out.printf("   • Actual duration: %s%n", formatDuration(actualDuration));
        System.out.println();
    }
    
    /**
     * Logs configuration failure
     */
    public void logConfigurationFailure(int configIndex, SystemBehaviourConfiguration config, Exception error) {
        System.out.printf("❌ Configuration %d FAILED%n", configIndex);
        System.out.printf("   • Error: %s%n", error.getMessage());
        System.out.printf("   • Failed selections:%n");
        
        for (SingleBehaviourSelection selection : config) {
            System.out.printf("     - %s -> %s%n", 
                selection.getFullMethodPath(), 
                selection.selectedBehaviour().getBehaviourId());
        }
        System.out.println();
    }
    
    /**
     * Logs test execution completion
     */
    public void logTestExecutionComplete(GlobalMetricsRegistry results, Duration totalActualDuration) {
        System.out.println("\n" + SEPARATOR_MAJOR);
        System.out.println("🏁 AB TEST EXECUTION COMPLETED");
        System.out.println(SEPARATOR_MAJOR);
        
        System.out.printf("⏱️  Total Execution Time: %s%n", formatDuration(totalActualDuration));
        System.out.printf("📊 Configurations Tested: %d%n", getConfigurationCount(results));
        System.out.printf("✅ Successful Configurations: %d%n", getSuccessfulConfigurationCount(results));
        System.out.printf("❌ Failed Configurations: %d%n", getFailedConfigurationCount(results));
        System.out.printf("🎯 Success Rate: %.1f%%%n", getSuccessRate(results));
        
        System.out.println("\n📈 Results Summary:");
        logResultsSummary(results);
        
        System.out.println(SEPARATOR_MAJOR);
        System.out.printf("📋 Results stored in GlobalMetricsRegistry with %d entries%n", getConfigurationCount(results));
        System.out.println(SEPARATOR_MAJOR + "\n");
    }
    
    /**
     * Logs system state restoration
     */
    public void logSystemStateRestoration(SystemConfigurationSnapshot originalState, boolean successful) {
        System.out.println(SEPARATOR_MINOR);
        if (successful) {
            System.out.println("🔄 System state restored successfully");
        } else {
            System.out.println("⚠️  System state restoration failed");
        }
        System.out.printf("   • Original state from: %s%n", originalState.capturedAt());
        System.out.printf("   • Services to restore: %d%n", originalState.services().size());
        System.out.println(SEPARATOR_MINOR);
    }
    
    /**
     * Logs critical errors
     */
    public void logCriticalError(String phase, Exception error) {
        System.err.println("\n" + "🚨".repeat(20));
        System.err.println("🚨 CRITICAL ERROR DURING " + phase.toUpperCase());
        System.err.println("🚨".repeat(20));
        System.err.printf("Error: %s%n", error.getMessage());
        System.err.printf("Type: %s%n", error.getClass().getSimpleName());
        if (error.getCause() != null) {
            System.err.printf("Cause: %s%n", error.getCause().getMessage());
        }
        System.err.println("🚨".repeat(20) + "\n");
    }
    
    // Private helper methods
    
    private void logConfigurationsSummary(List<SystemBehaviourConfiguration> configurations) {
        // Group configurations by number of selections
        Map<Integer, Long> selectionCounts = configurations.stream()
            .collect(Collectors.groupingBy(
                config -> config.getBehaviourSelections().size(),
                Collectors.counting()
            ));
        
        selectionCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> 
                System.out.printf("   • %d configurations with %d behavior selections%n", 
                    entry.getValue(), entry.getKey()));
        
        // Show example of first configuration
        if (!configurations.isEmpty()) {
            System.out.println("\n📝 Example Configuration (#1):");
            SystemBehaviourConfiguration firstConfig = configurations.get(0);
            logBehaviorSelections(firstConfig);
        }
    }
    
    private void logBehaviorSelections(SystemBehaviourConfiguration config) {
        // Group by service for cleaner display
        Map<String, List<SingleBehaviourSelection>> byService = new HashMap<>();
        
        for (SingleBehaviourSelection selection : config) {
            String serviceName = selection.getServiceName().getServiceName();
            byService.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(selection);
        }
        
        byService.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(serviceEntry -> {
                System.out.printf("   📦 %s:%n", serviceEntry.getKey());
                serviceEntry.getValue().forEach(selection -> 
                    System.out.printf("      • %s.%s -> %s%n",
                        selection.getClassName().getClassName(),
                        selection.getMethodName().getMethodName(),
                        selection.selectedBehaviour().getBehaviourId()));
            });
    }
    
    private void logResultsSummary(GlobalMetricsRegistry results) {
        // This would need to be implemented based on your GlobalMetricsRegistry structure
        System.out.println("   • Results analysis requires extending GlobalMetricsRegistry with query methods");
        System.out.println("   • Consider adding methods like: getBestPerformingConfiguration()");
        System.out.println("   • Consider adding methods like: getAverageResponseTime()");
    }
    
    private String formatDuration(Duration duration) {
        if (duration.toHours() > 0) {
            return String.format("%dh %dm %ds", 
                duration.toHours(), 
                duration.toMinutesPart(), 
                duration.toSecondsPart());
        } else if (duration.toMinutes() > 0) {
            return String.format("%dm %ds", 
                duration.toMinutes(), 
                duration.toSecondsPart());
        } else {
            return String.format("%ds", duration.toSeconds());
        }
    }
    
    private Duration computeTimeSlice(Duration totalTime, int configurationsCount) {
        return Duration.ofSeconds(totalTime.toSeconds() / configurationsCount);
    }
    
    // These methods would need to be implemented based on your GlobalMetricsRegistry
    private int getConfigurationCount(GlobalMetricsRegistry results) {
        // You'll need to add this method to GlobalMetricsRegistry
        return 0; // Placeholder
    }
    
    private int getSuccessfulConfigurationCount(GlobalMetricsRegistry results) {
        // You'll need to implement logic to count successful configurations
        return 0; // Placeholder
    }
    
    private int getFailedConfigurationCount(GlobalMetricsRegistry results) {
        // You'll need to implement logic to count failed configurations
        return 0; // Placeholder
    }
    
    private double getSuccessRate(GlobalMetricsRegistry results) {
        int total = getConfigurationCount(results);
        if (total == 0) return 0.0;
        return (getSuccessfulConfigurationCount(results) * 100.0) / total;
    }
}
