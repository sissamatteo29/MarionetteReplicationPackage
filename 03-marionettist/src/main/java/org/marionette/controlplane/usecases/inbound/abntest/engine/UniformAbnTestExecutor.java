package org.marionette.controlplane.usecases.inbound.abntest.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;
import org.marionette.controlplane.domain.values.ServiceName;
import org.marionette.controlplane.usecases.domain.configsnapshot.ServiceSnapshot;
import org.marionette.controlplane.usecases.domain.configsnapshot.SystemConfigurationSnapshot;
import org.marionette.controlplane.usecases.inbound.abntest.domain.GlobalMetricsRegistry;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SingleBehaviourSelection;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SystemBehaviourConfiguration;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.FetchMarionetteNodesMetricsGateway;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.ServiceMetricsDataPoint;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.SystemMetricsDataPoint;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ChangeBehaviourData;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ControlMarionetteServiceBehaviourGateway;

public class UniformAbnTestExecutor implements AbnTestExecutor {

    private final ConfigRegistry globalRegistry;
    private final ControlMarionetteServiceBehaviourGateway controlMarionetteGateway;
    private final FetchMarionetteNodesMetricsGateway fetchMarionetteMetricsGateway;
    private final AbnTestExecutorLogger logger = new AbnTestExecutorLogger();
    private final NonMarionetteNodesTracker nonMarionetteNodesTracker;

    public UniformAbnTestExecutor(ConfigRegistry globalRegistry,
            ControlMarionetteServiceBehaviourGateway controlMarionetteGateway,
            FetchMarionetteNodesMetricsGateway fetchMarionetteMetricsGateway,
            NonMarionetteNodesTracker nonMarionetteNodesTracker) {
        this.globalRegistry = globalRegistry;
        this.controlMarionetteGateway = controlMarionetteGateway;
        this.fetchMarionetteMetricsGateway = fetchMarionetteMetricsGateway;
        this.nonMarionetteNodesTracker = nonMarionetteNodesTracker;
    }

    @Override
    public GlobalMetricsRegistry executeAbnTest(List<SystemBehaviourConfiguration> systemConfigurations,
            Duration totalTime) {

        Instant testStart = Instant.now();

        // Log test start
        logger.logTestExecutionStart(systemConfigurations, totalTime);

        GlobalMetricsRegistry globalMetricsRegistry = new GlobalMetricsRegistry();
        // Compute time slice for each configuration
        Duration timeSlice = computeTimeSlice(totalTime, systemConfigurations.size());

        Duration samplingPeriod = Duration.ofSeconds(20);

        // Capture original state
        SystemConfigurationSnapshot originalState = SystemConfigurationSnapshot.fromConfigRegistry(globalRegistry);

        // MAIN LOOP
        try {
            for (int i = 0; i < systemConfigurations.size(); i++) {
                SystemBehaviourConfiguration config = systemConfigurations.get(i);
                int configIndex = i + 1;
                Instant configStart = Instant.now();

                try {
                    // Log configuration start
                    logger.logConfigurationStart(configIndex, systemConfigurations.size(), config, timeSlice);

                    // Apply configuration
                    SystemConfigurationSnapshot appliedSnapshot = applyConfigurationToSystem(config);
                    logger.logConfigurationApplied(configIndex, appliedSnapshot);

                    System.out.println("Spleeping 6s to let configuration stabilise");
                    Thread.sleep(6_000);

                    System.out.println("Now sleeping for " + timeSlice.toSeconds() + " seconds to gather metrics");;
                    // Wait for time slice
                    Thread.sleep(timeSlice.toMillis());

                    // Collect metrics
                    SystemMetricsDataPoint metrics = collectMetrics(appliedSnapshot, timeSlice, samplingPeriod);
                    logger.logMetricsCollection(configIndex, metrics);

                    // Store results
                    globalMetricsRegistry.putSystemMetrics(appliedSnapshot, metrics);

                    // Log completion
                    Duration actualDuration = Duration.between(configStart, Instant.now());
                    logger.logConfigurationComplete(configIndex, actualDuration, true);

                    System.out.println("=".repeat(30));
                    System.out.println("Sleeping 6s before applying new configuration");
                    Thread.sleep(6_000);

                } catch (Exception e) {
                    logger.logConfigurationFailure(configIndex, config, e);
                }
            }

        } finally {
            System.out.println("Restoring original state...");
        }

        return globalMetricsRegistry;
    }

    private SystemMetricsDataPoint collectMetrics(SystemConfigurationSnapshot appliedSnapshot, Duration timeSlice,
            Duration samplingPeriod) {

        List<ServiceMetricsDataPoint> collectedServiceDataPoints = new ArrayList<>();

        for (String serviceName : appliedSnapshot.getServiceNamesList()) {
            List<AggregateMetric> metricsForService = fetchMarionetteMetricsGateway.fetchMetricsForService(serviceName,
                    timeSlice, samplingPeriod);
            ServiceMetricsDataPoint serviceDataPoint = new ServiceMetricsDataPoint(
                    appliedSnapshot.getServiceSnapshotByName(serviceName), metricsForService);
            collectedServiceDataPoints.add(serviceDataPoint);
        }

        // Metrics for the non marionette nodes
        for (String nonMarionetteService : nonMarionetteNodesTracker.retrieveNonMarionetteNodeNames()) {
            // Always check if the service is already a marionette service, in this case do
            // not repeat the sampling
            if (!appliedSnapshot.getServiceNamesList().contains(nonMarionetteService)) {
                List<AggregateMetric> metricsForService = fetchMarionetteMetricsGateway
                        .fetchMetricsForService(nonMarionetteService, timeSlice, samplingPeriod);
                ServiceMetricsDataPoint serviceDataPoint = new ServiceMetricsDataPoint(
                        ServiceSnapshot.forNonMarionetteNode(nonMarionetteService),
                        metricsForService);
                collectedServiceDataPoints.add(serviceDataPoint);
            }
        }

        return new SystemMetricsDataPoint(collectedServiceDataPoints);
    }

    private SystemConfigurationSnapshot applyConfigurationToSystem(
            SystemBehaviourConfiguration systemBehaviourConfiguration) {

        System.out.println("\n" + "-".repeat(60));
        System.out.println("⚙️  APPLYING SYSTEM CONFIGURATION");
        System.out.println("-".repeat(60));

        // Count total selections and group by service for summary
        Map<String, List<SingleBehaviourSelection>> selectionsByService = groupSelectionsByService(
                systemBehaviourConfiguration);
        int totalSelections = systemBehaviourConfiguration.getBehaviourSelections().size();

        System.out.printf("📊 Configuration Summary:%n");
        System.out.printf("   • Total behavior changes: %d%n", totalSelections);
        System.out.printf("   • Services affected: %d%n", selectionsByService.size());

        // Show service breakdown
        selectionsByService.forEach((serviceName, selections) -> System.out.printf("   • %s: %d changes%n", serviceName,
                selections.size()));

        System.out.println("\n🔄 Applying changes:");

        int appliedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        // Apply each selection
        for (SingleBehaviourSelection behaviourSelection : systemBehaviourConfiguration) {
            try {
                boolean wasApplied = applySingleBehaviourSelection(behaviourSelection);
                if (wasApplied) {
                    appliedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                errors.add(String.format("%s: %s",
                        behaviourSelection.getFullMethodPath(),
                        e.getMessage()));
            }
        }

        // Summary
        System.out.println("\n📋 Application Results:");
        System.out.printf("   ✅ Applied: %d%n", appliedCount);
        System.out.printf("   ⏭️  Skipped: %d%n", skippedCount);

        if (!errors.isEmpty()) {
            System.out.printf("   ❌ Errors: %d%n", errors.size());
            errors.forEach(error -> System.out.printf("      • %s%n", error));
        }

        System.out.println("-".repeat(60));

        return SystemConfigurationSnapshot.fromConfigRegistry(globalRegistry);
    }

    private boolean applySingleBehaviourSelection(SingleBehaviourSelection behaviourSelection) {
        ServiceName serviceName = behaviourSelection.getServiceName();
        ClassName className = behaviourSelection.getClassName();
        MethodName methodName = behaviourSelection.getMethodName();
        BehaviourId selectedBehaviour = behaviourSelection.selectedBehaviour();

        String methodPath = behaviourSelection.getFullMethodPath();
        String behaviourId = selectedBehaviour.getBehaviourId();

        // Check if already applied
        if (isBehaviourAlreadyApplied(globalRegistry, behaviourSelection)) {
            System.out.printf("   ⏭️  %s -> %s (already applied)%n", methodPath, behaviourId);
            return false;
        }

        try {
            System.out.printf("   🔄 %s -> %s", methodPath, behaviourId);

            // Apply to registry
            globalRegistry.modifyCurrentBehaviourForMethod(serviceName, className, methodName, selectedBehaviour);

            // Notify service via HTTP
            String serviceEndpoint = globalRegistry.getEndpointOfService(serviceName).toString();
            controlMarionetteGateway.changeMarionetteServiceBehaviour(
                    serviceEndpoint,
                    new ChangeBehaviourData(
                            serviceName.getServiceName(),
                            className.getClassName(),
                            methodName.getMethodName(),
                            selectedBehaviour.getBehaviourId()));

            System.out.println(" ✅");
            return true;

        } catch (Exception e) {
            System.out.printf(" ❌ (%s)%n", e.getMessage());
            throw e; // Re-throw to be handled by caller
        }
    }

    private Map<String, List<SingleBehaviourSelection>> groupSelectionsByService(
            SystemBehaviourConfiguration systemBehaviourConfiguration) {

        Map<String, List<SingleBehaviourSelection>> grouped = new HashMap<>();

        for (SingleBehaviourSelection selection : systemBehaviourConfiguration) {
            String serviceName = selection.getServiceName().getServiceName();
            grouped.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(selection);
        }

        return grouped;
    }

    private boolean isBehaviourAlreadyApplied(ConfigRegistry globalRegistry,
            SingleBehaviourSelection behaviourSelection) {

        ServiceName serviceName = behaviourSelection.getServiceName();
        ClassName className = behaviourSelection.getClassName();
        MethodName methodName = behaviourSelection.getMethodName();
        BehaviourId selectedBehaviour = behaviourSelection.selectedBehaviour();

        BehaviourId currentBehaviourInRegistry = globalRegistry.getCurrentBehaviourIdForMethod(serviceName, className,
                methodName);

        return currentBehaviourInRegistry.equals(selectedBehaviour);
    }

    public static Duration computeTimeSlice(Duration totalTime, int configurationsCount) {
        validateInput(totalTime, configurationsCount);

        // Calculate base time slice
        long totalSeconds = totalTime.toSeconds();
        long timeSliceSeconds = totalSeconds / configurationsCount;

        // Handle very short time slices
        if (timeSliceSeconds < 30) {
            System.out.printf("Warning: Time slice would be %d seconds per configuration. " +
                    "Consider using a longer total time or fewer configurations.%n", timeSliceSeconds);

            if (timeSliceSeconds == 0) {
                long recommendedTotalSeconds = configurationsCount * 30; // 30 seconds minimum per config
                System.out.printf("Recommendation: Use at least %d seconds total time for %d configurations.%n",
                        recommendedTotalSeconds, configurationsCount);

                // Use minimum viable time slice
                return Duration.ofSeconds(Math.max(1, totalSeconds / configurationsCount));
            }
        }

        // Handle very long time slices
        if (timeSliceSeconds > 600) { // More than 10 minutes
            System.out.printf("Warning: Time slice would be %d seconds (%.1f minutes) per configuration. " +
                    "This is quite long.%n", timeSliceSeconds, timeSliceSeconds / 60.0);
        }

        return Duration.ofSeconds(timeSliceSeconds);
    }

    private static void validateInput(Duration totalTime, int configurationsCount) {
        if (totalTime == null) {
            throw new IllegalArgumentException("Total time cannot be null");
        }
        if (configurationsCount <= 0) {
            throw new IllegalArgumentException("Configurations count must be positive, got: " + configurationsCount);
        }
        if (totalTime.isNegative() || totalTime.isZero()) {
            throw new IllegalArgumentException("Total time must be positive, got: " + totalTime);
        }
    }

}
