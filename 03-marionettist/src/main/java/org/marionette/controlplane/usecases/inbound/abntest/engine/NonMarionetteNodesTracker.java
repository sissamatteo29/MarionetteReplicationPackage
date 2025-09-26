package org.marionette.controlplane.usecases.inbound.abntest.engine;

import java.util.Set;

public interface NonMarionetteNodesTracker {

    public Set<String> retrieveNonMarionetteNodeNames();

}
