package org.example.cinema.controller;

import org.example.cinema.model.Sessao;
import org.example.cinema.repository.FilmeRepository;
import org.example.cinema.repository.SessaoRepository;
import org.example.cinema.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/ingressos")
public class IngressoController {

    private final RelatorioService relatorio;
    private final FilmeRepository filmeRepository;
    private final SessaoRepository sessaoRepository;

    public IngressoController(RelatorioService relatorio, FilmeRepository filmeRepository,
                              SessaoRepository sessaoRepository) {
        this.relatorio = relatorio;
        this.filmeRepository = filmeRepository;
        this.sessaoRepository = sessaoRepository;
    }

    @GetMapping
    public String totais(@RequestParam(required = false) Integer filmeId,
                         @RequestParam(required = false) Integer sessaoId, Model model) {
        List<Sessao> sessoes = sessaoRepository.listarTodas().stream()
                .filter(s -> filmeId == null || s.getFilme().getId().equals(filmeId))
                .toList();
        model.addAttribute("totais", relatorio.totais(filmeId, sessaoId));
        model.addAttribute("filmes", filmeRepository.findAllByOrderByTituloAsc());
        model.addAttribute("sessoes", sessoes);
        model.addAttribute("filmeId", filmeId);
        model.addAttribute("sessaoId", sessaoId);
        return "ingressos/totais";
    }
}
