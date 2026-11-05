package org.example.cinema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class ResumoSessao {
    private final Integer id;
    private final String filme;
    private final String sala;
    private final LocalDateTime dataHora;
    private final long vendidos;
    private final int lugares;
    private final BigDecimal faturamento;

    public ResumoSessao(Integer id, String filme, String sala, LocalDateTime dataHora,
                        long vendidos, int lugares, BigDecimal faturamento) {
        this.id = id;
        this.filme = filme;
        this.sala = sala;
        this.dataHora = dataHora;
        this.vendidos = vendidos;
        this.lugares = lugares;
        this.faturamento = faturamento;
    }

    public Integer getId() { return id; }
    public String getFilme() { return filme; }
    public String getSala() { return sala; }
    public LocalDateTime getDataHora() { return dataHora; }
    public long getVendidos() { return vendidos; }
    public int getLugares() { return lugares; }
    public BigDecimal getFaturamento() { return faturamento; }

    public int getOcupacao() {
        return lugares <= 0 ? 0 : (int) Math.round(vendidos * 100.0 / lugares);
    }
}
