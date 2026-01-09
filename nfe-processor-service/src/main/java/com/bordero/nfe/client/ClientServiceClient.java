package com.bordero.nfe.client;

import com.bordero.nfe.client.dto.ClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "client-service")
public interface ClientServiceClient {

    @GetMapping("/clients")
    List<ClientDTO> getAllClients();

    @GetMapping("/clients/{id}")
    ClientDTO getClientById(@PathVariable("id") Long id);

    @GetMapping("/clients/cnpj/{cnpj}")
    ClientDTO buscarPorCnpj(@PathVariable("cnpj") String cnpj);

}