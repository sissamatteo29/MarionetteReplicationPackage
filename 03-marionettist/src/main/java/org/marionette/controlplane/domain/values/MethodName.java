package org.marionette.controlplane.domain.values;


import org.marionette.controlplane.domain.helpers.StringValidator;

public class MethodName {

    private final String methodName;

    public MethodName(String methodName) {
        this.methodName = StringValidator.validateStringAndTrim(methodName);
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
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
        MethodName other = (MethodName) obj;
        if (methodName == null) {
            if (other.methodName != null)
                return false;
        } else if (!methodName.equals(other.methodName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return methodName;
    }
    
}
