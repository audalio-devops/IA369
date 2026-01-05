package com.bordero.bordero.repository;

import com.bordero.bordero.domain.model.Bordero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorderoRepository extends JpaRepository<Bordero, Long> {
}
