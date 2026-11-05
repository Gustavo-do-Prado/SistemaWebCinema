package org.example.cinema;

import org.example.cinema.model.Filme;
import org.example.cinema.model.Genero;
import org.example.cinema.repository.FilmeRepository;
import org.example.cinema.repository.GeneroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilmeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired FilmeRepository repository;
    @Autowired GeneroRepository generos;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    private Genero genero(String nome) {
        return generos.findByNome(nome).orElseThrow();
    }

    private Filme salvar(String titulo, String classificacao, String... nomesDeGeneros) {
        Filme f = new Filme();
        f.setTitulo(titulo);
        f.setDuracaoMin(100);
        f.setClassificacao(classificacao);
        f.setGeneros(new LinkedHashSet<>());
        for (String nome : nomesDeGeneros) {
            f.getGeneros().add(genero(nome));
        }
        return repository.save(f);
    }

    @SuppressWarnings("unchecked")
    private List<Filme> listar(String url) throws Exception {
        return (List<Filme>) mvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("filmes");
    }

    private List<String> titulos(List<Filme> filmes) {
        return filmes.stream().map(Filme::getTitulo).toList();
    }

    @Test
    void osDozeGenerosExistem() {
        List<String> esperados = List.of("Ação", "Aventura", "Animação", "Comédia", "Drama", "Ficção Científica",
                "Terror", "Suspense", "Romance", "Fantasia", "Documentário", "Musical");
        for (String nome : esperados) {
            assertTrue(generos.existsByNome(nome), nome);
        }
    }

    @Test
    void listaMostraFilmesComSeusGeneros() throws Exception {
        salvar("Parasita", "16", "Suspense", "Drama");
        mvc.perform(get("/filmes"))
                .andExpect(status().isOk())
                .andExpect(view().name("filmes/lista"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Parasita")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Suspense")));
    }

    @Test
    void ordenaPorTituloDeAaZeDeZaA() throws Exception {
        salvar("Bravura", "L", "Drama");
        salvar("Ágata", "12", "Drama");
        salvar("Cidade", "16", "Drama");
        assertEquals(List.of("Ágata", "Bravura", "Cidade"), titulos(listar("/filmes?ordem=titulo")));
        assertEquals(List.of("Cidade", "Bravura", "Ágata"), titulos(listar("/filmes?ordem=titulo_desc")));
    }

    @Test
    void ordenaPorClassificacaoIndicativa() throws Exception {
        salvar("A", "18", "Drama");
        salvar("B", "L", "Drama");
        salvar("C", "12", "Drama");
        salvar("D", "10", "Drama");
        assertEquals(List.of("B", "D", "C", "A"), titulos(listar("/filmes?ordem=classificacao")));
    }

    @Test
    void ordenaPorGenero() throws Exception {
        salvar("Z1", "L", "Terror");
        salvar("Z2", "L", "Ação", "Terror");
        salvar("Z3", "L", "Comédia");
        assertEquals(List.of("Z2", "Z3", "Z1"), titulos(listar("/filmes?ordem=genero")));
    }

    @Test
    void filtraPorGeneroEClassificacao() throws Exception {
        salvar("Susto", "16", "Terror", "Suspense");
        salvar("Riso", "L", "Comédia");
        salvar("Medo Leve", "10", "Terror");
        Integer terror = genero("Terror").getId();

        assertEquals(List.of("Medo Leve", "Susto"), titulos(listar("/filmes?generoId=" + terror)));
        assertEquals(List.of("Susto"), titulos(listar("/filmes?generoId=" + terror + "&classificacao=16")));
        assertEquals(List.of("Riso"), titulos(listar("/filmes?classificacao=L")));
        assertEquals(List.of("Susto"), titulos(listar("/filmes?busca=sus")));
    }

    @Test
    void cadastraFilmeComVariosGeneros() throws Exception {
        String g1 = "" + genero("Ficção Científica").getId();
        String g2 = "" + genero("Aventura").getId();
        mvc.perform(post("/filmes")
                        .param("titulo", "Interestelar").param("duracaoMin", "169").param("classificacao", "10")
                        .param("generoIds", g1, g2))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/filmes"));
        Filme f = repository.findAll().get(0);
        assertEquals(2, f.getGeneros().size());
        assertEquals("Aventura, Ficção Científica", f.getGenerosTexto());
    }

    @Test
    void exigePeloMenosUmGenero() throws Exception {
        mvc.perform(post("/filmes")
                        .param("titulo", "Sem gênero").param("duracaoMin", "90").param("classificacao", "L"))
                .andExpect(status().isOk())
                .andExpect(view().name("filmes/form"))
                .andExpect(model().attributeHasFieldErrors("form", "generoIds"));
        assertEquals(0, repository.count());
    }

    @Test
    void rejeitaFilmeInvalido() throws Exception {
        mvc.perform(post("/filmes")
                        .param("titulo", "").param("duracaoMin", "0").param("classificacao", ""))
                .andExpect(view().name("filmes/form"))
                .andExpect(model().attributeHasFieldErrors("form", "titulo", "duracaoMin", "classificacao", "generoIds"));
    }

    @Test
    void editaFilmeTrocandoOsGeneros() throws Exception {
        Filme f = salvar("Antigo", "12", "Drama", "Romance");
        mvc.perform(post("/filmes/" + f.getId())
                        .param("titulo", "Novo").param("duracaoMin", "90").param("classificacao", "L")
                        .param("generoIds", "" + genero("Musical").getId()))
                .andExpect(redirectedUrl("/filmes"));
        Filme editado = repository.findById(f.getId()).orElseThrow();
        assertEquals("Novo", editado.getTitulo());
        assertEquals("Musical", editado.getGenerosTexto());
    }

    @Test
    void excluiFilme() throws Exception {
        Filme f = salvar("Apagar", "L", "Drama", "Comédia");
        mvc.perform(post("/filmes/" + f.getId() + "/excluir")).andExpect(redirectedUrl("/filmes"));
        assertTrue(repository.findById(f.getId()).isEmpty());
    }

    @Test
    void telasDeFormularioAbrem() throws Exception {
        Filme f = salvar("Qualquer", "L", "Drama");
        mvc.perform(get("/filmes/novo")).andExpect(status().isOk());
        mvc.perform(get("/filmes/" + f.getId() + "/editar")).andExpect(status().isOk());
    }
}
