package org.example.cinema.repository;

import org.example.cinema.model.Filme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;


public interface FilmeRepository extends JpaRepository<Filme, Integer>, JpaSpecificationExecutor<Filme> {

    List<Filme> findAllByOrderByTituloAsc();
}
