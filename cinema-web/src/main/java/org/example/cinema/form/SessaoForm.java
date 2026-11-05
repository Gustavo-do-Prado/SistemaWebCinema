package org.example.cinema.form;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SessaoForm {

    private Integer id;

    @NotNull(message = "Selecione o filme.")
    private Integer filmeId;

    @NotNull(message = "Selecione a sala.")
    private Integer salaId;

    @NotNull(message = "Informe a data e a hora.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataHora;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
    @Digits(integer = 6, fraction = 2, message = "Use no máximo 2 casas decimais.")
    private BigDecimal preco;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFilmeId() { return filmeId; }
    public void setFilmeId(Integer filmeId) { this.filmeId = filmeId; }
    public Integer getSalaId() { return salaId; }
    public void setSalaId(Integer salaId) { this.salaId = salaId; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
}
