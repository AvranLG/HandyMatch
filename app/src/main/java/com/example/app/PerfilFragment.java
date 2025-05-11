package com.example.app;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.net.Uri;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PerfilFragment extends Fragment {

    private ImageView profileImage;
    private ImageView verifiedBadge;

    private TextView nameText, apellidosText, phoneText, emailText, direccionText, codigoPostalText, coloniaText, estadoText, ciudadText, referenciaText;
    private DatabaseReference userRef;
    private FirebaseAuth auth;

    private LinearLayout layoutValidarIdentidad;
    private TextView tvValidarIdentidad, tvAcercaDeNosotros;
    private Button btnCerrarSesion;
    private Button btnEliminarCuenta;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        profileImage = view.findViewById(R.id.profileImage);
        verifiedBadge = view.findViewById(R.id.verifiedBadge);
        nameText = view.findViewById(R.id.nombreText);
        apellidosText = view.findViewById(R.id.apellidosText);
        phoneText = view.findViewById(R.id.telefonoText);
        emailText = view.findViewById(R.id.emailText);
        btnEliminarCuenta = view.findViewById(R.id.btnEliminarCuenta);


        // Campos adicionales de dirección
        direccionText = view.findViewById(R.id.edit_calle_y_numero);
        codigoPostalText = view.findViewById(R.id.edit_postal);
        coloniaText = view.findViewById(R.id.edit_colonia);
        estadoText = view.findViewById(R.id.edit_estado);
        ciudadText = view.findViewById(R.id.edit_ciudad);
        referenciaText = view.findViewById(R.id.edit_referencia);

        // Inicializar nuevos elementos
        layoutValidarIdentidad = view.findViewById(R.id.layoutValidarIdentidad);
        tvValidarIdentidad = view.findViewById(R.id.tvValidarIdentidad);
        tvAcercaDeNosotros = view.findViewById(R.id.tvAcercaDeNosotros);
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);

        btnEliminarCuenta.setOnClickListener(v -> {
            // Primer diálogo de confirmación
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar cuenta")
                    .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer.")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Segundo diálogo de confirmación
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Confirmar eliminación")
                                .setMessage("Esta es tu última oportunidad. ¿Realmente deseas eliminar tu cuenta permanentemente?")
                                .setPositiveButton("Eliminar", (secondDialog, secondWhich) -> {
                                    eliminarCuenta(); // Método para eliminar cuenta
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });


// Click listener para Acerca de nosotros
        tvAcercaDeNosotros.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AcercaDeNosotrosActivity.class);
            startActivity(intent);
        });

// Click listener para Validar identidad
        tvValidarIdentidad.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ValidarIdentidadActivity.class);
            startActivity(intent);
        });

// Click listener para Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            // Mostrar diálogo de confirmación
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro que deseas cerrar sesión?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        // Redirigir al login
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

// Layout que contiene esos campos
        View layoutDireccionFields = view.findViewById(R.id.layoutDomicilioFields);
        layoutDireccionFields.setVisibility(View.GONE); // Oculto por defecto

        ImageButton btnEditarPerfil = view.findViewById(R.id.btn_editar);
        btnEditarPerfil.setOnClickListener(v -> abrirEditarPerfil());

        auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        cargarDatosUsuario();

        TextView tvAyuda = view.findViewById(R.id.tvAyuda);
        tvAyuda.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Activity_ayuda.class);
            startActivity(intent);
        });

        TextView tvTermino = view.findViewById(R.id.textTerminos);
        tvTermino.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), activity_terminos.class);
            startActivity(intent);
        });

// Flechita para expandir/contraer
        ImageView ivToggle = view.findViewById(R.id.ivToggle);
        LinearLayout layoutDomicilio = view.findViewById(R.id.layoutDomicilioFields);

        ivToggle.setOnClickListener(v -> {
            if (layoutDomicilio.getVisibility() == View.GONE) {
                layoutDomicilio.setAlpha(0f);
                layoutDomicilio.setTranslationY(-30); // Comienza un poco arriba
                layoutDomicilio.setVisibility(View.VISIBLE);

                layoutDomicilio.animate()
                        .alpha(1f)
                        .translationY(0)
                        .setDuration(300)
                        .start();

                ivToggle.animate().rotation(90f).setDuration(200).start(); // Flecha hacia abajo
            } else {
                layoutDomicilio.animate()
                        .alpha(0f)
                        .translationY(-30)
                        .setDuration(200)
                        .withEndAction(() -> layoutDomicilio.setVisibility(View.GONE))
                        .start();

                ivToggle.animate().rotation(0f).setDuration(200).start(); // Flecha a la derecha
            }
        });


        return view;
    }

    private void cargarDatosUsuario() {
        // Verificar si el usuario está autenticado
        if (auth.getCurrentUser() == null) {
            Log.e("PerfilFragment", "No hay usuario autenticado");
            return;
        }

        // Registrar los datos del usuario actual
        String uid = auth.getCurrentUser().getUid();
        String providerId = auth.getCurrentUser().getProviderData().get(1).getProviderId();

        Log.d("PerfilFragment", "UID del usuario: " + uid);
        Log.d("PerfilFragment", "Proveedor actual: " + providerId);
        Log.d("PerfilFragment", "Correo del usuario: " + auth.getCurrentUser().getEmail());

        // Verificar qué cuenta está activa
        if (providerId.equals("google.com")) {
            Log.d("PerfilFragment", "Cuenta activa: Google");
        } else if (providerId.equals("password")) {
            Log.d("PerfilFragment", "Cuenta activa: Correo/Contraseña");
        } else {
            Log.d("PerfilFragment", "Cuenta activa: Otro proveedor");
        }

        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("PerfilFragment", "DataSnapshot recibido: " + snapshot.exists());

                if (snapshot.exists()) {
                    // Registrar todas las claves en el snapshot para depuración
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Log.d("PerfilFragment", "Clave en Firebase: " + child.getKey() + ", Valor: " + child.getValue());
                    }


                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String apellidos = snapshot.child("apellidos").getValue(String.class);
                    String telefono = snapshot.child("telefono").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String imagenUrl = snapshot.child("imagenUrl").getValue(String.class);
                    String direccion = snapshot.child("direccion").getValue(String.class);
                    String codigoPostal = snapshot.child("codigo_postal").getValue(String.class);
                    String colonia = snapshot.child("colonia").getValue(String.class);
                    String estado = snapshot.child("estado").getValue(String.class);
                    String ciudad = snapshot.child("ciudad").getValue(String.class);
                    String referencia = snapshot.child("referencia").getValue(String.class);

                    // Gestionar la visibilidad de la insignia

                    Boolean verificado = snapshot.child("verificado").getValue(Boolean.class);
                    boolean mostrarInsignia = verificado != null && verificado;
                    Log.d("DEBUG_VERIFICADO", "Valor: " + verificado);  // Debe mostrar "true"

                    if (verifiedBadge != null) {
                        verifiedBadge.setVisibility(mostrarInsignia ? View.VISIBLE : View.GONE);
                        Log.d("PerfilFragment", "Insignia " + (mostrarInsignia ? "visible" : "oculta"));
                    }
                    // Gestionar la visibilidad de la opción "Validar identidad"
                    boolean estaVerificado = verificado != null && verificado;
                    layoutValidarIdentidad.setVisibility(estaVerificado ? View.GONE : View.VISIBLE);

                    Log.d("PerfilFragment", "Datos obtenidos - Nombre: " + nombre +
                            ", Apellidos: " + apellidos +
                            ", Email: " + email +
                            ", ImagenUrl: " + imagenUrl);

                    // Obtener datos de Google si están disponibles
                    String googleName = auth.getCurrentUser().getDisplayName();
                    Uri googlePhotoUrl = auth.getCurrentUser().getPhotoUrl();

                    Log.d("PerfilFragment", "Datos de Google - Nombre: " + googleName +
                            ", Foto URL: " + (googlePhotoUrl != null ? googlePhotoUrl.toString() : "null"));

                    // Procesar el nombre de Google
                    String nombreGoogle = "";
                    String apellidosGoogle = "";

                    if (googleName != null && !googleName.isEmpty()) {
                        String[] nameParts = googleName.split(" ");
                        nombreGoogle = nameParts.length > 0 ? nameParts[0] : "";
                        apellidosGoogle = nameParts.length > 1 ? googleName.substring(nameParts[0].length()).trim() : "";
                    }

                    // Si el nombre o los apellidos de Firebase están vacíos, se toman los de Google
                    if (nombre == null || nombre.isEmpty()) {
                        nombre = nombreGoogle;
                        Log.d("PerfilFragment", "Usando nombre de Google: " + nombre);
                    }
                    if (apellidos == null || apellidos.isEmpty()) {
                        apellidos = apellidosGoogle;
                        Log.d("PerfilFragment", "Usando apellidos de Google: " + apellidos);
                    }

                    // Establecer los valores en los campos de texto
                    nameText.setText(nombre != null && !nombre.isEmpty() ? nombre : "Nombre no disponible");
                    apellidosText.setText(apellidos != null && !apellidos.isEmpty() ? apellidos : "Apellidos no disponibles");
                    phoneText.setText(telefono != null && !telefono.isEmpty() ? telefono : "Teléfono no disponible");
                    direccionText.setText(direccion != null && !direccion.isEmpty() ? direccion : "Dirección no disponible");
                    codigoPostalText.setText(codigoPostal != null && !codigoPostal.isEmpty() ? codigoPostal : "CP no disponible");
                    coloniaText.setText(colonia != null && !colonia.isEmpty() ? colonia : "Colonia no disponible");
                    estadoText.setText(estado != null && !estado.isEmpty() ? estado : "Estado no disponible");
                    ciudadText.setText(ciudad != null && !ciudad.isEmpty() ? ciudad : "Ciudad no disponible");
                    referenciaText.setText(referencia != null && !referencia.isEmpty() ? referencia : "Referencia no disponible");
                    emailText.setText(email != null && !email.isEmpty() ? email : auth.getCurrentUser().getEmail());

                    // Eliminar cualquier tinte previo del ImageView
                    profileImage.setColorFilter(null);

                    // Determinar qué URL de imagen cargar
                    String imageUrlToLoad = null;
                    String imageSource = "default";

                    if (imagenUrl != null && !imagenUrl.isEmpty()) {
                        // Usar imagen guardada en Firebase (URL de Supabase)
                        imageUrlToLoad = imagenUrl;
                        imageSource = "firebase";
                        Log.d("PerfilFragment", "Usando imagen de Firebase/Supabase: " + imageUrlToLoad);
                    } else if (googlePhotoUrl != null) {
                        // Usar imagen de Google
                        imageUrlToLoad = googlePhotoUrl.toString();
                        imageSource = "google";
                        Log.d("PerfilFragment", "Usando imagen de Google: " + imageUrlToLoad);
                    } else {
                        Log.d("PerfilFragment", "No se encontró ninguna imagen, usando predeterminada");
                    }

                    // Cargar la imagen seleccionada
                    final String finalImageSource = imageSource;
                    if (imageUrlToLoad != null) {
                        Glide.with(requireContext())
                                .load(imageUrlToLoad)
                                .placeholder(R.drawable.usuario)
                                .error(R.drawable.usuario)
                                .transform(new CircleCrop())
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        Log.e("PerfilFragment", "Error al cargar imagen de " + finalImageSource + ": " + e);
                                        if (e != null) {
                                            for (Throwable t : e.getRootCauses()) {
                                                Log.e("PerfilFragment", "Causa: " + t.getMessage());
                                            }
                                        }
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        Log.d("PerfilFragment", "Imagen de " + finalImageSource + " cargada correctamente desde: " + dataSource.name());
                                        return false;
                                    }
                                })
                                .into(profileImage);
                    } else {
                        // Si no hay ninguna URL disponible, usar la imagen predeterminada
                        profileImage.setImageResource(R.drawable.usuario);
                        profileImage.setColorFilter(null); // Asegurar que no haya tinte
                    }
                } else {
                    Log.e("PerfilFragment", "No se encontraron datos del usuario en Firebase");
                    // Mostrar la información básica de la cuenta
                    profileImage.setImageResource(R.drawable.usuario);
                    profileImage.setColorFilter(null);

                    String email = auth.getCurrentUser().getEmail();
                    emailText.setText(email != null ? email : "Correo no disponible");
                    nameText.setText("Nombre no disponible");
                    apellidosText.setText("Apellidos no disponibles");
                    phoneText.setText("Teléfono no disponible");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PerfilFragment", "Error al cargar datos: " + error.getMessage());
            }
        });
    }

    private void eliminarCuenta() {
        String uid = auth.getCurrentUser().getUid();

        // 1. Eliminar datos del Realtime Database
        userRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("PerfilFragment", "Datos del usuario eliminados de la base de datos");

                // 2. Eliminar cuenta de autenticación
                auth.getCurrentUser().delete().addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        Log.d("PerfilFragment", "Cuenta de usuario eliminada de Firebase Auth");

                        // Redirigir al login
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    } else {
                        Log.e("PerfilFragment", "Error al eliminar la cuenta: " + deleteTask.getException());
                        mostrarError("Ocurrió un error al eliminar la cuenta. Intenta nuevamente.");
                    }
                });
            } else {
                Log.e("PerfilFragment", "Error al eliminar datos del usuario: " + task.getException());
                mostrarError("No se pudieron eliminar los datos del usuario.");
            }
        });
    }

    private void mostrarError(String mensaje) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }


    private void abrirEditarPerfil() {
        Intent intent = new Intent(getActivity(), EditarPerfilActivity.class);
        startActivity(intent);
    }
}