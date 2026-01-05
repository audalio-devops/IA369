package com.bordero.client.repository;

import com.bordero.client.domain.model.Client;
import com.bordero.client.domain.model.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
    boolean existsByEmail(String email);
    List<Client> findByStatus(ClientStatus status);
    List<Client> findByAtivoTrue();
}