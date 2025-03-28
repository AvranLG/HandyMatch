package com.example.app;
public class Usuario {
    private String uid;
    private String nombre;
    private String apellidos;
    private String email;
    private String telefono;
    private String contrasena;
    private String fecha_registro;

    // Campos de la dirección
    private String direccion;
    private String codigo_postal;
    private String colonia;
    private String estado;
    private String ciudad;
    private String referencia;
    private String imagenUrl;

    // Constructor vacío para Firebase
    public Usuario() {}

    // Constructor con parámetros (agregando los campos de la dirección)
    public Usuario(String nombre, String apellidos, String email, String telefono, String contrasena,
                   String fecha_registro,
                   String direccion, String codigo_postal, String colonia,
                   String estado, String ciudad, String referencia, String imagenUrl) {

        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.contrasena = contrasena;
        this.fecha_registro = fecha_registro;
        this.direccion = direccion;
        this.codigo_postal = codigo_postal;
        this.colonia = colonia;
        this.estado = estado;
        this.ciudad = ciudad;
        this.referencia = referencia;
        this.imagenUrl = imagenUrl;
    }

    // Getters y Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getNombre() {
        return nombre;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
    public String getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(String fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    // Métodos para los campos de dirección
    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCodigo_postal() {
        return codigo_postal;
    }

    public void setCodigo_postal(String codigo_postal) {
        this.codigo_postal = codigo_postal;
    }

    public String getColonia() {
        return colonia;
    }

    public void setColonia(String colonia) {
        this.colonia = colonia;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {this.referencia = referencia;}

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

}
