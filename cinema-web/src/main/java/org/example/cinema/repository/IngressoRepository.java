package org.example.cinema.repository;

import org.example.cinema.model.Ingresso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngressoRepository extends JpaRepository<Ingresso, Integer> {

    long countBySessaoId(Integer sessaoId);

    boolean existsByAssentoId(Integer assentoId);

    Optional<Ingresso> findBySessaoIdAndAssentoId(Integer sessaoId, Integer assentoId);

    @Query("select i.assento.id from Ingresso i where i.sessao.id = :sessaoId")
    List<Integer> assentosVendidos(@Param("sessaoId") Integer sessaoId);

    @Query("select i.sessao.id, count(i), sum(i.valorPago) from Ingresso i group by i.sessao.id")
    List<Object[]> totaisPorSessao();
}
