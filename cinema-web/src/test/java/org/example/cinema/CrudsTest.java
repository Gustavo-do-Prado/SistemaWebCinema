package org.example.cinema;

import org.example.cinema.dto.TotaisIngressos;
import org.example.cinema.model.*;
import org.example.cinema.repository.*;
import org.example.cinema.service.SalaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Testa Sala (com mapa e venda), Sessão e Ingresso (totais) de ponta a ponta: controller + Thymeleaf + H2. */
@SpringBootTest
@AutoConfigureMockMvc
class CrudsTest {

    @Autowired MockMvc mvc;
    @Autowired FilmeRepository filmes;
    @Autowired GeneroRepository generos;
    @Autowired SalaRepository salas;
    @Autowired AssentoRepository assentos;
    @Autowired SessaoRepository sessoes;
    @Autowired IngressoRepository ingressos;
    @Autowired SalaService salaService;

    @BeforeEach @AfterEach
    void limpar() {
        ingressos.deleteAll();
        sessoes.deleteAll();
        assentos.deleteAll();
        salas.deleteAll();
        filmes.deleteAll();
    }

    private Filme novoFilme(String titulo) {
        Filme f = new Filme();
        f.setTitulo(titulo);
        f.setDuracaoMin(132);
        f.setClassificacao("16");
        f.setGeneros(new LinkedHashSet<>(List.of(generos.findByNome("Drama").orElseThrow())));
        return filmes.save(f);
    }

    private Sessao novaSessao(Filme f, Sala s, LocalDateTime quando) {
        Sessao x = new Sessao();
        x.setFilme(f);
        x.setSala(s);
        x.setDataHora(quando);
        x.setPreco(new BigDecimal("32.00"));
        return sessoes.save(x);
    }

    private List<String> rotulos(Sala s) {
        return assentos.findBySalaId(s.getId(), Sort.by("fileira", "numero")).stream().map(Assento::getRotulo).toList();
    }

    private List<Integer> idsDosAssentos(Sala s) {
        return assentos.findBySalaId(s.getId(), Sort.by("fileira", "numero")).stream().map(Assento::getId).toList();
    }

    // ---------- SALA ----------

    @Test
    void criarSalaGeraOsAssentos() throws Exception {
        mvc.perform(post("/salas").param("nome", "Sala X").param("capacidade", "5").param("assentosPorFileira", "3"))
                .andExpect(redirectedUrl("/salas"));
        assertEquals(List.of("A1", "A2", "A3", "B1", "B2"), rotulos(salas.findAll().get(0)));
    }

    @Test
    void salaComNomeRepetidoEhRejeitada() throws Exception {
        salaService.criarComAssentos("Sala 1", 2, 2);
        mvc.perform(post("/salas").param("nome", "Sala 1").param("capacidade", "2").param("assentosPorFileira", "2"))
                .andExpect(view().name("salas/form"))
                .andExpect(model().attributeHasFieldErrors("form", "nome"));
        assertEquals(1, salas.count());
    }

    @Test
    void salaAcimaDoLimiteDeFileirasEhRejeitada() throws Exception {
        mvc.perform(post("/salas").param("nome", "Enorme").param("capacidade", "30").param("assentosPorFileira", "1"))
                .andExpect(view().name("salas/form"))
                .andExpect(model().attributeHasFieldErrors("form", "assentosPorFileira"));
    }

    @Test
    void aumentaOsAssentosSeguindoASequencia() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 4, 3); // A1 A2 A3 B1
        List<Integer> idsAntes = idsDosAssentos(s);
        mvc.perform(post("/salas/" + s.getId()).param("nome", "Sala 1").param("capacidade", "7").param("assentosPorFileira", "3"))
                .andExpect(redirectedUrl("/salas/" + s.getId() + "/editar"));
        assertEquals(List.of("A1", "A2", "A3", "B1", "B2", "B3", "C1"), rotulos(s));
        assertEquals(7, salas.findById(s.getId()).orElseThrow().getCapacidade());
        assertTrue(idsDosAssentos(s).containsAll(idsAntes)); // os que já existiam foram mantidos
    }

    @Test
    void diminuiOsAssentosRemovendoOsUltimos() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 7, 3);
        mvc.perform(post("/salas/" + s.getId()).param("nome", "Sala 1").param("capacidade", "4").param("assentosPorFileira", "3"))
                .andExpect(redirectedUrl("/salas/" + s.getId() + "/editar"));
        assertEquals(List.of("A1", "A2", "A3", "B1"), rotulos(s));
    }

    @Test
    void naoRemoveAssentoComIngresso() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 6, 3);
        Sessao sessao = novaSessao(novoFilme("Filme"), s, LocalDateTime.of(2026, 10, 10, 19, 0));
        Integer ultimo = idsDosAssentos(s).get(5); // B3
        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + sessao.getId()).param("assentoIds", "" + ultimo));

        mvc.perform(post("/salas/" + s.getId()).param("nome", "Sala 1").param("capacidade", "3").param("assentosPorFileira", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("salas/editar"))
                .andExpect(model().attributeHasFieldErrors("form", "capacidade"));
        assertEquals(6, assentos.countBySalaId(s.getId()));
    }

    @Test
    void renomearSalaComNomeRepetidoEhRejeitado() throws Exception {
        salaService.criarComAssentos("Sala 1", 2, 2);
        Sala s2 = salaService.criarComAssentos("Sala 2", 2, 2);
        mvc.perform(post("/salas/" + s2.getId()).param("nome", "Sala 1").param("capacidade", "2").param("assentosPorFileira", "2"))
                .andExpect(view().name("salas/editar"))
                .andExpect(model().attributeHasFieldErrors("form", "nome"));
    }

    @Test
    void excluirSalaApagaOsAssentos() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 4, 2);
        mvc.perform(post("/salas/" + s.getId() + "/excluir")).andExpect(flash().attributeExists("sucesso"));
        assertEquals(0, salas.count());
        assertEquals(0, assentos.count());
    }

    @Test
    void salaComSessaoNaoPodeSerExcluida() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 4, 2);
        novaSessao(novoFilme("Filme"), s, LocalDateTime.of(2026, 10, 10, 19, 0));
        mvc.perform(post("/salas/" + s.getId() + "/excluir")).andExpect(flash().attributeExists("erro"));
        assertEquals(1, salas.count());
    }

    // ---------- VENDA PELO MAPA ----------

    @Test
    void vendeEcancelaPeloMapaDaSala() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 4, 2);
        Sessao sessao = novaSessao(novoFilme("Filme"), s, LocalDateTime.of(2026, 10, 10, 19, 0));
        List<Integer> ids = idsDosAssentos(s);

        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + sessao.getId())
                        .param("assentoIds", "" + ids.get(0), "" + ids.get(1)))
                .andExpect(redirectedUrl("/salas/" + s.getId() + "/editar?sessaoId=" + sessao.getId()))
                .andExpect(flash().attributeExists("sucesso"));
        assertEquals(2, ingressos.count());
        assertEquals(0, new BigDecimal("32.00").compareTo(ingressos.findAll().get(0).getValorPago()));

        // vender de novo um assento já vendido: nada é vendido
        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + sessao.getId())
                        .param("assentoIds", "" + ids.get(1), "" + ids.get(2)))
                .andExpect(flash().attributeExists("erro"));
        assertEquals(2, ingressos.count());

        mvc.perform(post("/salas/" + s.getId() + "/cancelar").param("sessaoId", "" + sessao.getId())
                        .param("assentoIds", "" + ids.get(0)))
                .andExpect(flash().attributeExists("sucesso"));
        assertEquals(1, ingressos.count());
    }

    @Test
    void vendaSemAssentoOuSemSessaoEhRecusada() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 2, 2);
        Sessao sessao = novaSessao(novoFilme("Filme"), s, LocalDateTime.of(2026, 10, 10, 19, 0));
        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + sessao.getId()))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/salas/" + s.getId() + "/vender").param("assentoIds", "" + idsDosAssentos(s).get(0)))
                .andExpect(flash().attributeExists("erro"));
        assertEquals(0, ingressos.count());
    }

    @Test
    void naoVendeAssentoDeOutraSala() throws Exception {
        Sala s1 = salaService.criarComAssentos("Sala 1", 2, 2);
        Sala s2 = salaService.criarComAssentos("Sala 2", 2, 2);
        Sessao sessao = novaSessao(novoFilme("Filme"), s1, LocalDateTime.of(2026, 10, 10, 19, 0));
        mvc.perform(post("/salas/" + s1.getId() + "/vender").param("sessaoId", "" + sessao.getId())
                        .param("assentoIds", "" + idsDosAssentos(s2).get(0)))
                .andExpect(flash().attributeExists("erro"));
        assertEquals(0, ingressos.count());
    }

    // ---------- SESSÃO ----------

    @Test
    void crudDeSessaoEHorarioDuplicado() throws Exception {
        Filme f = novoFilme("Filme");
        Sala s = salaService.criarComAssentos("Sala 1", 2, 2);
        mvc.perform(post("/sessoes").param("filmeId", "" + f.getId()).param("salaId", "" + s.getId())
                        .param("dataHora", "2026-10-10T19:00").param("preco", "32.00"))
                .andExpect(redirectedUrl("/sessoes"));
        assertEquals(1, sessoes.count());

        mvc.perform(post("/sessoes").param("filmeId", "" + f.getId()).param("salaId", "" + s.getId())
                        .param("dataHora", "2026-10-10T19:00").param("preco", "20.00"))
                .andExpect(view().name("sessoes/form"))
                .andExpect(model().attributeHasFieldErrors("form", "dataHora"));

        Sessao sessao = sessoes.findAll().get(0);
        mvc.perform(post("/sessoes/" + sessao.getId()).param("filmeId", "" + f.getId()).param("salaId", "" + s.getId())
                        .param("dataHora", "2026-10-10T21:30").param("preco", "40.00"))
                .andExpect(redirectedUrl("/sessoes"));
        assertEquals(0, new BigDecimal("40.00").compareTo(sessoes.findById(sessao.getId()).orElseThrow().getPreco()));

        mvc.perform(post("/sessoes/" + sessao.getId() + "/excluir")).andExpect(redirectedUrl("/sessoes"));
        assertEquals(0, sessoes.count());
    }

    @Test
    void sessaoInvalidaEhRejeitada() throws Exception {
        mvc.perform(post("/sessoes").param("filmeId", "").param("salaId", "").param("dataHora", "").param("preco", "-1"))
                .andExpect(view().name("sessoes/form"))
                .andExpect(model().attributeHasFieldErrors("form", "filmeId", "salaId", "dataHora", "preco"));
    }

    @Test
    void filtraSessoesPorFilmeSalaEPeriodo() throws Exception {
        Filme a = novoFilme("Filme A");
        Filme b = novoFilme("Filme B");
        Sala s1 = salaService.criarComAssentos("Sala 1", 2, 2);
        Sala s2 = salaService.criarComAssentos("Sala 2", 2, 2);
        novaSessao(a, s1, LocalDateTime.of(2026, 10, 10, 16, 0));
        novaSessao(a, s2, LocalDateTime.of(2026, 10, 10, 19, 0));
        novaSessao(b, s1, LocalDateTime.of(2026, 10, 11, 21, 30));

        assertEquals(3, contarSessoes("/sessoes"));
        assertEquals(2, contarSessoes("/sessoes?filmeId=" + a.getId()));
        assertEquals(2, contarSessoes("/sessoes?salaId=" + s1.getId()));
        assertEquals(1, contarSessoes("/sessoes?filmeId=" + a.getId() + "&salaId=" + s1.getId()));
        assertEquals(2, contarSessoes("/sessoes?de=2026-10-10T18:00"));
        assertEquals(1, contarSessoes("/sessoes?de=2026-10-10T18:00&ate=2026-10-10T20:00"));
        assertEquals(0, contarSessoes("/sessoes?de=2027-01-01T00:00"));
    }

    @SuppressWarnings("unchecked")
    private int contarSessoes(String url) throws Exception {
        List<Sessao> lista = (List<Sessao>) mvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("sessoes");
        return lista.size();
    }

    @Test
    void filmeComSessaoNaoPodeSerExcluido() throws Exception {
        Filme f = novoFilme("Filme");
        novaSessao(f, salaService.criarComAssentos("Sala 1", 2, 2), LocalDateTime.of(2026, 10, 10, 19, 0));
        mvc.perform(post("/filmes/" + f.getId() + "/excluir")).andExpect(flash().attributeExists("erro"));
        assertEquals(1, filmes.count());
    }

    @Test
    void excluirSessaoApagaOsIngressos() throws Exception {
        Sala s = salaService.criarComAssentos("Sala 1", 2, 2);
        Sessao sessao = novaSessao(novoFilme("Filme"), s, LocalDateTime.of(2026, 10, 10, 19, 0));
        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + sessao.getId())
                .param("assentoIds", "" + idsDosAssentos(s).get(0)));
        assertEquals(1, ingressos.count());
        mvc.perform(post("/sessoes/" + sessao.getId() + "/excluir"));
        assertEquals(0, ingressos.count());
    }

    // ---------- TOTAIS DE INGRESSOS ----------

    @Test
    void totaisPorFilmeEPorSessao() throws Exception {
        Filme a = novoFilme("Filme A");
        Filme b = novoFilme("Filme B");
        Sala s = salaService.criarComAssentos("Sala 1", 4, 2);
        Sessao a1 = novaSessao(a, s, LocalDateTime.of(2026, 10, 10, 16, 0));
        Sessao a2 = novaSessao(a, s, LocalDateTime.of(2026, 10, 10, 19, 0));
        novaSessao(b, s, LocalDateTime.of(2026, 10, 10, 22, 0));
        List<Integer> ids = idsDosAssentos(s);

        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + a1.getId())
                .param("assentoIds", "" + ids.get(0), "" + ids.get(1), "" + ids.get(2)));
        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + a2.getId())
                .param("assentoIds", "" + ids.get(0)));

        TotaisIngressos geral = totais("/ingressos");
        assertEquals(4, geral.getVendidos());
        assertEquals(3, geral.getSessoes());
        assertEquals(0, new BigDecimal("128.00").compareTo(geral.getFaturamento()));

        TotaisIngressos doFilmeA = totais("/ingressos?filmeId=" + a.getId());
        assertEquals(4, doFilmeA.getVendidos());
        assertEquals(2, doFilmeA.getSessoes());
        assertEquals(1, doFilmeA.getPorFilme().size());

        TotaisIngressos daSessao = totais("/ingressos?sessaoId=" + a1.getId());
        assertEquals(3, daSessao.getVendidos());
        assertEquals(1, daSessao.getSessoes());
        assertEquals(75, daSessao.getOcupacao()); // 3 de 4 lugares

        TotaisIngressos doFilmeB = totais("/ingressos?filmeId=" + b.getId());
        assertEquals(0, doFilmeB.getVendidos());
    }

    private TotaisIngressos totais(String url) throws Exception {
        return (TotaisIngressos) mvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("totais");
    }

    // ---------- TODAS AS TELAS RENDERIZAM ----------

    @Test
    void todasAsTelasAbrem() throws Exception {
        Filme f = novoFilme("Filme");
        Sala s = salaService.criarComAssentos("Sala 1", 4, 2);
        Sessao sessao = novaSessao(f, s, LocalDateTime.of(2026, 10, 10, 19, 0));
        mvc.perform(post("/salas/" + s.getId() + "/vender").param("sessaoId", "" + sessao.getId())
                .param("assentoIds", "" + idsDosAssentos(s).get(0)));

        String[] urls = {
                "/filmes", "/filmes?ordem=genero&classificacao=16", "/filmes/novo", "/filmes/" + f.getId() + "/editar",
                "/salas", "/salas/novo", "/salas/" + s.getId() + "/editar",
                "/salas/" + s.getId() + "/editar?sessaoId=" + sessao.getId(),
                "/sessoes", "/sessoes?filmeId=" + f.getId() + "&salaId=" + s.getId() + "&de=2026-10-10T00:00&ate=2026-10-11T00:00",
                "/sessoes/novo", "/sessoes/" + sessao.getId() + "/editar",
                "/ingressos", "/ingressos?filmeId=" + f.getId(), "/ingressos?sessaoId=" + sessao.getId()
        };
        for (String url : urls) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }
}
