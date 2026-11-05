package org.example.cinema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessao",
        uniqueConstraints = @UniqueConstraint(name = "uq_sessao_sala_horario", columnNames = {"id_sala", "data_hora"}))
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sessao")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_filme", nullable = false)
    private Filme filme;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sala", nullable = false)
    private Sala sala;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "preco", nullable = false, precision = 8, scale = 2)
    private BigDecimal preco;

    public Sessao() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Filme getFilme() { return filme; }
    public void setFilme(Filme filme) { this.filme = filme; }
    public Sala getSala() { return sala; }
    public void setSala(Sala sala) { this.sala = sala; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
}
