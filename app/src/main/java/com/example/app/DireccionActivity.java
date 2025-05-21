package com.example.app;

import android.app.ProgressDialog;
import android.content.Intent;

import android.graphics.Color;
import android.os.Bundle;

import android.util.Log;

import android.view.MotionEvent;
import android.view.View;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
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
import com.google.firebase.database.FirebaseDatabase;

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
    private static final String GEOCODING_API_KEY = BuildConfig.GEOCODING_API_KEY;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direccion);

        // Verificar si hay un usuario autenticado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // No hay usuario autenticado, redirigir al login
            Toast.makeText(this, "Error de autenticación. Inicie sesión nuevamente.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Inicializar RequestQueue
        queue = Volley.newRequestQueue(this);

        // Referencias a los EditText
        direccionText = findViewById(R.id.direccionText);
        codigoPostalText = findViewById(R.id.postalText);
        coloniaText = findViewById(R.id.coloniaText);
        estadoText = findViewById(R.id.estadoText);
        ciudadText = findViewById(R.id.ciudadText);
        referenciaText = findViewById(R.id.descripcionText);

        // Detectar cuando se pierde el foco en el campo de código postal
        codigoPostalText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String codigoPostal = codigoPostalText.getText().toString().trim();
                if (codigoPostal.length() == 5) {
                    obtenerCiudadPorCodigoPostal(codigoPostal);
                }
            }
        });

        referenciaText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                referenciaText.scrollTo(0, 0); // Resetea el scroll al inicio
            }
        });

        referenciaText.setOnTouchListener((v, event) -> {
            if (v.getId() == R.id.descripcionText) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
            }
            return false;
        });

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));
    }


    // Metodo para validar dirección
    private boolean validarDireccion(String direccion) {
        // Permite letras (mayúsculas y minúsculas), números, espacios, guiones, puntos y #
        String regex = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-\\.#]+$";
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
        CheckBox terminos3 = findViewById(R.id.terminos3);
        if (!terminos3.isChecked()) {
            Toast.makeText(this, "Debes aceptar términos y condiciones para continuar", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Guardando dirección...");
        progressDialog.show();

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

        boolean camposValidos = true;

        if (!validarDireccion(direccion)) {
            direccionContainer.setError("Dirección inválida");
            camposValidos = false;
        } else direccionContainer.setError(null);

        if (!validarCodigoPostal(codigoPostal)) {
            postalContainer.setError("Código postal inválido");
            camposValidos = false;
        } else postalContainer.setError(null);

        if (!validarColonia(colonia)) {
            coloniaContainer.setError("Colonia inválida");
            camposValidos = false;
        } else coloniaContainer.setError(null);

        if (!validarCiudad(ciudad)) {
            ciudadContainer.setError("Ciudad inválida");
            camposValidos = false;
        } else ciudadContainer.setError(null);

        if (!validarReferencia(referencia)) {
            referenciaContainer.setError("Referencia inválida");
            camposValidos = false;
        } else referenciaContainer.setError(null);

        if (!camposValidos) {
            progressDialog.dismiss();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = user.getUid();

        // Guardar la dirección en Firebase
        UsuarioDireccion direccionObj = new UsuarioDireccion(
                direccion, codigoPostal, colonia, estado, ciudad, referencia
        );

        FirebaseDatabase.getInstance().getReference("usuarios")
                .child(uid)
                .child("direccion")
                .setValue(direccionObj)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Dirección guardada con éxito", Toast.LENGTH_SHORT).show();

                    // Ir a la pantalla de verificación o siguiente paso
                    Intent intent = new Intent(this, VerificacionCorreoActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error al guardar dirección: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public class UsuarioDireccion {
        public String direccion, codigoPostal, colonia, estado, ciudad, referencia;

        public UsuarioDireccion() {
            // Constructor vacío requerido por Firebase
        }

        public UsuarioDireccion(String direccion, String codigoPostal, String colonia,
                                String estado, String ciudad, String referencia) {
            this.direccion = direccion;
            this.codigoPostal = codigoPostal;
            this.colonia = colonia;
            this.estado = estado;
            this.ciudad = ciudad;
            this.referencia = referencia;
        }
    }


    private String obtenerFechaHoraActual() {
        // Obtener la fecha y hora actual usando Calendar
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Formatear la fecha y hora y devolverla como un String
        return dateFormat.format(calendar.getTime());
    }

    // Metodo para quitar el foco de todos los EditTexts
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
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + codigoPostal + ",MX&key=" + GEOCODING_API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray results = jsonObject.getJSONArray("results");

                        if (results.length() > 0) {
                            JSONArray addressComponents = results.getJSONObject(0).getJSONArray("address_components");

                            String ciudad = "";
                            String estado = "";

                            for (int i = 0; i < addressComponents.length(); i++) {
                                JSONObject component = addressComponents.getJSONObject(i);
                                JSONArray types = component.getJSONArray("types");

                                for (int j = 0; j < types.length(); j++) {
                                    String type = types.getString(j);

                                    if (type.equals("locality")) {
                                        ciudad = component.getString("long_name");
                                    }

                                    if (type.equals("administrative_area_level_1")) {
                                        estado = component.getString("long_name");
                                    }
                                }
                            }

                            String finalCiudad = ciudad;
                            String finalEstado = estado;
                            runOnUiThread(() -> {
                                if (!finalCiudad.isEmpty() || !finalEstado.isEmpty()) {
                                    ciudadText.setText(finalCiudad);
                                    estadoText.setText(finalEstado);
                                    estadoText.setEnabled(false);
                                } else {
                                    limpiarCampos();
                                }
                            });
                        } else {
                            runOnUiThread(() -> limpiarCampos());
                        }
                    } catch (JSONException e) {
                        Log.e("GeoAPI", "Error parseando JSON", e);
                        runOnUiThread(() -> limpiarCampos());
                    }
                } else {
                    Log.e("GeoAPI", "Respuesta fallida: " + response.code());
                    runOnUiThread(() -> limpiarCampos());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("GeoAPI", "Error de red", e);
                runOnUiThread(() -> limpiarCampos());
            }

            private void limpiarCampos() {
                ciudadText.setText("");
                estadoText.setText("");
                estadoText.setEnabled(true);
            }
        });
    }


    public void abrirTerminos(View v) {
        Intent i = new Intent(this, activity_terminos.class);
        startActivity(i);
    }

    public void abrirRegistro(View v) {
        Intent i = new Intent(this, RegistroActivity.class);
        startActivity(i);
    }
}
