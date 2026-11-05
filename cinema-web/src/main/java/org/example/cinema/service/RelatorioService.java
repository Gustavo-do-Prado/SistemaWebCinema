package org.example.cinema.service;

import org.example.cinema.dto.ResumoFilme;
import org.example.cinema.dto.ResumoSessao;
import org.example.cinema.dto.TotaisIngressos;
import org.example.cinema.model.Filme;
import org.example.cinema.model.Sessao;
import org.example.cinema.repository.FilmeRepository;
import org.example.cinema.repository.IngressoRepository;
import org.example.cinema.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** Totais de ingressos vendidos por filme e por sessão. */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final SessaoRepository sessoes;
    private final FilmeRepository filmes;
    private final IngressoRepository ingressos;

    public RelatorioService(SessaoRepository sessoes, FilmeRepository filmes, IngressoRepository ingressos) {
        this.sessoes = sessoes;
        this.filmes = filmes;
        this.ingressos = ingressos;
    }

    /**
     * @param filmeId  se informado, limita ao filme
     * @param sessaoId se informado, limita à sessão (tem prioridade sobre o filme)
     */
    public TotaisIngressos totais(Integer filmeId, Integer sessaoId) {
        // quantidade e valor vendidos, por id de sessão
        Map<Integer, Long> quantidades = new HashMap<>();
        Map<Integer, BigDecimal> valores = new HashMap<>();
        for (Object[] linha : ingressos.totaisPorSessao()) {
            Integer id = ((Number) linha[0]).intValue();
            quantidades.put(id, ((Number) linha[1]).longValue());
            valores.put(id, linha[2] == null ? BigDecimal.ZERO : (BigDecimal) linha[2]);
        }

        List<ResumoSessao> todas = new ArrayList<>();
        Map<Integer, ResumoFilme> porFilme = new LinkedHashMap<>();
        for (Filme f : filmes.findAllByOrderByTituloAsc()) {
            porFilme.put(f.getId(), new ResumoFilme(f.getId(), f.getTitulo()));
        }
        Map<Integer, Integer> filmeDaSessao = new HashMap<>();
        for (Sessao s : sessoes.listarTodas()) {
            ResumoSessao r = new ResumoSessao(s.getId(), s.getFilme().getTitulo(), s.getSala().getNome(),
                    s.getDataHora(), quantidades.getOrDefault(s.getId(), 0L), s.getSala().getCapacidade(),
                    valores.getOrDefault(s.getId(), BigDecimal.ZERO));
            todas.add(r);
            filmeDaSessao.put(s.getId(), s.getFilme().getId());
            porFilme.get(s.getFilme().getId()).somar(r);
        }

        // aplica o filtro: sessão tem prioridade sobre filme
        Integer filmeEscolhido = filmeId;
        if (sessaoId != null) {
            filmeEscolhido = filmeDaSessao.get(sessaoId);
        }
        List<ResumoSessao> selecionadas = new ArrayList<>();
        for (ResumoSessao r : todas) {
            boolean entra = sessaoId != null
                    ? r.getId().equals(sessaoId)
                    : (filmeId == null || Objects.equals(filmeDaSessao.get(r.getId()), filmeId));
            if (entra) {
                selecionadas.add(r);
            }
        }

        List<ResumoFilme> filmesExibidos = new ArrayList<>();
        for (ResumoFilme rf : porFilme.values()) {
            if (filmeEscolhido == null || rf.getId().equals(filmeEscolhido)) {
                filmesExibidos.add(rf);
            }
        }
        filmesExibidos.sort(Comparator.comparingLong(ResumoFilme::getVendidos).reversed()
                .thenComparing(ResumoFilme::getTitulo));

        long vendidos = 0;
        int lugares = 0;
        BigDecimal faturamento = BigDecimal.ZERO;
        for (ResumoSessao r : selecionadas) {
            vendidos += r.getVendidos();
            lugares += r.getLugares();
            faturamento = faturamento.add(r.getFaturamento());
        }

        String escopo;
        if (sessaoId != null && selecionadas.size() == 1) {
            ResumoSessao r = selecionadas.get(0);
            escopo = "Sessão: " + r.getFilme() + " · " + r.getSala();
        } else if (filmeEscolhido != null && !filmesExibidos.isEmpty()) {
            escopo = "Filme: " + filmesExibidos.get(0).getTitulo();
        } else {
            escopo = "Todos os filmes e sessões";
        }
        return new TotaisIngressos(escopo, vendidos, faturamento, lugares, selecionadas.size(),
                filmesExibidos, selecionadas);
    }
}
