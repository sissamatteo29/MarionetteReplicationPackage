package org.marionette.controlplane.adapters.inbound.downloadresult.dto;

import java.util.List;

public record ClassConfigSnapshotDTO (String className, List<BehaviourSelectionSnapshotDTO> behaviours) {
    
}
