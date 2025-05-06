package com.example.app;

public class Mensaje {
    private String mensaje;
    private String uidEmisor;
    private long timestamp;

    // Constructor
    public Mensaje(String mensaje, String uidEmisor, long timestamp) {
        this.mensaje = mensaje;
        this.uidEmisor = uidEmisor;
        this.timestamp = timestamp;
    }

    public Mensaje() {
        // Constructor vacío
    }

    // Getters y setters
    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getUidEmisor() {
        return uidEmisor;
    }

    public void setUidEmisor(String uidEmisor) {
        this.uidEmisor = uidEmisor;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
