package org.example.cinema.model;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "filme")
public class Filme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_filme")
    private Integer id;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "duracao_min", nullable = false)
    private Integer duracaoMin;

    @Column(name = "classificacao", nullable = false, length = 10)
    private String classificacao;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "filme_genero",
            joinColumns = @JoinColumn(name = "id_filme"),
            inverseJoinColumns = @JoinColumn(name = "id_genero"))
    @OrderBy("nome")
    private Set<Genero> generos = new LinkedHashSet<>();

    public Filme() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Integer getDuracaoMin() { return duracaoMin; }
    public void setDuracaoMin(Integer duracaoMin) { this.duracaoMin = duracaoMin; }
    public String getClassificacao() { return classificacao; }
    public void setClassificacao(String classificacao) { this.classificacao = classificacao; }
    public Set<Genero> getGeneros() { return generos; }
    public void setGeneros(Set<Genero> generos) { this.generos = generos; }

    /** Ex.: "Aventura, Drama, Ficção Científica" */
    public String getGenerosTexto() {
        return generos.stream().map(Genero::getNome).collect(Collectors.joining(", "));
    }
}
