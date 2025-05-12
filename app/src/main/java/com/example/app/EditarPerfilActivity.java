package com.example.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditarPerfilActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final String PREF_NAME = "ProfilePrefs";
    private static final String KEY_IMAGE_PATH = "current_image_path";
    private static final String KEY_IMAGE_MODIFIED = "image_modified";
    private static final String USER_ID_KEY = "user_id";

    private ImageView profileImage, logoImagen;

    private DatabaseReference databaseRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private String currentImagePath = null;
    private boolean imagenModificada = false;

    private TextInputEditText nombreText, apellidosText, numeroText, direccionText, postalText, coloniaText, descripcionText;
    private EditText ciudadText;
    private EditText estadoText;
    private static final String GEOCODING_API_KEY = BuildConfig.GEOCODING_API_KEY;

    // Variable para almacenar la URL de la imagen
    private String fotoUrl = "";
    private SharedPreferences preferences;

    GenerativeModel gm = new GenerativeModel(/* modelName */ "gemini-1.5-flash", Secrets.GEMINI_API_KEY);
    GenerativeModelFutures model = GenerativeModelFutures.from(gm);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        // Inicializar SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Inicializar vistas primero
        profileImage = findViewById(R.id.profileImage);
        logoImagen = findViewById(R.id.logoImagen);
        nombreText = findViewById(R.id.nombreText);
        apellidosText = findViewById(R.id.apellidosText);
        numeroText = findViewById(R.id.numeroText);
        direccionText = findViewById(R.id.direccionText);
        postalText = findViewById(R.id.postalText);
        coloniaText = findViewById(R.id.coloniaText);
        ciudadText = findViewById(R.id.ciudadText);
        estadoText = findViewById(R.id.estadoText);
        descripcionText = findViewById(R.id.descripcionText);
        EditText passwordText = findViewById(R.id.passwordText);
        EditText newPasswordText = findViewById(R.id.newPasswordText);
        TextView contra = findViewById(R.id.contra);
        ImageView ivToggle = findViewById(R.id.flecha_desplegarPWD);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            // Limpiar datos guardados de otro usuario si el ID cambió
            String savedUserId = preferences.getString(USER_ID_KEY, "");
            if (!savedUserId.equals(currentUser.getUid())) {
                // Si es un usuario diferente, limpiar las preferencias
                clearImagePreferences();
            } else {
                // Cargar datos guardados solo si es el mismo usuario
                currentImagePath = preferences.getString(KEY_IMAGE_PATH, null);
                imagenModificada = preferences.getBoolean(KEY_IMAGE_MODIFIED, false);
            }

            // Guardar el ID del usuario actual
            preferences.edit().putString(USER_ID_KEY, currentUser.getUid()).apply();
        }

        // Recuperar estado de imagen si existe
        if (currentImagePath != null && imagenModificada) {
            File file = new File(currentImagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath);
                if (bitmap != null) {
                    // Usar Glide para mostrar la imagen como circular
                    Glide.with(this)
                            .load(bitmap)
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .into(profileImage);
                }
            } else {
                // Si el archivo ya no existe, limpiar la referencia
                clearImagePreferences();
            }
        }

        // Escucha para abrir galería
        logoImagen.setOnClickListener(v -> abrirGaleria());

        passwordText.setEnabled(false);
        newPasswordText.setEnabled(false);

        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            DatabaseReference usuariosRef = firebaseDatabase.getReference("usuarios");

            usuariosRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String tipoLogin = snapshot.child("tipoLogin").getValue(String.class);

                    // Si es login por correo/contraseña
                    if (tipoLogin == null || tipoLogin.equals("")) {
                        ivToggle.setVisibility(View.VISIBLE);
                        contra.setVisibility(View.VISIBLE);

                        passwordText.setEnabled(true); // permitir escribir contraseña actual

                        // Detectar cuando el usuario termina de escribir la contraseña
                        passwordText.setOnFocusChangeListener((v, hasFocus) -> {
                            if (!hasFocus) { // cuando pierde el foco
                                String passwordIngresada = passwordText.getText().toString().trim();
                                String email = currentUser.getEmail();

                                if (!passwordIngresada.isEmpty()) {
                                    AuthCredential credential = EmailAuthProvider.getCredential(email, passwordIngresada);

                                    currentUser.reauthenticate(credential)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    // Contraseña correcta
                                                    newPasswordText.setEnabled(true);
                                                } else {
                                                    // Contraseña incorrecta
                                                    newPasswordText.setEnabled(false);
                                                    newPasswordText.setText("");
                                                }
                                            });
                                }
                            }
                        });
                    } else {
                        // Si es login con Google, deshabilita ambos campos
                        passwordText.setEnabled(false);
                        newPasswordText.setEnabled(false);
                        ivToggle.setVisibility(View.GONE);
                        contra.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), "Error al obtener los datos del usuario", Toast.LENGTH_SHORT).show();
                }
            });

            // Cargamos los datos del usuario
            databaseRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);
            databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String nombre = snapshot.child("nombre").getValue(String.class);
                        String apellidos = snapshot.child("apellidos").getValue(String.class);
                        String telefono = snapshot.child("telefono").getValue(String.class);
                        String direccion = snapshot.child("direccion").getValue(String.class);
                        String codigoPostal = snapshot.child("codigo_postal").getValue(String.class);
                        String colonia = snapshot.child("colonia").getValue(String.class);
                        String ciudad = snapshot.child("ciudad").getValue(String.class);
                        String estado = snapshot.child("estado").getValue(String.class);
                        String descripcion = snapshot.child("referencia").getValue(String.class);

                        nombreText.setText(nombre);
                        apellidosText.setText(apellidos);
                        numeroText.setText(telefono);
                        direccionText.setText(direccion);
                        postalText.setText(codigoPostal);
                        coloniaText.setText(colonia);
                        ciudadText.setText(ciudad);
                        estadoText.setText(estado);
                        descripcionText.setText(descripcion);

                        // Cargamos la URL de la imagen solo si no se ha modificado localmente
                        if (!imagenModificada) {
                            fotoUrl = snapshot.child("imagenUrl").getValue(String.class);

                            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                Glide.with(EditarPerfilActivity.this)
                                        .load(fotoUrl)
                                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                        .placeholder(R.drawable.circulo_imagen)
                                        .error(R.drawable.circulo_imagen)
                                        .into(profileImage);
                            }
                        }
                    } else {
                        Toast.makeText(EditarPerfilActivity.this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(EditarPerfilActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Configurar el listener para el código postal
        postalText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String codigoPostal = postalText.getText().toString().trim();
                if (codigoPostal.length() == 5) {
                    obtenerCiudadPorCodigoPostal(codigoPostal);
                }
            }
        });

        // Configurar el toggle para la sección de contraseña
        LinearLayout layoutPasswords = findViewById(R.id.layoutPasswordFields);
        layoutPasswords.setVisibility(View.GONE);
        ivToggle.setRotation(0f); // flecha a la derecha

        ivToggle.setOnClickListener(v -> {
            if (layoutPasswords.getVisibility() == View.GONE) {
                layoutPasswords.setVisibility(View.VISIBLE);
                ivToggle.animate().rotation(90f).setDuration(200).start(); // Rota a abajo
            } else {
                layoutPasswords.setVisibility(View.GONE);
                ivToggle.animate().rotation(0f).setDuration(200).start(); // Rota a derecha
            }
        });
    }

    private void clearImagePreferences() {
        // Limpiar los valores de imagen guardados
        currentImagePath = null;
        imagenModificada = false;
        preferences.edit()
                .remove(KEY_IMAGE_PATH)
                .remove(KEY_IMAGE_MODIFIED)
                .apply();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentImagePath != null) {
            outState.putString(KEY_IMAGE_PATH, currentImagePath);
            outState.putBoolean(KEY_IMAGE_MODIFIED, imagenModificada);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Si el usuario selecciona una imagen
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri sourceUri = data.getData();

            // Generar un nombre de archivo único basado en la hora actual
            String fileName = "croppedImage_" + System.currentTimeMillis() + ".jpg";

            // Uri temporal donde se guardará la imagen recortada
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), fileName));

            UCrop.Options options = new UCrop.Options();
            options.setShowCropFrame(false); // Elimina el borde cuadrado
            options.setCircleDimmedLayer(true); // Muestra el recorte circular
            options.setHideBottomControls(true); // Oculta botones extra
            options.setCompressionQuality(90);  // Compresión de imagen

            // Configuración de UCrop para recortar la imagen
            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)  // Establece el aspecto 1:1 (cuadrado)
                    .withMaxResultSize(600, 600)  // Tamaño máximo de la imagen recortada
                    .withOptions(options)  // Aplica las opciones
                    .start(this);  // Inicia el recorte
        }
        // Si el usuario terminó de recortar la imagen
        //_____________________________________________________
        //_____________________________________________________
        //_____________________________________________________
        //_____________________________________________________
        //_____________________________________________________


        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                // Si había una imagen previa, intentar borrarla
                if (currentImagePath != null) {
                    File oldFile = new File(currentImagePath);
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }

                // Guardamos la nueva ruta
                currentImagePath = resultUri.getPath();

                // Guardar en SharedPreferences
                preferences.edit()
                        .putString(KEY_IMAGE_PATH, currentImagePath)
                        .putBoolean(KEY_IMAGE_MODIFIED, true)
                        .apply();

                imagenModificada = true;

                // Mostrar la imagen como circular usando Glide
                Glide.with(this)
                        .load(new File(currentImagePath))
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(profileImage);

                Toast.makeText(this, "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Error al recortar: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Solo mostrar imagen de Firebase si NO hay imagen local modificada
        if (!imagenModificada && fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(fotoUrl)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.circulo_imagen)
                    .error(R.drawable.circulo_imagen)
                    .into(profileImage);
        }
        // Si hay imagen local modificada, mostrar esa
        else if (imagenModificada && currentImagePath != null) {
            File file = new File(currentImagePath);
            if (file.exists()) {
                Glide.with(this)
                        .load(file)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(profileImage);
            } else {
                // Si el archivo ya no existe, reiniciar el estado
                clearImagePreferences();

                // Intentar mostrar la imagen de Firebase si está disponible
                if (fotoUrl != null && !fotoUrl.isEmpty()) {
                    Glide.with(this)
                            .load(fotoUrl)
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .placeholder(R.drawable.circulo_imagen)
                            .error(R.drawable.circulo_imagen)
                            .into(profileImage);
                }
            }
        }
    }

    private void obtenerCiudadPorCodigoPostal(String codigoPostal) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + codigoPostal + ",MX&key=" + GEOCODING_API_KEY;

        Log.d("GeocodingRequest", "URL de la API: " + url);  // Log para verificar que la URL es correcta

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GeocodingError", "Error en la solicitud: " + e.getMessage());  // Log para capturar errores de la solicitud
                runOnUiThread(() -> Toast.makeText(EditarPerfilActivity.this, "Error al obtener ciudad", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("GeocodingResponse", "Respuesta de la API: " + responseData);  // Log para ver la respuesta completa de la API
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray results = jsonObject.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONArray addressComponents = results.getJSONObject(0).getJSONArray("address_components");
                            String ciudad = "", estado = "";
                            for (int i = 0; i < addressComponents.length(); i++) {
                                JSONObject component = addressComponents.getJSONObject(i);
                                JSONArray types = component.getJSONArray("types");

                                for (int j = 0; j < types.length(); j++) {
                                    String type = types.getString(j);
                                    if (type.equals("locality")) {
                                        ciudad = component.getString("long_name");
                                    } else if (type.equals("administrative_area_level_1")) {
                                        estado = component.getString("long_name");
                                    }
                                }
                            }

                            String finalCiudad = ciudad;
                            String finalEstado = estado;
                            runOnUiThread(() -> {
                                ciudadText.setText(finalCiudad);
                                estadoText.setText(finalEstado);
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("GeocodingError", "Error al procesar JSON: " + e.getMessage());  // Log para errores en el procesamiento del JSON
                        runOnUiThread(() -> Toast.makeText(EditarPerfilActivity.this, "Error al procesar JSON", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("GeocodingError", "Respuesta no exitosa: " + response.code());  // Log si la respuesta no es exitosa
                }
            }
        });
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // Metodo para guardar los cambios, incluyendo la imagen
    public void guardarCambios(View view) {
        if (currentUser == null) {
            Toast.makeText(this, "Error: No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Validar que los campos requeridos no estén vacíos
        String nombre = nombreText.getText().toString().trim();
        String apellidos = apellidosText.getText().toString().trim();
        String telefono = numeroText.getText().toString().trim();
        String direccion = direccionText.getText().toString().trim();
        String codigoPostal = postalText.getText().toString().trim();
        String colonia = coloniaText.getText().toString().trim();
        String ciudad = ciudadText.getText().toString().trim();
        String estado = estadoText.getText().toString().trim();
        String descripcion = descripcionText.getText().toString().trim();

        // Validar campos obligatorios
        if (nombre.isEmpty() || apellidos.isEmpty() || telefono.isEmpty() ||
                direccion.isEmpty() || codigoPostal.isEmpty() || colonia.isEmpty() ||
                ciudad.isEmpty() || estado.isEmpty()) {

            Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar diálogo de progreso
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Guardando cambios");
        progressDialog.setMessage("Por favor espera mientras actualizamos tu información...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Obtener referencia a la base de datos para el usuario actual
        String uid = currentUser.getUid();
        final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        // 2. Verificar si el usuario puede y quiere cambiar su contraseña
        EditText newPasswordText = findViewById(R.id.newPasswordText);
        EditText passwordText = findViewById(R.id.passwordText);
        final String newPassword = newPasswordText.getText().toString().trim();

        // Obtener tipo de login para verificar si puede cambiar contraseña
        userRef.child("tipoLogin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final String tipoLogin = snapshot.exists() ? snapshot.getValue(String.class) : "";
                boolean isGoogleLogin = tipoLogin != null && !tipoLogin.isEmpty();

                // Si no es login de Google y el usuario ingresó nueva contraseña
                if (!isGoogleLogin && !newPassword.isEmpty()) {
                    // Validar que la nueva contraseña cumpla requisitos
                    if (!isValidPassword(newPassword)) {
                        progressDialog.dismiss();
                        Toast.makeText(EditarPerfilActivity.this,
                                "La contraseña debe tener al menos 8 caracteres, una letra mayúscula, una minúscula y un número",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Actualizar contraseña en Authentication
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> {
                                // Actualizar también en la base de datos (si almacenas contraseña ahí)
                                userRef.child("contrasena").setValue(newPassword);
                                // Continuar con el resto de la actualización
                                subirImagenYDatos(progressDialog, userRef, nombre, apellidos, telefono, direccion,
                                        codigoPostal, colonia, ciudad, estado, descripcion);
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(EditarPerfilActivity.this,
                                        "Error al actualizar contraseña: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // Si no hay cambio de contraseña, continuar con el resto de actualizaciones
                    subirImagenYDatos(progressDialog, userRef, nombre, apellidos, telefono, direccion,
                            codigoPostal, colonia, ciudad, estado, descripcion);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(EditarPerfilActivity.this,
                        "Error al verificar tipo de cuenta: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidPassword(String password) {
        // Al menos 8 caracteres
        if (password.length() < 8) return false;

        // Al menos una letra mayúscula
        if (!password.matches(".*[A-Z].*")) return false;

        // Al menos una letra minúscula
        if (!password.matches(".*[a-z].*")) return false;

        // Al menos un número
        if (!password.matches(".*\\d.*")) return false;

        return true;
    }

    private void subirImagenYDatos(ProgressDialog progressDialog, DatabaseReference userRef,
                                   String nombre, String apellidos, String telefono,
                                   String direccion, String codigoPostal, String colonia,
                                   String ciudad, String estado, String descripcion) {

        // Crear mapa de datos a actualizar (sin imagen por ahora)
        final Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", nombre);
        userData.put("apellidos", apellidos);
        userData.put("telefono", telefono);
        userData.put("direccion", direccion);
        userData.put("codigo_postal", codigoPostal);
        userData.put("colonia", colonia);
        userData.put("ciudad", ciudad);
        userData.put("estado", estado);
        userData.put("referencia", descripcion);

        // Si no hubo cambio de imagen, actualizar solo los datos
        if (!imagenModificada || currentImagePath == null) {
            // Si no hubo modificación de imagen, se actualizan solo los datos
            actualizarDatos(userRef, userData, progressDialog);
            return;
        }

        // Comprimir la imagen antes de subirla
        File compressedImageFile = compressImage(currentImagePath);

        // Si hay imagen nueva, subirla a Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        final StorageReference imageRef = storageRef.child("imagenes_usuarios/" + currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg");

        // Preparar imagen comprimida para subir
        Uri fileUri = Uri.fromFile(compressedImageFile);

        // Subir imagen a Storage
        imageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String nuevaUrl = uri.toString();
                        userData.put("imagenUrl", nuevaUrl);

                        // Obtener URL anterior de la base de datos
                        userRef.child("imagenUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String urlAnterior = snapshot.getValue(String.class);

                                // Verificar si la imagen anterior existe y no es la imagen por defecto
                                if (urlAnterior != null && !urlAnterior.isEmpty() && !urlAnterior.contains("default_profile.png")) {
                                    try {
                                        // Eliminar imagen anterior del Storage
                                        FirebaseStorage.getInstance().getReferenceFromUrl(urlAnterior)
                                                .delete()
                                                .addOnSuccessListener(unused -> Log.d("Storage", "Imagen anterior eliminada"))
                                                .addOnFailureListener(e -> Log.e("Storage", "No se pudo eliminar imagen anterior: " + e.getMessage()));
                                    } catch (Exception ex) {
                                        Log.e("Storage", "Error al intentar eliminar imagen anterior: " + ex.getMessage());
                                    }
                                }

                                // Finalmente, actualizar los datos en la base
                                actualizarDatos(userRef, userData, progressDialog);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("Storage", "Error al leer imagenUrl anterior: " + error.getMessage());
                                // Aunque no se pudo leer la imagen anterior, seguimos con la actualización
                                actualizarDatos(userRef, userData, progressDialog);
                            }
                        });
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(EditarPerfilActivity.this,
                                "Error al obtener URL de imagen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditarPerfilActivity.this,
                            "Error al subir imagen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    // Actualizar progreso de la subida si quieres
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Subiendo imagen: " + (int) progress + "%");
                });
    }


    // Metodo para comprimir la imagen
    private File compressImage(String imagePath) {
        try {
            // Cargar la imagen desde la ruta proporcionada
            Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);

            // Reducir el tamaño de la imagen si es muy grande
            int maxWidth = 1200; // Ajusta según tus necesidades
            int maxHeight = 1200; // Ajusta según tus necesidades

            float scale = Math.min(
                    (float) maxWidth / originalBitmap.getWidth(),
                    (float) maxHeight / originalBitmap.getHeight());

            int targetWidth = Math.round(scale * originalBitmap.getWidth());
            int targetHeight = Math.round(scale * originalBitmap.getHeight());

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);

            // Crear archivo temporal para la imagen comprimida
            File outputDir = this.getCacheDir();
            File outputFile = File.createTempFile("compressed_image", ".jpg", outputDir);

            // Comprimir y guardar la imagen
            FileOutputStream fos = new FileOutputStream(outputFile);

            // Ajusta la calidad de compresión según necesites (0-100)
            // Un valor de 80-85 generalmente da buen balance entre calidad y tamaño
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);

            fos.flush();
            fos.close();

            // Liberar recursos de los bitmaps
            if (originalBitmap != null && !originalBitmap.isRecycled()) {
                originalBitmap.recycle();
            }
            if (resizedBitmap != null && !resizedBitmap.isRecycled()) {
                resizedBitmap.recycle();
            }

            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            // Si ocurre algún error, retornar el archivo original
            return new File(imagePath);
        }
    }

    private void actualizarDatos(DatabaseReference userRef, Map<String, Object> userData, ProgressDialog progressDialog) {
        userRef.updateChildren(userData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();

                    // Limpiar estado de imagen local después de subir a Firebase
                    clearImagePreferences();

                    Toast.makeText(EditarPerfilActivity.this,
                            "Perfil actualizado correctamente",
                            Toast.LENGTH_SHORT).show();

                    // Opcional: Volver a la actividad anterior
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditarPerfilActivity.this,
                            "Error al actualizar datos: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}