package com.outfit.imagestore.__marionette;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/marionette/api")
public class BehaviourController {

    private ConfigurationService configurationService;

    public BehaviourController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping("/isMarionette")
    public ResponseEntity<String> validateMarionetteNode() {
        return ResponseEntity.ok("true");
    }

    @GetMapping("/getBehaviour")
    public String getBehaviour(@RequestParam String className, @RequestParam String methodName) {
        System.out.println("Requested behaviour for class " + className + " and method " + methodName);
        return BehaviourRegistry.getBehaviourId(className, methodName);
    }

    @GetMapping("/getConfiguration")
    public ResponseEntity<String> getConfiguration() {
        System.out.println("Received request to get the marionette configuration");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSONRegistrySerializer.generateJson(BehaviourRegistry.behaviourRegistry, BehaviourRegistry.serviceName));
    }

    @PostMapping("/changeBehaviour")
    public String changeBehaviour(@RequestBody BehaviourChangeRequest req) {
        System.out.println("Requested change behaviour for class " + req.getClassName() + " and method " + req.getMethodName() + " to behaviour " + req.getBehaviourId());
        BehaviourRegistry.setBehaviour(req.getClassName(), req.getMethodName(), req.getBehaviourId());
        return "Success";
    }
}
