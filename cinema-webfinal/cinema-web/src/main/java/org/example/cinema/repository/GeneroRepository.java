package org.example.cinema.repository;

import org.example.cinema.model.Genero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeneroRepository extends JpaRepository<Genero, Integer> {

    Optional<Genero> findByNome(String nome);

    boolean existsByNome(String nome);
}
