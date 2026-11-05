package org.example.cinema.dto;

import java.math.BigDecimal;


public class ResumoFilme {
    private final Integer id;
    private final String titulo;
    private int sessoes;
    private long vendidos;
    private BigDecimal faturamento = BigDecimal.ZERO;

    public ResumoFilme(Integer id, String titulo) {
        this.id = id;
        this.titulo = titulo;
    }

    public void somar(ResumoSessao s) {
        sessoes++;
        vendidos += s.getVendidos();
        faturamento = faturamento.add(s.getFaturamento());
    }

    public Integer getId() { return id; }
    public String getTitulo() { return titulo; }
    public int getSessoes() { return sessoes; }
    public long getVendidos() { return vendidos; }
    public BigDecimal getFaturamento() { return faturamento; }
}
