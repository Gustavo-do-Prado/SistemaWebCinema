package org.example.cinema.dto;

public class AssentoMapa {
    private final Integer id;
    private final String rotulo;
    private final boolean vendido;

    public AssentoMapa(Integer id, String rotulo, boolean vendido) {
        this.id = id;
        this.rotulo = rotulo;
        this.vendido = vendido;
    }

    public Integer getId() { return id; }
    public String getRotulo() { return rotulo; }
    public boolean isVendido() { return vendido; }
}
