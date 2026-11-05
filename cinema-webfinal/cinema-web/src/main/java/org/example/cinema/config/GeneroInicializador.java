package org.example.cinema.config;

import org.example.cinema.model.Genero;
import org.example.cinema.repository.GeneroRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class GeneroInicializador implements CommandLineRunner {

    public static final List<String> GENEROS = List.of(
            "Ação", "Aventura", "Animação", "Comédia", "Drama", "Ficção Científica",
            "Terror", "Suspense", "Romance", "Fantasia", "Documentário", "Musical");

    private final GeneroRepository repository;

    public GeneroInicializador(GeneroRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        for (String nome : GENEROS) {
            if (!repository.existsByNome(nome)) {
                repository.save(new Genero(nome));
            }
        }
    }
}
