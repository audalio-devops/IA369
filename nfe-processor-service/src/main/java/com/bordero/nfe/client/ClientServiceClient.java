package com.bordero.nfe.client;

import com.bordero.nfe.client.dto.ClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "client-service")
public interface ClientServiceClient {

    @GetMapping("/clients/cnpj/{cnpj}")
    ClientDTO buscarPorCnpj(@PathVariable("cnpj") String cnpj);
}