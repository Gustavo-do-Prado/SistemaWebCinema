package org.example.cinema.service;

import org.example.cinema.model.Assento;
import org.example.cinema.model.Sala;
import org.example.cinema.repository.AssentoRepository;
import org.example.cinema.repository.IngressoRepository;
import org.example.cinema.repository.SalaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SalaService {

    private final SalaRepository salas;
    private final AssentoRepository assentos;
    private final IngressoRepository ingressos;

    public SalaService(SalaRepository salas, AssentoRepository assentos, IngressoRepository ingressos) {
        this.salas = salas;
        this.assentos = assentos;
        this.ingressos = ingressos;
    }

    /** Posição i (0, 1, 2...) vira A1, A2, ..., B1, B2, ... conforme os assentos por fileira. */
    private static String fileiraDe(int i, int assentosPorFileira) {
        return String.valueOf((char) ('A' + i / assentosPorFileira));
    }

    private static int numeroDe(int i, int assentosPorFileira) {
        return i % assentosPorFileira + 1;
    }

    private static void validar(int capacidade, int assentosPorFileira) {
        if (capacidade <= 0 || assentosPorFileira <= 0) {
            throw new IllegalArgumentException("Capacidade e assentos por fileira devem ser maiores que zero.");
        }
        if (capacidade > assentosPorFileira * 26) {
            throw new IllegalArgumentException("Limite de 26 fileiras (A–Z) excedido. Aumente os assentos por fileira.");
        }
    }

    /** Cria a sala e gera os assentos (A1, A2, ... B1, B2, ...). Tudo na mesma transação. */
    @Transactional
    public Sala criarComAssentos(String nome, int capacidade, int assentosPorFileira) {
        validar(capacidade, assentosPorFileira);
        Sala sala = new Sala();
        sala.setNome(nome);
        sala.setCapacidade(capacidade);
        salas.save(sala);

        List<Assento> lista = new ArrayList<>();
        for (int i = 0; i < capacidade; i++) {
            lista.add(novoAssento(sala, i, assentosPorFileira));
        }
        assentos.saveAll(lista);
        return sala;
    }

    private static Assento novoAssento(Sala sala, int i, int assentosPorFileira) {
        Assento a = new Assento();
        a.setSala(sala);
        a.setFileira(fileiraDe(i, assentosPorFileira));
        a.setNumero(numeroDe(i, assentosPorFileira));
        return a;
    }

    /**
     * Atualiza o nome e a quantidade de assentos da sala.
     * Os assentos que já existem e continuam no novo desenho são mantidos (com seus ingressos);
     * ao aumentar, os novos seguem a sequência (A1, A2, ... B1, ...); ao diminuir, os últimos saem.
     * Não remove assentos que já têm ingressos vendidos.
     */
    @Transactional
    public void atualizar(Integer id, String nome, int capacidade, int assentosPorFileira) {
        validar(capacidade, assentosPorFileira);
        Sala sala = salas.findById(id).orElseThrow(() -> new IllegalArgumentException("Sala não encontrada."));

        List<Assento> atuais = assentos.findBySalaId(id, Sort.by("fileira", "numero"));
        Set<String> existentes = new HashSet<>();
        for (Assento a : atuais) {
            existentes.add(a.getRotulo());
        }
        Set<String> desejados = new LinkedHashSet<>();
        for (int i = 0; i < capacidade; i++) {
            desejados.add(fileiraDe(i, assentosPorFileira) + numeroDe(i, assentosPorFileira));
        }

        List<Assento> remover = new ArrayList<>();
        List<String> comIngresso = new ArrayList<>();
        for (Assento a : atuais) {
            if (!desejados.contains(a.getRotulo())) {
                remover.add(a);
                if (ingressos.existsByAssentoId(a.getId())) {
                    comIngresso.add(a.getRotulo());
                }
            }
        }
        if (!comIngresso.isEmpty()) {
            throw new IllegalArgumentException("Não é possível remover os assentos " + String.join(", ", comIngresso)
                    + ": já têm ingressos vendidos. Cancele os ingressos antes.");
        }
        assentos.deleteAll(remover);

        List<Assento> novos = new ArrayList<>();
        for (int i = 0; i < capacidade; i++) {
            String rotulo = fileiraDe(i, assentosPorFileira) + numeroDe(i, assentosPorFileira);
            if (!existentes.contains(rotulo)) {
                novos.add(novoAssento(sala, i, assentosPorFileira));
            }
        }
        assentos.saveAll(novos);

        sala.setNome(nome);
        sala.setCapacidade(capacidade);
        salas.save(sala);
    }
}
