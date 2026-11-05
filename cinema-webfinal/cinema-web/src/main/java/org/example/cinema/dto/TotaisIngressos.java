package org.example.cinema.dto;

import java.math.BigDecimal;
import java.util.List;

public class TotaisIngressos {
    private final String escopo;
    private final long vendidos;
    private final BigDecimal faturamento;
    private final int lugares;
    private final int sessoes;
    private final List<ResumoFilme> porFilme;
    private final List<ResumoSessao> porSessao;

    public TotaisIngressos(String escopo, long vendidos, BigDecimal faturamento, int lugares, int sessoes,
                           List<ResumoFilme> porFilme, List<ResumoSessao> porSessao) {
        this.escopo = escopo;
        this.vendidos = vendidos;
        this.faturamento = faturamento;
        this.lugares = lugares;
        this.sessoes = sessoes;
        this.porFilme = porFilme;
        this.porSessao = porSessao;
    }

    public String getEscopo() { return escopo; }
    public long getVendidos() { return vendidos; }
    public BigDecimal getFaturamento() { return faturamento; }
    public int getLugares() { return lugares; }
    public int getSessoes() { return sessoes; }
    public List<ResumoFilme> getPorFilme() { return porFilme; }
    public List<ResumoSessao> getPorSessao() { return porSessao; }

    public int getOcupacao() {
        return lugares <= 0 ? 0 : (int) Math.round(vendidos * 100.0 / lugares);
    }
}
