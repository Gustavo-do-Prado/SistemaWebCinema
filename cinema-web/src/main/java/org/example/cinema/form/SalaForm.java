package org.example.cinema.form;

import jakarta.validation.constraints.*;

public class SalaForm {

    @NotBlank(message = "Informe o nome da sala.")
    @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres.")
    private String nome;

    @NotNull(message = "Informe a capacidade.")
    @Min(value = 1, message = "A capacidade deve ser maior que zero.")
    @Max(value = 1000, message = "A capacidade máxima é 1000.")
    private Integer capacidade;

    @NotNull(message = "Informe quantos assentos há por fileira.")
    @Min(value = 1, message = "Deve ser maior que zero.")
    @Max(value = 100, message = "O máximo é 100 por fileira.")
    private Integer assentosPorFileira;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }
    public Integer getAssentosPorFileira() { return assentosPorFileira; }
    public void setAssentosPorFileira(Integer assentosPorFileira) { this.assentosPorFileira = assentosPorFileira; }
}
