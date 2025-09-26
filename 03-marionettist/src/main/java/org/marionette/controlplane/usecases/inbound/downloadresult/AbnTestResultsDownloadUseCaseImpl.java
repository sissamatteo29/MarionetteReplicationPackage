package org.marionette.controlplane.usecases.inbound.downloadresult;

import java.util.ArrayList;
import java.util.List;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.AbnTestResultsDTO;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.BehaviourSelectionSnapshotDTO;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.ClassConfigSnapshotDTO;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.MetricConfigurationDTO;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.MetricValueDTO;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.ServiceConfigSnapshotDTO;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.ServiceLevelResultsDTO;
import org.marionette.controlplane.adapters.inbound.downloadresult.dto.SystemConfigurationRankDTO;
import org.marionette.controlplane.domain.entities.abntest.AbnTestResultsStorage;
import org.marionette.controlplane.domain.entities.abntest.SingleAbnTestResult;
import org.marionette.controlplane.usecases.domain.configsnapshot.SystemConfigurationSnapshot;
import org.marionette.controlplane.usecases.inbound.AbnTestDownloadResult;
import org.marionette.controlplane.usecases.inbound.abntest.ranking.SimpleConfigurationRanking;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.MetricsConfiguration;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.SystemMetricsDataPoint;

public class AbnTestResultsDownloadUseCaseImpl implements AbnTestResultsDownloadUseCase {

    private final AbnTestResultsStorage testResultsStorage;

    public AbnTestResultsDownloadUseCaseImpl(AbnTestResultsStorage testResultsStorage) {
        this.testResultsStorage = testResultsStorage;
    }

    @Override
    public AbnTestDownloadResult execute() {

        SingleAbnTestResult testResult = testResultsStorage.getResults();  // Get first result

        List<MetricConfigurationDTO> metricConfigurationDTOs = metricsConfigToDto(testResult.metricsConfiguration());

        // Extract first 5 positions in the ranking
        List<SystemConfigurationRankDTO> rankingDTO = new ArrayList<>();

        int maxResults = Math.min(5, testResult.ranking().size());
        for(int i = 0; i < maxResults; i++) {

            // Get the rank data
            SimpleConfigurationRanking rank = testResult.ranking().get(i);
            int positionInRankFromOne = i + 1;
            List<MetricValueDTO> systemLevelResults = systemMetricsToDto(rank.systemMetrics());

            // Mapping ot system snapshot
            SystemConfigurationSnapshot systemSnapshot = testResult.metricsRegistry().getSystemConfig(rank.configurationId());

            List<ServiceConfigSnapshotDTO> serviceConfigSnapshotDTOs = serviceConfigsToDto(systemSnapshot);

            List<ServiceLevelResultsDTO> serviceLevelResultsDTOs = serviceResultsToDto(testResult.metricsRegistry().getSystemDataPoint(rank.configurationId()));

            rankingDTO.add(
                new SystemConfigurationRankDTO(
                    positionInRankFromOne,
                    serviceConfigSnapshotDTOs,
                    systemLevelResults,
                    serviceLevelResultsDTOs
                )
            );

        }

        AbnTestResultsDTO resultDTO = new AbnTestResultsDTO(
            metricConfigurationDTOs,
            rankingDTO
        );

        return new AbnTestDownloadResult(resultDTO);
       
    }
    

    private List<ServiceLevelResultsDTO> serviceResultsToDto(SystemMetricsDataPoint systemDataPoint) {
        
        return systemDataPoint.serviceMetrics().stream()
            .map(
                serviceMetricsDataPoint -> new ServiceLevelResultsDTO(
                    serviceMetricsDataPoint.serviceConfiguration().serviceName(),
                    serviceMetricsDataPoint.metrics().stream().map(
                        aggregateMetric -> new MetricValueDTO(
                            aggregateMetric.name(),
                            aggregateMetric.value(),
                            aggregateMetric.unit()
                        )
                    )
                    .toList()
                )   
            )
            .toList();

    }

    private List<ServiceConfigSnapshotDTO> serviceConfigsToDto(SystemConfigurationSnapshot systemSnapshot) {
       
        return systemSnapshot.services().entrySet().stream()
            .map(
                entry1-> new ServiceConfigSnapshotDTO(
                    entry1.getKey(),
                    entry1.getValue().classes().entrySet().stream()
                        .map(
                            entry2 -> new ClassConfigSnapshotDTO(
                                entry2.getKey(),
                                entry2.getValue().methodBehaviors().entrySet().stream()
                                    .map(
                                        entry3 -> new BehaviourSelectionSnapshotDTO(
                                            entry3.getKey(),
                                            entry3.getValue()
                                        )
                                    )
                                    .toList()
                            )
                        )
                        .toList()
                )
            )
            .toList();


    }

    private List<MetricValueDTO> systemMetricsToDto(List<AggregateMetric> systemMetrics) {
        return systemMetrics.stream().map(
            el -> new MetricValueDTO(
                el.name(),
                el.value(),
                el.unit()
            )
        )
        .toList();
    }

    private List<MetricConfigurationDTO> metricsConfigToDto(MetricsConfiguration metricsConfiguration) {

        return metricsConfiguration.getMetricsConfig().stream().map(
            el -> new MetricConfigurationDTO(
                el.metricName(),
                el.order(),
                el.unit(),
                el.direction().getDirection()
            )
        )
        .toList();
    }




}
