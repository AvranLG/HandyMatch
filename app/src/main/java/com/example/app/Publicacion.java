package com.example.app;
public class Publicacion {
    private String titulo;
    private String tituloLower;
    private String categoria;
    private String descripcion;
    private String fechaHora;
    private String pago;
    private String ubicacion;
    private Double latitud;
    private Double longitud;
    private String idUsuario;
    private String estadoPublicacion;
    private String idPublicacion;

    // Constructor vacío (requerido por Firebase)
    public Publicacion() {}

    // Constructor con todos los campos
    public Publicacion(String titulo, String tituloLower, String categoria, String descripcion, String fechaHora,
                       String pago, String ubicacion, Double latitud, Double longitud, String idUsuario, String estadoPublicacion) {
        this.titulo = titulo;
        this.tituloLower = tituloLower;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.pago = pago;
        this.ubicacion = ubicacion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.idUsuario = idUsuario;
        this.estadoPublicacion = estadoPublicacion;
    }

    // Getters y Setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getPago() {
        return pago;
    }

    public void setPago(String pago) {
        this.pago = pago;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public String getTituloLower() {
        return tituloLower;
    }

    public void setTituloLower(String tituloLower) {
        this.tituloLower = tituloLower;
    }

    public String getIdPublicacion() {
        return idPublicacion;
    }

    public void setIdPublicacion(String idPublicacion) {
        this.idPublicacion = idPublicacion;
    }


    public void setEstadoPublicacion(String estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
    }
}
