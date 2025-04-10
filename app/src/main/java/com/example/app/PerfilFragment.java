package com.example.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private TextView nameText, apellidosText, phoneText, emailText;
    private DatabaseReference userRef;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        profileImage = view.findViewById(R.id.profileImage);
        nameText = view.findViewById(R.id.nombreText);
        apellidosText = view.findViewById(R.id.apellidosText);
        phoneText = view.findViewById(R.id.telefonoText);
        emailText = view.findViewById(R.id.emailText);

        auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        cargarDatosUsuario();

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

}
