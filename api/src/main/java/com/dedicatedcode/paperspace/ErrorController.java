package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ErrorController {
    private final BinaryService binaryService;

    public ErrorController(BinaryService binaryService) {
        this.binaryService = binaryService;
    }

    @GetMapping(value = "/api/errors.json",produces = "application/json")
    @ResponseBody
    public List<BinaryErrorResponse> errors() {
        List<Binary> binariesWithError = binaryService.getFailed();
        return binariesWithError.stream().map(BinaryErrorResponse::new).collect(Collectors.toList());
    }

    private static class BinaryErrorResponse {

        private final String path;
        private final Map<String, String> links = new HashMap<>();

        private BinaryErrorResponse(Binary binary) {
            this.path = binary.getStorageLocation();
            this.links.put("delete", "/api/binary/" + binary.getId() + "/delete");
            this.links.put("ignore", "/api/binary/" + binary.getId() + "/ignore");
        }

        public String getPath() {
            return path;
        }

        public Map<String, String> getLinks() {
            return links;
        }
    }
}
