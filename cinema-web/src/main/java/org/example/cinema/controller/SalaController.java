package org.example.cinema.controller;

import jakarta.validation.Valid;
import org.example.cinema.dto.AssentoMapa;
import org.example.cinema.dto.FileiraMapa;
import org.example.cinema.form.SalaForm;
import org.example.cinema.model.Assento;
import org.example.cinema.model.Sala;
import org.example.cinema.model.Sessao;
import org.example.cinema.repository.AssentoRepository;
import org.example.cinema.repository.IngressoRepository;
import org.example.cinema.repository.SalaRepository;
import org.example.cinema.repository.SessaoRepository;
import org.example.cinema.service.IngressoService;
import org.example.cinema.service.SalaService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/salas")
public class SalaController {

    private final SalaRepository repository;
    private final AssentoRepository assentoRepository;
    private final SessaoRepository sessaoRepository;
    private final IngressoRepository ingressoRepository;
    private final SalaService salaService;
    private final IngressoService ingressoService;

    public SalaController(SalaRepository repository, AssentoRepository assentoRepository,
                          SessaoRepository sessaoRepository, IngressoRepository ingressoRepository,
                          SalaService salaService, IngressoService ingressoService) {
        this.repository = repository;
        this.assentoRepository = assentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.ingressoRepository = ingressoRepository;
        this.salaService = salaService;
        this.ingressoService = ingressoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("salas", repository.findAll(Sort.by("nome")));
        return "salas/lista";
    }

    // CADASTRO (gera os assentos automaticamente)
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new SalaForm());
        return "salas/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") SalaForm form, BindingResult result,
                        RedirectAttributes redirect) {
        if (!result.hasErrors() && form.getCapacidade() > form.getAssentosPorFileira() * 26) {
            result.rejectValue("assentosPorFileira", "limite",
                    "Limite de 26 fileiras (A–Z) excedido. Aumente os assentos por fileira.");
        }
        if (result.hasErrors()) {
            return "salas/form";
        }
        try {
            salaService.criarComAssentos(form.getNome().trim(), form.getCapacidade(), form.getAssentosPorFileira());
        } catch (DataIntegrityViolationException e) {
            result.rejectValue("nome", "duplicado", "Já existe uma sala com esse nome.");
            return "salas/form";
        }
        redirect.addFlashAttribute("sucesso", "Sala \"" + form.getNome().trim() + "\" criada com "
                + form.getCapacidade() + " assentos.");
        return "redirect:/salas";
    }

    // EDIÇÃO: dados da sala (nome, nº de assentos) + mapa de assentos com venda
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, @RequestParam(required = false) Integer sessaoId,
                         Model model, RedirectAttributes redirect) {
        Optional<Sala> opt = repository.findById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("erro", "Sala não encontrada.");
            return "redirect:/salas";
        }
        Sala sala = opt.get();
        SalaForm form = new SalaForm();
        form.setNome(sala.getNome());
        form.setCapacidade((int) assentoRepository.countBySalaId(id));
        Integer maior = assentoRepository.maiorNumero(id);
        form.setAssentosPorFileira(maior == null ? 10 : maior);
        preparar(model, sala, form, sessaoId);
        return "salas/editar";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id, @Valid @ModelAttribute("form") SalaForm form,
                            BindingResult result, @RequestParam(required = false) Integer sessaoId,
                            Model model, RedirectAttributes redirect) {
        Optional<Sala> opt = repository.findById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("erro", "Sala não encontrada.");
            return "redirect:/salas";
        }
        Sala sala = opt.get();
        if (!result.hasErrors() && form.getCapacidade() > form.getAssentosPorFileira() * 26) {
            result.rejectValue("assentosPorFileira", "limite",
                    "Limite de 26 fileiras (A–Z) excedido. Aumente os assentos por fileira.");
        }
        if (!result.hasErrors()) {
            try {
                salaService.atualizar(id, form.getNome().trim(), form.getCapacidade(), form.getAssentosPorFileira());
            } catch (DataIntegrityViolationException e) {
                result.rejectValue("nome", "duplicado", "Já existe uma sala com esse nome.");
            } catch (IllegalArgumentException e) {
                result.rejectValue("capacidade", "emUso", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            preparar(model, sala, form, sessaoId);
            return "salas/editar";
        }
        redirect.addFlashAttribute("sucesso", "Sala atualizada: " + form.getCapacidade() + " assentos.");
        return voltar(id, sessaoId);
    }

    // VENDA pelo mapa: assentos marcados viram ingressos da sessão escolhida
    @PostMapping("/{id}/vender")
    public String vender(@PathVariable Integer id, @RequestParam(required = false) Integer sessaoId,
                         @RequestParam(required = false) List<Integer> assentoIds, RedirectAttributes redirect) {
        if (sessaoId == null) {
            redirect.addFlashAttribute("erro", "Escolha uma sessão para vender ingressos.");
        } else if (assentoIds == null || assentoIds.isEmpty()) {
            redirect.addFlashAttribute("erro", "Selecione pelo menos um assento livre.");
        } else {
            try {
                List<String> rotulos = ingressoService.vender(id, sessaoId, assentoIds);
                redirect.addFlashAttribute("sucesso", "Venda realizada: " + String.join(", ", rotulos) + ".");
            } catch (IllegalArgumentException e) {
                redirect.addFlashAttribute("erro", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                redirect.addFlashAttribute("erro", "Algum assento acabou de ser vendido. Confira o mapa e tente de novo.");
            }
        }
        return voltar(id, sessaoId);
    }

    // CANCELAMENTO pelo mapa: assentos vendidos marcados voltam a ficar livres
    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, @RequestParam(required = false) Integer sessaoId,
                           @RequestParam(required = false) List<Integer> assentoIds, RedirectAttributes redirect) {
        if (sessaoId == null) {
            redirect.addFlashAttribute("erro", "Escolha uma sessão.");
        } else if (assentoIds == null || assentoIds.isEmpty()) {
            redirect.addFlashAttribute("erro", "Selecione pelo menos um assento vendido (vermelho).");
        } else {
            try {
                List<String> rotulos = ingressoService.cancelar(id, sessaoId, assentoIds);
                if (rotulos.isEmpty()) {
                    redirect.addFlashAttribute("erro", "Os assentos selecionados estão livres: não há ingresso para cancelar.");
                } else {
                    redirect.addFlashAttribute("sucesso", "Ingresso(s) cancelado(s): " + String.join(", ", rotulos) + ".");
                }
            } catch (IllegalArgumentException e) {
                redirect.addFlashAttribute("erro", e.getMessage());
            }
        }
        return voltar(id, sessaoId);
    }

    // EXCLUSÃO (os assentos são apagados em cascata pelo banco)
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirect) {
        Optional<Sala> sala = repository.findById(id);
        if (sala.isEmpty()) {
            redirect.addFlashAttribute("erro", "Sala não encontrada.");
            return "redirect:/salas";
        }
        try {
            repository.delete(sala.get());
            redirect.addFlashAttribute("sucesso", "Sala \"" + sala.get().getNome() + "\" excluída.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("erro", "Esta sala possui sessões cadastradas. Exclua as sessões antes.");
        }
        return "redirect:/salas";
    }

    private String voltar(Integer salaId, Integer sessaoId) {
        return "redirect:/salas/" + salaId + "/editar" + (sessaoId != null ? "?sessaoId=" + sessaoId : "");
    }

    private void preparar(Model model, Sala sala, SalaForm form, Integer sessaoId) {
        model.addAttribute("sala", sala);
        model.addAttribute("form", form);

        List<Sessao> sessoesDaSala = sessaoRepository.findBySalaIdOrderByDataHora(sala.getId());
        Sessao selecionada = null;
        if (sessaoId != null) {
            for (Sessao s : sessoesDaSala) {
                if (s.getId().equals(sessaoId)) {
                    selecionada = s;
                }
            }
        }
        Set<Integer> vendidos = selecionada == null
                ? Collections.emptySet()
                : new HashSet<>(ingressoRepository.assentosVendidos(selecionada.getId()));

        Map<String, List<AssentoMapa>> grupos = new LinkedHashMap<>();
        int total = 0;
        for (Assento a : assentoRepository.findBySalaId(sala.getId(), Sort.by("fileira", "numero"))) {
            grupos.computeIfAbsent(a.getFileira(), k -> new ArrayList<>())
                    .add(new AssentoMapa(a.getId(), a.getRotulo(), vendidos.contains(a.getId())));
            total++;
        }
        List<FileiraMapa> fileiras = new ArrayList<>();
        int maxColunas = 0;
        for (Map.Entry<String, List<AssentoMapa>> e : grupos.entrySet()) {
            fileiras.add(new FileiraMapa(e.getKey(), e.getValue()));
            maxColunas = Math.max(maxColunas, e.getValue().size());
        }

        model.addAttribute("sessoesDaSala", sessoesDaSala);
        model.addAttribute("sessaoSelecionada", selecionada);
        model.addAttribute("fileiras", fileiras);
        model.addAttribute("totalAssentos", total);
        model.addAttribute("totalVendidos", vendidos.size());
        model.addAttribute("lado", ladoDoAssento(maxColunas, fileiras.size()));
    }


    static int ladoDoAssento(int colunas, int fileiras) {
        int porColunas = colunas <= 8 ? 64 : colunas <= 12 ? 54 : colunas <= 16 ? 46 : colunas <= 20 ? 40 : colunas <= 26 ? 34 : 28;
        int porFileiras = fileiras <= 6 ? 64 : fileiras <= 10 ? 54 : fileiras <= 16 ? 44 : 36;
        return Math.min(porColunas, porFileiras);
    }
}
