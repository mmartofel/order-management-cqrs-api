package pl.piomin.services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the merged contract, {@code contract/openapi.json} of this repository, packaged into the gateway's jar. It is
 * the file CI checks, so it is the contract the running API was built from.
 */
@RestController
public class ContractController {

    private static final Resource CONTRACT = new ClassPathResource("contract/openapi.json");

    @GetMapping(value = "/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> contract() {
        return CONTRACT.exists() ? ResponseEntity.ok(CONTRACT) : ResponseEntity.notFound().build();
    }
}
