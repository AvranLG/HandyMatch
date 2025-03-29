package com.example.app;

import android.app.ProgressDialog;
import android.content.Intent;

import android.os.Bundle;

import android.util.Log;

import android.view.MotionEvent;
import android.view.View;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.android.volley.RequestQueue;
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

    // Metodo para validar dirección
    private boolean validarDireccion(String direccion) {
        // Permite letras (mayúsculas y minúsculas), números, espacios, guiones, puntos y #
        String regex = "^[a-zA-Z0-9\\s\\-\\.#]+$";
        return direccion.matches(regex) && direccion.length() >= 5 && direccion.length() <= 100;
    }

    // Metodo para validar código postal
    private boolean validarCodigoPostal(String codigoPostal) {
        // Valida que sean exactamente 5 dígitos
        String regex = "^\\d{5}$";
        return codigoPostal.matches(regex);
    }

    // Metodo para validar colonia
    private boolean validarColonia(String colonia) {
        // Permite solo letras y espacios
        String regex = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$";
        return colonia.matches(regex) && colonia.length() >= 2 && colonia.length() <= 50;
    }

    // Metodo para validar ciudad
    private boolean validarCiudad(String ciudad) {
        // Permite solo letras y espacios, incluyendo acentos
        String regex = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$";
        return ciudad.matches(regex) && ciudad.length() >= 2 && ciudad.length() <= 50;
    }

    // Metodo para validar referencia (opcional, pero recomendado)
    private boolean validarReferencia(String referencia) {
        // Permite letras, números, espacios, guiones, puntos y #
        String regex = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-\\.#]+$";
        return referencia.length() <= 200; // Opcional, pero con límite de longitud
    }

    // Metodo para enviar los datos a Firebase
    public void enviarDatosFirebase(View v) {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registrando...");
        progressDialog.show();

        // Recoger los datos de los campos de dirección
        String direccion = direccionText.getText().toString();
        String codigoPostal = codigoPostalText.getText().toString();
        String colonia = coloniaText.getText().toString();
        String estado = estadoText.getText().toString();
        String ciudad = ciudadText.getText().toString();
        String referencia = referenciaText.getText().toString();

        TextInputLayout direccionContainer = findViewById(R.id.direccionContainer);
        TextInputLayout postalContainer = findViewById(R.id.postalContainer);
        TextInputLayout coloniaContainer = findViewById(R.id.coloniaContainer);
        TextInputLayout ciudadContainer = findViewById(R.id.ciudadContainer);
        TextInputLayout referenciaContainer = findViewById(R.id.referenciaContainer);

        // Validar si algún campo está vacío
        if (direccion.isEmpty() || codigoPostal.isEmpty() || colonia.isEmpty() || estado.isEmpty() || ciudad.isEmpty() || referencia.isEmpty()) {
            // Mostrar un Toast con el mensaje de error
            progressDialog.dismiss();
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
        } else {

            // Bandera para rastrear validación general
            boolean todosLosCamposValidos = true;

            // Validar dirección
            if (!validarDireccion(direccion)) {
                direccionContainer.setErrorEnabled(true);
                direccionContainer.setError("Dirección inválida. Use letras, números, espacios, -,.#");
                todosLosCamposValidos = false;
            } else {
                direccionContainer.setErrorEnabled(false);
                direccionContainer.setError(null);
            }

            // Validar código postal
            if (!validarCodigoPostal(codigoPostal)) {
                postalContainer.setErrorEnabled(true);
                postalContainer.setError("Código postal inválido. Debe contener 5 dígitos.");
                todosLosCamposValidos = false;
            } else {
                postalContainer.setErrorEnabled(false);
                postalContainer.setError(null);
            }

            // Validar colonia
            if (!validarColonia(colonia)) {
                coloniaContainer.setErrorEnabled(true);
                coloniaContainer.setError("Colonia inválida. Solo letras y espacios");
                todosLosCamposValidos = false;
            } else {
                coloniaContainer.setErrorEnabled(false);
                coloniaContainer.setError(null);
            }

            // Validar ciudad
            if (!validarCiudad(ciudad)) {
                ciudadContainer.setErrorEnabled(true);
                ciudadContainer.setError("Ciudad inválida. Solo letras y espacios");
                todosLosCamposValidos = false;
            } else {
                ciudadContainer.setErrorEnabled(false);
                ciudadContainer.setError(null);
            }

            // Validar referencia
            if (!validarReferencia(referencia)) {
                referenciaContainer.setErrorEnabled(true);
                referenciaContainer.setError("Referencia inválida. Caracteres no permitidos");
                todosLosCamposValidos = false;
            } else {
                referenciaContainer.setErrorEnabled(false);
                referenciaContainer.setError(null);
            }

            // Si hay algún error, no continuar
            if (!todosLosCamposValidos) {
                progressDialog.dismiss();
                return;
            }

            // Obtener instancia de FirebaseAuth
            FirebaseAuth mAuth = FirebaseAuth.getInstance();

            // Crear usuario en Firebase Authentication
            mAuth.createUserWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Registro exitoso en Authentication
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Enviar correo de verificación
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verificationTask -> {
                                            if (verificationTask.isSuccessful()) {
                                                // Preparar objeto de usuario
                                                String fechaRegistro = obtenerFechaHoraActual();
                                                Usuario usuario = new Usuario(
                                                        nombre,
                                                        apellidos,
                                                        correo,
                                                        telefono,
                                                        contrasena,
                                                        fechaRegistro,
                                                        direccion,
                                                        codigoPostal,
                                                        colonia,
                                                        estado,
                                                        ciudad,
                                                        referencia,
                                                        imagenUri
                                                );

                                                // Establecer el UID de Firebase Auth en el usuario
                                                usuario.setUid(user.getUid());

                                                // Redirigir a una pantalla de verificación de correo
                                                Intent intent = new Intent(this, VerificacionCorreoActivity.class);
                                                intent.putExtra("usuario", usuario);
                                                intent.putExtra("imagenUri", imagenUri);
                                                startActivity(intent);

                                                progressDialog.dismiss();
                                                finish();
                                            } else {
                                                // Error al enviar correo de verificación
                                                progressDialog.dismiss();
                                                Toast.makeText(DireccionActivity.this,
                                                        "Error al enviar correo de verificación",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            // Error en el registro de Authentication
                            progressDialog.dismiss();
                            Toast.makeText(DireccionActivity.this,
                                    "Error en el registro: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });


        }
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
