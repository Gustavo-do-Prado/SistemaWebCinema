package org.example.cinema.controller;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import org.example.cinema.form.SessaoForm;
import org.example.cinema.model.Filme;
import org.example.cinema.model.Sala;
import org.example.cinema.model.Sessao;
import org.example.cinema.repository.FilmeRepository;
import org.example.cinema.repository.IngressoRepository;
import org.example.cinema.repository.SalaRepository;
import org.example.cinema.repository.SessaoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/sessoes")
public class SessaoController {

    private final SessaoRepository repository;
    private final FilmeRepository filmeRepository;
    private final SalaRepository salaRepository;
    private final IngressoRepository ingressoRepository;

    public SessaoController(SessaoRepository repository, FilmeRepository filmeRepository,
                            SalaRepository salaRepository, IngressoRepository ingressoRepository) {
        this.repository = repository;
        this.filmeRepository = filmeRepository;
        this.salaRepository = salaRepository;
        this.ingressoRepository = ingressoRepository;
    }

    @ModelAttribute("filmes")
    public List<Filme> filmes() {
        return filmeRepository.findAllByOrderByTituloAsc();
    }

    @ModelAttribute("salas")
    public List<Sala> salas() {
        return salaRepository.findAll(Sort.by("nome"));
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer filmeId,
                         @RequestParam(required = false) Integer salaId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate,
                         Model model) {
        Specification<Sessao> filtro = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filmeId != null) {
                condicoes.add(cb.equal(root.get("filme").get("id"), filmeId));
            }
            if (salaId != null) {
                condicoes.add(cb.equal(root.get("sala").get("id"), salaId));
            }
            if (de != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("dataHora"), de));
            }
            if (ate != null) {
                condicoes.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("dataHora"), ate));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        model.addAttribute("sessoes", repository.findAll(filtro, Sort.by("dataHora")));
        model.addAttribute("filmeId", filmeId);
        model.addAttribute("salaId", salaId);
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        return "sessoes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new SessaoForm());
        return "sessoes/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") SessaoForm form, BindingResult result,
                        RedirectAttributes redirect) {
        return salvar(null, form, result, redirect);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes redirect) {
        Optional<Sessao> opt = repository.findById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("erro", "Sessão não encontrada.");
            return "redirect:/sessoes";
        }
        Sessao s = opt.get();
        SessaoForm form = new SessaoForm();
        form.setId(s.getId());
        form.setFilmeId(s.getFilme().getId());
        form.setSalaId(s.getSala().getId());
        form.setDataHora(s.getDataHora());
        form.setPreco(s.getPreco());
        model.addAttribute("form", form);
        return "sessoes/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id, @Valid @ModelAttribute("form") SessaoForm form,
                            BindingResult result, RedirectAttributes redirect) {
        form.setId(id);
        return salvar(id, form, result, redirect);
    }

    private String salvar(Integer id, SessaoForm form, BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "sessoes/form";
        }
        Filme filme = filmeRepository.findById(form.getFilmeId()).orElse(null);
        Sala sala = salaRepository.findById(form.getSalaId()).orElse(null);
        if (filme == null) result.rejectValue("filmeId", "naoEncontrado", "Filme não encontrado.");
        if (sala == null) result.rejectValue("salaId", "naoEncontrada", "Sala não encontrada.");
        if (result.hasErrors()) {
            return "sessoes/form";
        }

        Sessao sessao;
        if (id == null) {
            sessao = new Sessao();
        } else {
            sessao = repository.findById(id).orElse(null);
            if (sessao == null) {
                redirect.addFlashAttribute("erro", "Sessão não encontrada.");
                return "redirect:/sessoes";
            }
            if (!sessao.getSala().getId().equals(sala.getId()) && ingressoRepository.countBySessaoId(id) > 0) {
                result.rejectValue("salaId", "temIngressos",
                        "Não é possível trocar a sala de uma sessão que já tem ingressos vendidos.");
                return "sessoes/form";
            }
        }
        sessao.setFilme(filme);
        sessao.setSala(sala);
        sessao.setDataHora(form.getDataHora());
        sessao.setPreco(form.getPreco());
        try {
            repository.save(sessao);
        } catch (DataIntegrityViolationException e) {
            result.rejectValue("dataHora", "duplicada", "Já existe uma sessão nessa sala nesse mesmo horário.");
            return "sessoes/form";
        }
        redirect.addFlashAttribute("sucesso", id == null ? "Sessão cadastrada." : "Sessão atualizada.");
        return "redirect:/sessoes";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirect) {
        Optional<Sessao> opt = repository.findById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("erro", "Sessão não encontrada.");
        } else {
            repository.delete(opt.get());
            redirect.addFlashAttribute("sucesso", "Sessão excluída.");
        }
        return "redirect:/sessoes";
    }
}
