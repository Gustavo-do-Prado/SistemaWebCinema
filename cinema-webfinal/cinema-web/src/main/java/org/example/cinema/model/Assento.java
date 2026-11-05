package org.example.cinema.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "assento",
        uniqueConstraints = @UniqueConstraint(name = "uq_assento_posicao", columnNames = {"id_sala", "fileira", "numero"}))
public class Assento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_assento")
    private Integer id;

    // ON DELETE CASCADE, como no Cinemaa.sql
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sala", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sala sala;

    // CHAR(1) no MySQL
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fileira", nullable = false, length = 1)
    private String fileira;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    public Assento() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Sala getSala() { return sala; }
    public void setSala(Sala sala) { this.sala = sala; }
    public String getFileira() { return fileira; }
    public void setFileira(String fileira) { this.fileira = fileira; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getRotulo() { return fileira + numero; }
}
