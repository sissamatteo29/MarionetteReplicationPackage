package org.marionette.controlplane.domain.values;

import org.marionette.controlplane.domain.helpers.StringValidator;

public class ClassName {

    private final String className;

    public ClassName(String className) {
        this.className = StringValidator.validateStringAndTrim(className);
    }

    public String getClassName() {
        return className;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
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
        ClassName other = (ClassName) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return className;
    }

    
}
