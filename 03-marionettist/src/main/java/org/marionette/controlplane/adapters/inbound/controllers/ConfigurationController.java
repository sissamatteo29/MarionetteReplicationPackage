package org.marionette.controlplane.adapters.inbound.controllers;

import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;
import org.marionette.controlplane.usecases.inbound.AbnTestAllSystemConfigurationsUseCase;
import org.marionette.controlplane.usecases.inbound.ChangeMarionetteServiceBehaviourUseCase;
import org.marionette.controlplane.usecases.inbound.ReadAllMarionetteConfigsUseCase;
import org.marionette.controlplane.usecases.inbound.TriggerServiceRediscoveryUseCase;
import org.marionette.controlplane.usecases.inbound.changebehaviour.ChangeMarionetteServiceBehaviourRequest;
import org.marionette.controlplane.usecases.inbound.readconfigs.ReadAllMarionetteConfigsResponse;
import org.marionette.controlplane.adapters.inbound.dto.*;
import org.marionette.controlplane.adapters.outbound.changeconfig.InboundChangeBehaviourRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ConfigurationController {

    private final ReadAllMarionetteConfigsUseCase readAllConfigsUseCase;
    private final ChangeMarionetteServiceBehaviourUseCase changeBehaviourUseCase;
    private final TriggerServiceRediscoveryUseCase rediscoveryUseCase;
    private final AbnTestAllSystemConfigurationsUseCase abnTestUseCase;

    public ConfigurationController(ReadAllMarionetteConfigsUseCase readAllConfigsUseCase,
            ChangeMarionetteServiceBehaviourUseCase changeBehaviourUseCase,
            TriggerServiceRediscoveryUseCase rediscoveryUseCase,
            AbnTestAllSystemConfigurationsUseCase abnTestUseCase) {
        this.readAllConfigsUseCase = readAllConfigsUseCase;
        this.changeBehaviourUseCase = changeBehaviourUseCase;
        this.rediscoveryUseCase = rediscoveryUseCase;
        this.abnTestUseCase = abnTestUseCase;
    }

    /**
     * GET /api/services - Get all services with their current runtime
     * configurations
     */
    @GetMapping("/services")
    public ResponseEntity<AllServiceConfigsDTO> getAllServices() {

        ReadAllMarionetteConfigsResponse allConfigs = readAllConfigsUseCase.execute();

        AllServiceConfigsDTO response = mapToDTO(allConfigs);

        return ResponseEntity.ok(response);

    }


    @PutMapping("/services/{serviceName}/changeBehaviour")
    public ResponseEntity<?> changeBehaviour(
        @PathVariable String serviceName,
        @RequestBody InboundChangeBehaviourRequestDTO changeRequestDTO
    ) {
        // Required by API contract
        requireNonNull(serviceName, "The name of the service in the request to change behaviour was absent");

        logChangeBehaviourRequest(serviceName, changeRequestDTO);
        ChangeMarionetteServiceBehaviourRequest request = mapToRequest(serviceName, changeRequestDTO);
        
        // Use case
        changeBehaviourUseCase.execute(request);
        
        // Result handling




        return ResponseEntity.ok("success");
    }










    private void logChangeBehaviourRequest(String serviceName, InboundChangeBehaviourRequestDTO changeRequestDTO) {
        System.out.println("Received request to change beahviour for service " + serviceName + ", class " + changeRequestDTO.className() + ", method " + changeRequestDTO.methodName() + ", new behaviour " + changeRequestDTO.methodName());
    }

    private ChangeMarionetteServiceBehaviourRequest mapToRequest(String serviceName,
            InboundChangeBehaviourRequestDTO changeRequestDTO) {

        return new ChangeMarionetteServiceBehaviourRequest(
            serviceName, 
            changeRequestDTO.className(),
            changeRequestDTO.methodName(),
            changeRequestDTO.behaviourId()
        );

    }

    private AllServiceConfigsDTO mapToDTO(ReadAllMarionetteConfigsResponse allConfigs) {
        List<ServiceConfigDTO> serviceDtos = new ArrayList<>();
        for (ServiceConfigData serviceConfigData : allConfigs.serviceConfigs()) {

            serviceDtos.add(
                    new ServiceConfigDTO(serviceConfigData.serviceName(),
                            serviceConfigData.classConfigs().stream().map(
                                    classConfigData -> {
                                        return new ClassConfigDTO(classConfigData.className(),
                                                classConfigData.methodConfigData().stream().map(
                                                        methodConfigData -> {
                                                            System.out.println("DEBUG: Controller mapping MethodConfigData - " +
                                                                "method: " + methodConfigData.methodName() + 
                                                                ", default: " + methodConfigData.defaultBehaviourId() + 
                                                                ", current: " + methodConfigData.currentBehaviourId() + 
                                                                ", available: " + methodConfigData.availableBehaviourIds());
                                                            return new MethodConfigDTO(methodConfigData.methodName(),
                                                                    methodConfigData.defaultBehaviourId(),
                                                                    methodConfigData.currentBehaviourId(),
                                                                    methodConfigData.availableBehaviourIds());
                                                        }).collect(Collectors.toList()));
                                    }).collect(Collectors.toList())));

        }
        return new AllServiceConfigsDTO(serviceDtos);
    }

    /**
     * POST /api/services/discover - Trigger full service rediscovery
     * This will flush the current configuration registry and restart the discovery process
     */
    @PostMapping("/services/discover")
    public ResponseEntity<String> triggerServiceDiscovery(@RequestParam(defaultValue = "false") boolean fullRefresh) {
        try {
            if (fullRefresh) {
                // Full rediscovery: flush registry and restart discovery
                rediscoveryUseCase.execute();
                return ResponseEntity.ok("Full service rediscovery completed successfully");
            } else {
                // For now, we'll treat non-full refresh the same as full refresh
                // You could implement incremental discovery here if needed
                rediscoveryUseCase.execute();
                return ResponseEntity.ok("Service discovery completed successfully");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Service discovery failed: " + e.getMessage());
        }
    }

    /**
     * POST /api/services/start-ab-test - Start A/B testing with specified duration
     */
    @PostMapping("/services/start-ab-test")
    public ResponseEntity<String> startAbTest(@RequestParam int durationSeconds) {
        try {
            if (durationSeconds <= 0) {
                return ResponseEntity.badRequest()
                    .body("Duration must be a positive number of seconds");
            }
            
            Duration totalDuration = Duration.ofSeconds(durationSeconds);
            abnTestUseCase.execute(totalDuration);
            
            return ResponseEntity.ok("A/B test started successfully with duration: " + durationSeconds + " seconds");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to start A/B test: " + e.getMessage());
        }
    }

}