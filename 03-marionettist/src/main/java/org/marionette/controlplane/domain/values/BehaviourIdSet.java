package org.marionette.controlplane.domain.values;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BehaviourIdSet implements Iterable<BehaviourId>{

    private final Set<BehaviourId> behaviours;

    
    private BehaviourIdSet(Set<BehaviourId> ids) {
        this.behaviours = Set.copyOf(ids);
    }
    
    public static BehaviourIdSet of(Collection<BehaviourId> behaviours) {
        Objects.requireNonNull(behaviours, "Impossible to create BehaviourIdSet from null reference");
        return new BehaviourIdSet(new HashSet<>(behaviours));
    }

    public static BehaviourIdSet fromStringCollection(Collection<String> strings) {
        Objects.requireNonNull(strings, "Trying to build a BehaviourIdSet from null collection of strings");
        return of(strings.stream().map(BehaviourId::new).toList());
    }
    
    public boolean contains(BehaviourId behaviour) {
        return behaviours.contains(behaviour);
    }

    public boolean isEmpty() {
        return behaviours.isEmpty();
    }

    public int behaviourNumber() {
        return behaviours.size();
    }

    public BehaviourIdSet add(BehaviourId newBehaviour) {
        Objects.requireNonNull(newBehaviour, "Trying to add a null BehaviourId in a BehaviourIdSet");
        Set<BehaviourId> newSet = new HashSet<>(behaviours);
        newSet.add(newBehaviour);
        return new BehaviourIdSet(newSet);
    }

    public BehaviourIdSet remove(BehaviourId toBeRemoved) {
        Objects.requireNonNull(toBeRemoved, "Trying to remove a null reference in a BehaviourIdSet");
        Set<BehaviourId> next = new HashSet<>(behaviours);
        next.remove(toBeRemoved);
        return new BehaviourIdSet(next);
    }

    public Set<BehaviourId> getBehaviours() {
        return Set.copyOf(behaviours);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((behaviours == null) ? 0 : behaviours.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BehaviourIdSet other = (BehaviourIdSet) obj;
        if (behaviours == null) {
            if (other.behaviours != null)
                return false;
        } else if (!behaviours.equals(other.behaviours))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[ " + getBehaviours().stream()
            .map(id -> id.getBehaviourId())
            .collect(Collectors.joining(", ")) + " ]";
    }

    @Override
    public Iterator<BehaviourId> iterator() {
        return behaviours.iterator();
    }

    


    
}
