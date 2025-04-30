package com.example.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class EditarPerfilActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int UCROP_REQUEST_CODE = 69;

    ImageView profileImage, logoImagen;

    DatabaseReference databaseRef;
    FirebaseAuth auth;
    FirebaseUser currentUser;

    private static final String KEY_IMAGE_PATH = "cropped_image_path";
    private String currentImagePath = null;

    TextInputEditText nombreText, apellidosText, numeroText, direccionText, postalText, coloniaText;
    private EditText ciudadText;
    private EditText estadoText;
    private static final String GOOGLE_MAPS_API_KEY = BuildConfig.MAPS_API_KEY;

    // Variable para almacenar la URL de la imagen
    private String fotoUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        if (savedInstanceState != null) {
            currentImagePath = savedInstanceState.getString(KEY_IMAGE_PATH);
            if (currentImagePath != null) {
                File file = new File(currentImagePath);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath);
                    profileImage.setImageBitmap(bitmap);
                }
            }
        }

        profileImage = findViewById(R.id.profileImage);
        logoImagen = findViewById(R.id.logoImagen);

        // Escucha para abrir galería
        logoImagen.setOnClickListener(v -> abrirGaleria());

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        nombreText = findViewById(R.id.nombreText);
        apellidosText = findViewById(R.id.apellidosText);
        numeroText = findViewById(R.id.numeroText);
        direccionText = findViewById(R.id.direccionText);
        postalText = findViewById(R.id.postalText);
        coloniaText = findViewById(R.id.coloniaText);
        ciudadText = findViewById(R.id.ciudadText);
        estadoText = findViewById(R.id.estadoText);
        profileImage = findViewById(R.id.profileImage);

        if (currentUser != null) {
            String uid = currentUser.getUid();
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

                        nombreText.setText(nombre);
                        apellidosText.setText(apellidos);
                        numeroText.setText(telefono);
                        direccionText.setText(direccion);
                        postalText.setText(codigoPostal);
                        coloniaText.setText(colonia);
                    } else {
                        Toast.makeText(EditarPerfilActivity.this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show();
                    }

                    fotoUrl = snapshot.child("imagenUrl").getValue(String.class); // Guardamos la URL de la imagen

                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        Glide.with(EditarPerfilActivity.this)
                                .load(fotoUrl)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .placeholder(R.drawable.circulo_imagen)
                                .error(R.drawable.circulo_imagen)
                                .into(profileImage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(EditarPerfilActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            });
        }

        postalText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String codigoPostal = postalText.getText().toString().trim();
                if (codigoPostal.length() == 5) {
                    obtenerCiudadPorCodigoPostal(codigoPostal);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentImagePath != null) {
            outState.putString(KEY_IMAGE_PATH, currentImagePath);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Si el usuario selecciona una imagen
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri sourceUri = data.getData();

            // Uri temporal donde se guardará la imagen recortada
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "croppedImage.jpg"));

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
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                currentImagePath = resultUri.getPath();  // Guardamos la ruta para restaurarla luego

                Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath);
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap);
                    profileImage.invalidate();
                    profileImage.requestLayout();
                    Toast.makeText(this, "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Error al recortar: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restaurar la imagen de perfil si está disponible
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(fotoUrl)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.circulo_imagen)
                    .error(R.drawable.circulo_imagen)
                    .into(profileImage);
        }
    }

    private void obtenerCiudadPorCodigoPostal(String codigoPostal) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + codigoPostal + ",MX&key=" + GOOGLE_MAPS_API_KEY;

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
}
