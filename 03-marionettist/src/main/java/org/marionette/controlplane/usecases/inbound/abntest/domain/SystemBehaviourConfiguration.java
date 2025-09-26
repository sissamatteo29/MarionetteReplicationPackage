package org.marionette.controlplane.usecases.inbound.abntest.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.marionette.controlplane.domain.values.BehaviourId;

/**
 * Represents the selection of behaviours for VarianionPoints
 */
public class SystemBehaviourConfiguration implements Iterable<SingleBehaviourSelection> {

    private final Map<VariationPoint, BehaviourId> behaviourSelections = new HashMap<>();

    public Map<VariationPoint, BehaviourId> getBehaviourSelections() {
        return Map.copyOf(behaviourSelections);
    }

    public void selectBehaviour(VariationPoint variation, BehaviourId selection) {
        behaviourSelections.put(variation, selection);
    }

    public BehaviourId getBehaviourSelection(VariationPoint variation) {
        return behaviourSelections.get(variation);
    }

    public void removeSelection(VariationPoint variation) {
        behaviourSelections.remove(variation);
    }

    public static SystemBehaviourConfiguration copyOf(SystemBehaviourConfiguration source) {
        SystemBehaviourConfiguration copy =  new SystemBehaviourConfiguration();
        for(Map.Entry<VariationPoint, BehaviourId> entry : source.behaviourSelections.entrySet()) {
            copy.selectBehaviour(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    @Override
    public Iterator<SingleBehaviourSelection> iterator() {
        // Return unmodifiable iterator to preserve encapsulation
        return behaviourSelections.entrySet().stream().map(
            entry -> new SingleBehaviourSelection(entry.getKey(), entry.getValue())
        ).iterator();
    }



    @Override
    public String toString() {
        if (behaviourSelections.isEmpty()) {
            return "VariationSelector [no selections]";
        }

        String selectionsStr = behaviourSelections.entrySet().stream()
                .map(entry -> {
                    VariationPoint vp = entry.getKey();
                    BehaviourId behaviour = entry.getValue();
                    return String.format("    %s.%s.%s -> %s",
                            vp.serviceName(),
                            vp.className(),
                            vp.methodName(),
                            behaviour.getBehaviourId());
                })
                .collect(Collectors.joining("\n"));

        return String.format("VariationSelector [\n%s\n]", selectionsStr);
    }

}
