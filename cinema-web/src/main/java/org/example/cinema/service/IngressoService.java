package org.example.cinema.service;

import org.example.cinema.model.Assento;
import org.example.cinema.model.Ingresso;
import org.example.cinema.model.Sessao;
import org.example.cinema.repository.AssentoRepository;
import org.example.cinema.repository.IngressoRepository;
import org.example.cinema.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Venda e cancelamento de ingressos pelo mapa da sala. */
@Service
public class IngressoService {

    private final IngressoRepository ingressos;
    private final SessaoRepository sessoes;
    private final AssentoRepository assentos;

    public IngressoService(IngressoRepository ingressos, SessaoRepository sessoes, AssentoRepository assentos) {
        this.ingressos = ingressos;
        this.sessoes = sessoes;
        this.assentos = assentos;
    }

    private Sessao sessaoDaSala(Integer salaId, Integer sessaoId) {
        Sessao sessao = sessoes.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada."));
        if (!sessao.getSala().getId().equals(salaId)) {
            throw new IllegalArgumentException("Esta sessão não pertence a esta sala.");
        }
        return sessao;
    }

    /**
     * Vende um ingresso (pelo preço da sessão) para cada assento. Tudo ou nada: se algum assento já estiver
     * vendido, nada é vendido. Devolve os rótulos vendidos (ex.: [A1, A2]).
     */
    @Transactional
    public List<String> vender(Integer salaId, Integer sessaoId, Collection<Integer> assentoIds) {
        Sessao sessao = sessaoDaSala(salaId, sessaoId);
        Set<Integer> ids = new LinkedHashSet<>(assentoIds);
        List<Assento> escolhidos = new ArrayList<>(assentos.findAllById(ids));
        if (escolhidos.size() != ids.size()) {
            throw new IllegalArgumentException("Assento não encontrado.");
        }
        escolhidos.sort(Comparator.comparing(Assento::getFileira).thenComparing(Assento::getNumero));

        Set<Integer> vendidos = new HashSet<>(ingressos.assentosVendidos(sessaoId));
        List<String> ocupados = new ArrayList<>();
        for (Assento a : escolhidos) {
            if (!a.getSala().getId().equals(salaId)) {
                throw new IllegalArgumentException("O assento " + a.getRotulo() + " não pertence a esta sala.");
            }
            if (vendidos.contains(a.getId())) {
                ocupados.add(a.getRotulo());
            }
        }
        if (!ocupados.isEmpty()) {
            throw new IllegalArgumentException("Já vendido(s) para esta sessão: " + String.join(", ", ocupados) + ".");
        }

        List<String> rotulos = new ArrayList<>();
        for (Assento a : escolhidos) {
            Ingresso ingresso = new Ingresso();
            ingresso.setSessao(sessao);
            ingresso.setAssento(a);
            ingresso.setValorPago(sessao.getPreco());
            ingresso.setDataVenda(LocalDateTime.now());
            ingressos.save(ingresso);
            rotulos.add(a.getRotulo());
        }
        return rotulos;
    }

    /** Cancela os ingressos dos assentos informados (o assento volta a ficar livre). Devolve os rótulos cancelados. */
    @Transactional
    public List<String> cancelar(Integer salaId, Integer sessaoId, Collection<Integer> assentoIds) {
        sessaoDaSala(salaId, sessaoId);
        List<String> rotulos = new ArrayList<>();
        for (Integer assentoId : new LinkedHashSet<>(assentoIds)) {
            Optional<Ingresso> ingresso = ingressos.findBySessaoIdAndAssentoId(sessaoId, assentoId);
            if (ingresso.isPresent()) {
                rotulos.add(ingresso.get().getAssento().getRotulo());
                ingressos.delete(ingresso.get());
            }
        }
        return rotulos;
    }
}
