package org.marionette.controlplane.usecases.outbound.servicediscovery;

import java.util.List;

import org.marionette.controlplane.usecases.domain.dto.DiscoveredServiceMetadata;

public interface FindCandidateServicesPort {

    public List<DiscoveredServiceMetadata> findCandidateServices();
    
}
