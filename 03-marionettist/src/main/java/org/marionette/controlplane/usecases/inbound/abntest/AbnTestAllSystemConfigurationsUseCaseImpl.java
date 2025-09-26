package org.marionette.controlplane.usecases.inbound.abntest;

import java.time.Duration;
import java.util.List;

import org.marionette.controlplane.domain.entities.abntest.AbnTestResultsStorage;
import org.marionette.controlplane.domain.entities.abntest.SingleAbnTestResult;
import org.marionette.controlplane.usecases.inbound.AbnTestAllSystemConfigurationsUseCase;
import org.marionette.controlplane.usecases.inbound.AbnTestResult;
import org.marionette.controlplane.usecases.inbound.abntest.domain.GlobalMetricsRegistry;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SystemBehaviourConfiguration;
import org.marionette.controlplane.usecases.inbound.abntest.domain.VariationPoint;
import org.marionette.controlplane.usecases.inbound.abntest.engine.AbnTestExecutor;
import org.marionette.controlplane.usecases.inbound.abntest.engine.SystemConfigurationsGenerator;
import org.marionette.controlplane.usecases.inbound.abntest.engine.VariationPointsExtractor;
import org.marionette.controlplane.usecases.inbound.abntest.ranking.SimpleConfigurationRanking;
import org.marionette.controlplane.usecases.inbound.abntest.ranking.SystemConfigurationsRanker;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.OrderedMetricsMetadataProvider;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.MetricsConfiguration;

public class AbnTestAllSystemConfigurationsUseCaseImpl implements AbnTestAllSystemConfigurationsUseCase {

    private final VariationPointsExtractor variationPointsExtractor;
    private final SystemConfigurationsGenerator systemConfigurationsGenerator;
    private final AbnTestExecutor executor;
    private final SystemConfigurationsRanker ranker;
    private final AbnTestResultsStorage resultsStorage;
    private final OrderedMetricsMetadataProvider metricsMetadataProvider;

    public AbnTestAllSystemConfigurationsUseCaseImpl(
        VariationPointsExtractor variationPointsExtractor, 
        SystemConfigurationsGenerator systemConfigurationsGenerator, 
        AbnTestExecutor executor,
        SystemConfigurationsRanker ranker,
        AbnTestResultsStorage resultsStorage,
        OrderedMetricsMetadataProvider metricsMetadataProvider) {
        this.variationPointsExtractor = variationPointsExtractor;
        this.systemConfigurationsGenerator = systemConfigurationsGenerator;
        this.executor = executor;
        this.ranker = ranker;
        this.resultsStorage = resultsStorage;
        this.metricsMetadataProvider = metricsMetadataProvider;
    }

    @Override
    public AbnTestResult execute() {
        // Default duration of 100 seconds for backward compatibility
        return execute(Duration.ofSeconds(100));
    }
    
    @Override
    public AbnTestResult execute(Duration totalDuration) {
        
        List<VariationPoint> variationPoints = variationPointsExtractor.extractAllVariationPoints();
        
        List<SystemBehaviourConfiguration> systemConfigs =  systemConfigurationsGenerator.generateAllSystemConfigurations(variationPoints);

        MetricsConfiguration metricsConfiguration = metricsMetadataProvider.loadMetrics();
        
        GlobalMetricsRegistry globalMetricsRegistry = executor.executeAbnTest(systemConfigs, totalDuration);
        
        List<SimpleConfigurationRanking> systemConfigRanking = ranker.rankConfigurations(globalMetricsRegistry.getAllMetrics(), metricsConfiguration);

        resultsStorage.putResults(
            new SingleAbnTestResult(
                metricsConfiguration,
                globalMetricsRegistry,
                systemConfigRanking
            )
        );

        return new AbnTestResult();
    }
    
}
