package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class EditarPerfilActivity extends AppCompatActivity {
    DatabaseReference databaseRef;
    FirebaseAuth auth;
    FirebaseUser currentUser;

    TextInputEditText nombreText, apellidosText, numeroText, direccionText, postalText, coloniaText;

    private EditText ciudadText;
    private EditText estadoText;
    private static final String GOOGLE_MAPS_API_KEY = BuildConfig.MAPS_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

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

        postalText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String codigoPostal = postalText.getText().toString().trim();
                if (codigoPostal.length() == 5) {
                    obtenerCiudadPorCodigoPostal(codigoPostal);
                }
            }
        });



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
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(EditarPerfilActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void obtenerCiudadPorCodigoPostal(String codigoPostal) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + codigoPostal + ",MX&key=" + GOOGLE_MAPS_API_KEY;

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


}