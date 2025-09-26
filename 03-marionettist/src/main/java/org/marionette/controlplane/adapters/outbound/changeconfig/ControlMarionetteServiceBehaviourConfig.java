package org.marionette.controlplane.adapters.outbound.changeconfig;

public record ControlMarionetteServiceBehaviourConfig (String marionetteNodeInternalPath, int connectionTimeout, int readTimeout, int writeTimeout) {

    public static ControlMarionetteServiceBehaviourConfig defaultConfig() {
        return new ControlMarionetteServiceBehaviourConfig(
            "/marionette/api/changeBehaviour", 30_000, 30_000, 30_000);
    }


}
