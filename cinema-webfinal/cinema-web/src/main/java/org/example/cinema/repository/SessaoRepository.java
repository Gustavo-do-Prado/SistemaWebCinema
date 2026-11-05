package org.example.cinema.repository;

import org.example.cinema.model.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SessaoRepository extends JpaRepository<Sessao, Integer>, JpaSpecificationExecutor<Sessao> {

    @Query("select s from Sessao s join fetch s.filme join fetch s.sala order by s.dataHora")
    List<Sessao> listarTodas();

    List<Sessao> findBySalaIdOrderByDataHora(Integer salaId);
}
