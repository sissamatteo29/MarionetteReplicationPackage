package org.marionette.controlplane.adapters.outbound.changeconfig;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesServiceUrlParser {
    private static final Pattern K8S_SERVICE_URL_PATTERN = 
        Pattern.compile("^https?://([^.]+)\\.([^.]+)\\.svc\\.cluster\\.local(?::(\\d+))?.*$");
    
    public static Optional<KubernetesServiceInfo> parseServiceUrl(String url) {
        Matcher matcher = K8S_SERVICE_URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            String serviceName = matcher.group(1);
            String namespace = matcher.group(2);
            String port = matcher.group(3); // Optional port
            
            return Optional.of(new KubernetesServiceInfo(serviceName, namespace, port));
        }
        return Optional.empty();
    }
    
    public record KubernetesServiceInfo(String serviceName, String namespace, String port) {}
}
