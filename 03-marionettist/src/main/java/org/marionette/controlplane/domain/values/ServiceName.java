package org.marionette.controlplane.domain.values;

import static java.util.Objects.requireNonNull;

public class ServiceName {

    private final String serviceName;

    public ServiceName(String serviceName) {

        requireNonNull(serviceName, "The service name cannot be null");

        this.serviceName = serviceName;

    }

    public String getServiceName() {
        return serviceName;
    }
    

    public String toString() {
        return serviceName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
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
        ServiceName other = (ServiceName) obj;
        if (serviceName == null) {
            if (other.serviceName != null)
                return false;
        } else if (!serviceName.equals(other.serviceName))
            return false;
        return true;
    }


}
