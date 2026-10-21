package edu.cu.releaselab;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api")
public class PipelineController {
    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return pipelineService.overview();
    }

    @PostMapping("/pipeline/run")
    public Map<String, Object> runPipeline() {
        return pipelineService.runPipeline();
    }

    @PostMapping("/deployment/canary")
    public Map<String, Object> startCanary() {
        return pipelineService.startCanary();
    }

    @PostMapping("/deployment/promote")
    public Map<String, Object> promoteCanary() {
        return pipelineService.promoteCanary();
    }

    @PostMapping("/deployment/rollback")
    public Map<String, Object> rollback() {
        return pipelineService.rollback();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }
}
