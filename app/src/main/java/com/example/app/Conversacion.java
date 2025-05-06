package com.example.app;

import java.io.Serializable;

public class Conversacion implements Serializable {
    private String uidOtro;
    private String nombre;
    private String fotoUrl;
    private String ultimoMensaje;
    private long timestamp;

    public Conversacion() {
        // Requerido para Firebase
    }

    public Conversacion(String uidOtro, String nombre, String fotoUrl, String ultimoMensaje, long timestamp) {
        this.uidOtro = uidOtro;
        this.nombre = nombre;
        this.fotoUrl = fotoUrl;
        this.ultimoMensaje = ultimoMensaje;
        this.timestamp = timestamp;
    }

    public String getUidOtro() {
        return uidOtro;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
