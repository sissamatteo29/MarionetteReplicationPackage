package org.marionette.controlplane.usecases.inbound.abntest.engine;

import java.util.ArrayList;
import java.util.List;

import org.marionette.controlplane.domain.entities.ClassConfig;
import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.domain.entities.MethodConfig;
import org.marionette.controlplane.domain.entities.ServiceConfig;
import org.marionette.controlplane.usecases.inbound.abntest.domain.VariationPoint;

public class VariationPointsExtractor {

    private final ConfigRegistry globalRegistry;

    public VariationPointsExtractor(ConfigRegistry globalRegistry) {
        this.globalRegistry = globalRegistry;
    }

    public List<VariationPoint> extractAllVariationPoints() {

        System.out.println("\n== EXTRACTING VARIATION POINTS ==");
        System.out.println("The current status of the config registry is: " + globalRegistry);

        List<VariationPoint> variationPoints = new ArrayList<>();

        for (ServiceConfig service : globalRegistry.getAllServiceConfigs()) {
            for (ClassConfig clazz : globalRegistry.getClassConfigsForService(service.getServiceName())) {
                for (MethodConfig method : globalRegistry.getMethodConfigsForServiceAndClass(service.getServiceName(),
                        clazz.getClassName())) {
                    if (method.getAvailableBehaviourIds().behaviourNumber() > 1) { // At least 2 variants

                        variationPoints.add(
                                new VariationPoint(
                                        service.getServiceName(),
                                        clazz.getClassName(),
                                        method.getMethodName(),
                                        method.getAvailableBehaviourIds()));

                    }
                }
            }
        }

        printVariationPointsResults(variationPoints);

        return variationPoints;
    }

    private void printVariationPointsResults(List<VariationPoint> variationPoints) {
        System.out.println("\n== VARIATION POINTS EXTRACTION RESULTS ==");

        if (variationPoints.isEmpty()) {
            System.out.println("No variation points found (no methods with multiple behaviour variants).");
            return;
        }

        System.out.println("Found " + variationPoints.size() + " variation point(s):");
        System.out.println();

        for (int i = 0; i < variationPoints.size(); i++) {
            VariationPoint vp = variationPoints.get(i);
            System.out.printf("[%d] %s.%s.%s%n",
                    i + 1,
                    vp.serviceName(),
                    vp.className(),
                    vp.methodName());
            System.out.printf("    Available behaviours (%d): %s%n",
                    vp.behaviours().behaviourNumber(),
                    vp.behaviours());

            if (i < variationPoints.size() - 1) {
                System.out.println();
            }
        }

        System.out.println("\n== END EXTRACTION RESULTS ==\n");
    }

}
