package org.example.cinema.controller;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import org.example.cinema.form.FilmeForm;
import org.example.cinema.model.Filme;
import org.example.cinema.model.Genero;
import org.example.cinema.repository.FilmeRepository;
import org.example.cinema.repository.GeneroRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.Collator;
import java.util.*;

@Controller
@RequestMapping("/filmes")
public class FilmeController {

    private static final List<String> CLASSIFICACOES = List.of("L", "10", "12", "14", "16", "18");

    private static final Collator COLLATOR = criarCollator();

    private final FilmeRepository repository;
    private final GeneroRepository generoRepository;

    public FilmeController(FilmeRepository repository, GeneroRepository generoRepository) {
        this.repository = repository;
        this.generoRepository = generoRepository;
    }

    private static Collator criarCollator() {
        Collator c = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        c.setStrength(Collator.SECONDARY);
        return c;
    }

    @ModelAttribute("classificacoes")
    public List<String> classificacoes() {
        return CLASSIFICACOES;
    }

    @ModelAttribute("generos")
    public List<Genero> generos() {
        return generoRepository.findAll(Sort.by("nome"));
    }

    // LISTAGEM: busca + filtros (classificação, gênero) + ordenação
    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) String classificacao,
                         @RequestParam(required = false) Integer generoId,
                         @RequestParam(defaultValue = "titulo") String ordem,
                         Model model) {
        String textoBusca = busca == null ? "" : busca.trim();
        String classif = classificacao == null ? "" : classificacao.trim();

        Specification<Filme> filtro = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (!textoBusca.isEmpty()) {
                condicoes.add(cb.like(cb.lower(root.get("titulo")), "%" + textoBusca.toLowerCase() + "%"));
            }
            if (!classif.isEmpty()) {
                condicoes.add(cb.equal(root.get("classificacao"), classif));
            }
            if (generoId != null) {
                query.distinct(true);
                condicoes.add(cb.equal(root.join("generos", JoinType.INNER).get("id"), generoId));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };

        List<Filme> lista = new ArrayList<>(repository.findAll(filtro));
        lista.sort(comparador(ordem));

        model.addAttribute("filmes", lista);
        model.addAttribute("busca", textoBusca);
        model.addAttribute("classificacao", classif);
        model.addAttribute("generoId", generoId);
        model.addAttribute("ordem", ordem);
        return "filmes/lista";
    }

    private static Comparator<Filme> comparador(String ordem) {
        Comparator<Filme> porTitulo = (a, b) -> COLLATOR.compare(a.getTitulo(), b.getTitulo());
        return switch (ordem) {
            case "titulo_desc" -> porTitulo.reversed();
            case "classificacao" -> Comparator
                    .comparingInt((Filme f) -> CLASSIFICACOES.indexOf(f.getClassificacao()))
                    .thenComparing(porTitulo);
            case "genero" -> Comparator
                    .comparing(FilmeController::primeiroGenero, (String a, String b) -> COLLATOR.compare(a, b))
                    .thenComparing(porTitulo);
            default -> porTitulo;
        };
    }

    /** Gêneros já vêm ordenados por nome; filmes sem gênero vão para o fim. */
    private static String primeiroGenero(Filme f) {
        return f.getGeneros().isEmpty() ? "￿" : f.getGeneros().iterator().next().getNome();
    }

    // CADASTRO
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new FilmeForm());
        return "filmes/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") FilmeForm form, BindingResult result,
                        RedirectAttributes redirect) {
        return salvar(null, form, result, redirect);
    }

    // EDIÇÃO
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes redirect) {
        Optional<Filme> opt = repository.findById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("erro", "Filme não encontrado.");
            return "redirect:/filmes";
        }
        Filme f = opt.get();
        FilmeForm form = new FilmeForm();
        form.setId(f.getId());
        form.setTitulo(f.getTitulo());
        form.setDuracaoMin(f.getDuracaoMin());
        form.setClassificacao(f.getClassificacao());
        form.setGeneroIds(new ArrayList<>(f.getGeneros().stream().map(Genero::getId).toList()));
        model.addAttribute("form", form);
        return "filmes/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id, @Valid @ModelAttribute("form") FilmeForm form,
                            BindingResult result, RedirectAttributes redirect) {
        form.setId(id);
        return salvar(id, form, result, redirect);
    }

    private String salvar(Integer id, FilmeForm form, BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "filmes/form";
        }
        Set<Integer> idsPedidos = new LinkedHashSet<>(form.getGeneroIds());
        List<Genero> escolhidos = generoRepository.findAllById(idsPedidos);
        if (escolhidos.size() != idsPedidos.size()) {
            result.rejectValue("generoIds", "invalido", "Gênero inválido.");
            return "filmes/form";
        }

        Filme filme;
        if (id == null) {
            filme = new Filme();
        } else {
            filme = repository.findById(id).orElse(null);
            if (filme == null) {
                redirect.addFlashAttribute("erro", "Filme não encontrado.");
                return "redirect:/filmes";
            }
        }
        filme.setTitulo(form.getTitulo().trim());
        filme.setDuracaoMin(form.getDuracaoMin());
        filme.setClassificacao(form.getClassificacao());
        filme.setGeneros(new LinkedHashSet<>(escolhidos));
        repository.save(filme);

        redirect.addFlashAttribute("sucesso", "Filme \"" + filme.getTitulo() + "\" "
                + (id == null ? "cadastrado" : "atualizado") + " com sucesso.");
        return "redirect:/filmes";
    }

    // EXCLUSÃO
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirect) {
        Optional<Filme> filme = repository.findById(id);
        if (filme.isEmpty()) {
            redirect.addFlashAttribute("erro", "Filme não encontrado.");
            return "redirect:/filmes";
        }
        try {
            repository.delete(filme.get());
            redirect.addFlashAttribute("sucesso", "Filme \"" + filme.get().getTitulo() + "\" excluído.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("erro",
                    "Este filme possui sessões cadastradas. Exclua as sessões antes de excluí-lo.");
        }
        return "redirect:/filmes";
    }
}
