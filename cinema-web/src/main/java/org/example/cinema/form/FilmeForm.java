package org.example.cinema.form;

import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

public class FilmeForm {

    private Integer id;

    @NotBlank(message = "Informe o título do filme.")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres.")
    private String titulo;

    @NotNull(message = "Informe a duração em minutos.")
    @Min(value = 1, message = "A duração deve ser maior que zero.")
    private Integer duracaoMin;

    @NotBlank(message = "Selecione a classificação indicativa.")
    @Size(max = 10, message = "A classificação deve ter no máximo 10 caracteres.")
    private String classificacao;

    @NotEmpty(message = "Selecione pelo menos um gênero.")
    private List<Integer> generoIds = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Integer getDuracaoMin() { return duracaoMin; }
    public void setDuracaoMin(Integer duracaoMin) { this.duracaoMin = duracaoMin; }
    public String getClassificacao() { return classificacao; }
    public void setClassificacao(String classificacao) { this.classificacao = classificacao; }
    public List<Integer> getGeneroIds() { return generoIds; }
    public void setGeneroIds(List<Integer> generoIds) { this.generoIds = generoIds; }
}
