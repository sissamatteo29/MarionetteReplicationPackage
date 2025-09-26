package org.marionette.controlplane.adapters.inbound.downloadresult;

import org.marionette.controlplane.adapters.inbound.downloadresult.dto.AbnTestResultsDTO;
import org.marionette.controlplane.usecases.inbound.downloadresult.AbnTestResultsDownloadUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/downloadresult")
@CrossOrigin(origins = "*")
public class AbnTestDownloadResultsController {

    private final AbnTestResultsDownloadUseCase testResultsDownloadUseCase;

    public AbnTestDownloadResultsController(AbnTestResultsDownloadUseCase testResultsDownloadUseCase) {
        this.testResultsDownloadUseCase = testResultsDownloadUseCase;
    }

    @GetMapping("")
    public ResponseEntity<AbnTestResultsDTO> getAbnTestingResults() {

        AbnTestResultsDTO response = testResultsDownloadUseCase.execute().dto();

        return ResponseEntity.ok(response);

    }

    /**
     * GET /api/downloadresult/available - Check if test results are available for download
     */
    @GetMapping("/available")
    public ResponseEntity<Boolean> areResultsAvailable() {
        try {
            // Check if there are any results stored
            AbnTestResultsDTO results = testResultsDownloadUseCase.execute().dto();
            boolean hasResults = results != null && 
                                results.ranking() != null && 
                                !results.ranking().isEmpty();
            return ResponseEntity.ok(hasResults);
        } catch (Exception e) {
            // If there's any error (e.g., no results), return false
            return ResponseEntity.ok(false);
        }
    }
}
