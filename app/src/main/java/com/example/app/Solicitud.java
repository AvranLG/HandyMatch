package com.example.app;

public class Solicitud {
    private String idSolicitud;
    private String idPublicacion;
    private String idUsuarioPostulante;
    private long timestamp;

    public Solicitud() {
        // Constructor vacío obligatorio para Firebase
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(String idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public String getIdPublicacion() {
        return idPublicacion;
    }

    public void setIdPublicacion(String idPublicacion) {
        this.idPublicacion = idPublicacion;
    }

    public String getIdUsuarioPostulante() {
        return idUsuarioPostulante;
    }

    public void setIdUsuarioPostulante(String idUsuarioPostulante) {
        this.idUsuarioPostulante = idUsuarioPostulante;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Solicitud{" +
                "idUsuarioPostulante='" + idUsuarioPostulante + '\'' +
                ", idPublicacion='" + idPublicacion + '\'' +
                // agrega más campos si quieres
                '}';
    }

}

