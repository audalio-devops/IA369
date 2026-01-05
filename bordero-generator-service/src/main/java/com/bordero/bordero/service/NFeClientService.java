package com.bordero.bordero.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "nfe-processor-service")
public interface NFeClientService {

    @GetMapping("/nfe/{id}/data")
    Map<String, Object> buscarNFe(@PathVariable Long id);
}
