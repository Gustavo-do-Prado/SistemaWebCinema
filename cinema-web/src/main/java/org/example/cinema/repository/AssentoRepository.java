package org.example.cinema.repository;

import org.example.cinema.model.Assento;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssentoRepository extends JpaRepository<Assento, Integer> {

    List<Assento> findBySalaId(Integer salaId, Sort sort);

    long countBySalaId(Integer salaId);

    @Query("select max(a.numero) from Assento a where a.sala.id = :salaId")
    Integer maiorNumero(@Param("salaId") Integer salaId);
}
