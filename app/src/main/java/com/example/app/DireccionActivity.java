package com.example.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import okhttp3.MediaType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import android.widget.EditText;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.*;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DireccionActivity extends AppCompatActivity {

    private RequestQueue queue;

    // Declarar los EditText para la dirección
    private EditText direccionText;
    private EditText codigoPostalText;
    private EditText coloniaText;
    private EditText estadoText;
    private EditText ciudadText;
    private EditText referenciaText;

    // Variables para recibir datos de la actividad anterior
    private String nombre;
    private String apellidos;
    private String correo;
    private String contrasena;
    private String telefono;
    private String imagenUri;

    private static final String SUPABASE_URL = "https://yyaepcxpedvbkxsjldtf.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl5YWVwY3hwZWR2Ymt4c2psZHRmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDI5MjY5NTQsImV4cCI6MjA1ODUwMjk1NH0.B8WbrpGjiMWQxR2cNGKsJ_DXOQbmdA-DW8ygNfCbl_8";  // Coloca tu API key de Supabase aquí
    private static final String STORAGE_BUCKET_NAME = "imagenes-usuarios";  // Nombre del bucket en Supabase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direccion);

        // Inicializar RequestQueue
        queue = Volley.newRequestQueue(this);

        // Referencias a los EditText
        codigoPostalText = findViewById(R.id.postalText);
        ciudadText = findViewById(R.id.ciudadText);

        // Detectar cuando se pierde el foco en el campo de código postal
        codigoPostalText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String codigoPostal = codigoPostalText.getText().toString().trim();
                if (codigoPostal.length() == 5) {
                    obtenerCiudadPorCodigoPostal(codigoPostal);
                }
            }
        });

        // Recibir los datos de la actividad anterior (RegistroActivity)
        Intent intent = getIntent();
        nombre = intent.getStringExtra("nombre");
        apellidos = intent.getStringExtra("apellidos");
        correo = intent.getStringExtra("correo");
        contrasena = intent.getStringExtra("contrasena");
        telefono = intent.getStringExtra("telefono");
        imagenUri = intent.getStringExtra("imagenUri");

        // Inicializar los EditText
        direccionText = findViewById(R.id.direccionText);
        codigoPostalText = findViewById(R.id.postalText);
        coloniaText = findViewById(R.id.coloniaText);
        estadoText = findViewById(R.id.estadoText);
        ciudadText = findViewById(R.id.ciudadText);
        referenciaText = findViewById(R.id.referenciaText);

        referenciaText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                referenciaText.scrollTo(0, 0); // Resetea el scroll al inicio
            }
        });

        referenciaText.setOnTouchListener((v, event) -> {
            if (v.getId() == R.id.referenciaText) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
            }
            return false;
        });
    }

    // Método para enviar los datos a Firebase
    public void enviarDatosFirebase(View v) {
        // Recoger los datos de los campos de dirección
        String direccion = direccionText.getText().toString();
        String codigoPostal = codigoPostalText.getText().toString();
        String colonia = coloniaText.getText().toString();
        String estado = estadoText.getText().toString();
        String ciudad = ciudadText.getText().toString();
        String referencia = referenciaText.getText().toString();

        // Validar si algún campo está vacío
        if (direccion.isEmpty() || codigoPostal.isEmpty() || colonia.isEmpty() || estado.isEmpty() || ciudad.isEmpty() || referencia.isEmpty()) {
            // Mostrar un Toast con el mensaje de error
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
        } else {
            String fechaRegistro = obtenerFechaHoraActual();
            // Crear un objeto de usuario con los datos
            Usuario usuario = new Usuario(nombre, apellidos, correo, telefono, contrasena, fechaRegistro, direccion, codigoPostal, colonia, estado, ciudad, referencia, imagenUri);

            // Subir la imagen a Supabase y obtener la URL
            if (imagenUri != null && !imagenUri.isEmpty()) {
                Uri imageUri = Uri.parse(imagenUri);
                subirImagenASupabase(imageUri, usuario);
            } else {
                // Si no hay imagen, solo subimos los demás datos a Firebase
                subirDatosAFirebase(usuario);
            }
        }
    }

    private void subirImagenASupabase(Uri imageUri, Usuario usuario) {
        // Verificar que las credenciales estén configuradas
        if (TextUtils.isEmpty(SUPABASE_URL) || TextUtils.isEmpty(STORAGE_BUCKET_NAME) || TextUtils.isEmpty(API_KEY)) {
            Log.e("Supabase", "Credenciales de Supabase no configuradas correctamente");
            return;
        }

        // Nombre de archivo único
        String nombreArchivo = "imagen_" + System.currentTimeMillis() + ".jpg";

        // Construir URL de subida de Supabase
        String url = SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET_NAME + "/" + nombreArchivo;

        // Construir URL pública de la imagen
        String imagenUrlPublica = SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET_NAME + "/" + nombreArchivo;

        try {
            // Leer bytes de la imagen
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e("Supabase", "No se pudo abrir el InputStream de la imagen");
                return;
            }

            // Comprimir la imagen para reducir tamaño
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();

            // Crear cliente OkHttp con timeouts más largos
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            // Crear cuerpo de la solicitud
            RequestBody fileBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", nombreArchivo, fileBody)
                    .build();

            // Crear solicitud POST
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "multipart/form-data")
                    .post(requestBody)
                    .build();

            // Ejecutar solicitud de forma asíncrona
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseString = response.body().string();
                    Log.d("Supabase", "Respuesta completa de Supabase: " + responseString);

                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            // Guardar la URL pública en el usuario
                            usuario.setImagenUrl(imagenUrlPublica);
                            subirDatosAFirebase(usuario);
                            Toast.makeText(DireccionActivity.this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.e("Supabase", "Error en la respuesta: " + response.code() + " - " + responseString);
                        mostrarErrorEnUI("Error al subir la imagen: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Supabase", "Error en la solicitud", e);
                    mostrarErrorEnUI("Fallo al subir la imagen: " + e.getMessage());
                }

                private void mostrarErrorEnUI(String mensaje) {
                    runOnUiThread(() -> {
                        Toast.makeText(DireccionActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e("Supabase", "Error general al subir imagen", e);
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }


    private void subirDatosAFirebase(Usuario usuario) {
        // Referencia a la base de datos Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("usuarios");

        // Subir los datos del usuario a Firebase con su clave única de push
        myRef.push().setValue(usuario);

        // Mostrar un mensaje de éxito
        Toast.makeText(this, "Datos registrados exitosamente", Toast.LENGTH_SHORT).show();

        // Opcional: Redirigir a otra actividad, por ejemplo LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finaliza la actividad actual para que no pueda volver con el botón de retroceso
    }

    private String obtenerFechaHoraActual() {
        // Obtener la fecha y hora actual usando Calendar
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Formatear la fecha y hora y devolverla como un String
        return dateFormat.format(calendar.getTime());
    }

    // Método para quitar el foco de todos los EditTexts
    private void clearFocusFromAllEditTexts() {
        if (direccionText.hasFocus()) {
            direccionText.clearFocus();
        }
        if (codigoPostalText.hasFocus()) {
            codigoPostalText.clearFocus();
        }
        if (coloniaText.hasFocus()) {
            coloniaText.clearFocus();
        }
        if (estadoText.hasFocus()) {
            estadoText.clearFocus();
        }
        if (ciudadText.hasFocus()) {
            ciudadText.clearFocus();
        }
        if (referenciaText.hasFocus()) {
            referenciaText.clearFocus();
        }
    }

    private void obtenerCiudadPorCodigoPostal(String codigoPostal) {
        String url = "https://api.zippopotam.us/MX/" + codigoPostal;

        // Crear cliente OkHttp
        OkHttpClient client = new OkHttpClient();

        // Crear solicitud GET
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Ejecutar solicitud de forma asíncrona
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray places = jsonObject.getJSONArray("places");

                        if (places.length() > 0) {
                            String estado = places.getJSONObject(0).getString("state");
                            String ciudad = places.getJSONObject(0).getString("place name");

                            // Actualizar UI en el hilo principal
                            runOnUiThread(() -> {
                                ciudadText.setText(ciudad);
                                estadoText.setText(estado);
                                estadoText.setEnabled(false);
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("ObtenerCiudad", "Error parseando JSON", e);

                        // Manejar error en el hilo principal
                        runOnUiThread(() -> {
                            ciudadText.setText("");
                            estadoText.setText("");
                            estadoText.setEnabled(true);
                        });
                    }
                } else {
                    // Manejar errores de respuesta
                    Log.e("ObtenerCiudad", "Error en la respuesta: " + response.code());

                    runOnUiThread(() -> {
                        ciudadText.setText("");
                        estadoText.setText("");
                        estadoText.setEnabled(true);
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // Manejar errores de red
                Log.e("ObtenerCiudad", "Error de red", e);

                runOnUiThread(() -> {
                    ciudadText.setText("");
                    estadoText.setText("");
                    estadoText.setEnabled(true);
                });
            }
        });
    }
}
