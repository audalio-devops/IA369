package com.bordero.bordero.client;

import com.bordero.bordero.client.dto.ClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@FeignClient(name = "client-service")
public interface ClientServiceClient {

    @GetMapping("/clients/cnpj/{cnpj}")
    ClientDTO buscarPorCnpj(@PathVariable("cnpj") String cnpj);

    @PostMapping("/clients/cnpj/{cnpj}/descontar")
    void descontarLimite(@PathVariable("cnpj") String cnpj,
                         @RequestParam("valor") BigDecimal valor);
}