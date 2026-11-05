package org.example.cinema.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingresso",
        uniqueConstraints = @UniqueConstraint(name = "uq_ingresso_sessao_assento", columnNames = {"id_sessao", "id_assento"}))
public class Ingresso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingresso")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sessao", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sessao sessao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_assento", nullable = false)
    private Assento assento;

    @Column(name = "valor_pago", nullable = false, precision = 8, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "data_venda", nullable = false)
    private LocalDateTime dataVenda;

    public Ingresso() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Sessao getSessao() { return sessao; }
    public void setSessao(Sessao sessao) { this.sessao = sessao; }
    public Assento getAssento() { return assento; }
    public void setAssento(Assento assento) { this.assento = assento; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public LocalDateTime getDataVenda() { return dataVenda; }
    public void setDataVenda(LocalDateTime dataVenda) { this.dataVenda = dataVenda; }
}
