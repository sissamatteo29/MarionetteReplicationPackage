package org.marionette.controlplane.usecases.inbound.abntest.engine;

import java.util.LinkedList;
import java.util.List;

import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SystemBehaviourConfiguration;
import org.marionette.controlplane.usecases.inbound.abntest.domain.VariationPoint;

public class SystemConfigurationsGenerator {


    public List<SystemBehaviourConfiguration> generateAllSystemConfigurations(List<VariationPoint> variationPoints) {

        SystemConfigurationsGeneratorLogger.logGenerationStart(variationPoints);

        List<SystemBehaviourConfiguration> allBehaviourConfigurations = new LinkedList<>();

        SystemBehaviourConfiguration currentlyExploredConfiguration = new SystemBehaviourConfiguration();

        generateAllCombinations(
            allBehaviourConfigurations,
            0,
            currentlyExploredConfiguration,
            variationPoints
        );

        SystemConfigurationsGeneratorLogger.logGenerationResults(variationPoints, allBehaviourConfigurations);

        return allBehaviourConfigurations;
        
    }

    private void generateAllCombinations(List<SystemBehaviourConfiguration> allBehaviourConfigurations, int i,
            SystemBehaviourConfiguration currentlyExploredConfiguration,
            List<VariationPoint> variationPoints) {
        
        // Base case - All variation points have a selection
        if(i == variationPoints.size()) {
            allBehaviourConfigurations.add(SystemBehaviourConfiguration.copyOf(currentlyExploredConfiguration));
            return;
        }

        for(BehaviourId behaviourId : variationPoints.get(i).behaviours()) {
            currentlyExploredConfiguration.selectBehaviour(
                variationPoints.get(i),
                behaviourId
            );

            // Recursive call to allow the next variation point to make its choice
            generateAllCombinations(allBehaviourConfigurations, i + 1, currentlyExploredConfiguration, variationPoints);

            // Returning up, cleanup the currentlyExploredConfiguration data structure
            currentlyExploredConfiguration.removeSelection(variationPoints.get(i));
        }


    } 
    
}
