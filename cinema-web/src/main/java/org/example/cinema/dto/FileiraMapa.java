package org.example.cinema.dto;

import java.util.List;

public class FileiraMapa {
    private final String letra;
    private final List<AssentoMapa> assentos;

    public FileiraMapa(String letra, List<AssentoMapa> assentos) {
        this.letra = letra;
        this.assentos = assentos;
    }

    public String getLetra() { return letra; }
    public List<AssentoMapa> getAssentos() { return assentos; }
}
