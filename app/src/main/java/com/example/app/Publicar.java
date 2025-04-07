package com.example.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class Publicar extends AppCompatActivity {

    private TextInputEditText ubicacionEditText;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publicar);

        // Inicializar Places (asegúrate de que ya tienes la API Key)
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "TU_API_KEY_AQUÍ");
        }

        ubicacionEditText = findViewById(R.id.ubicacion);

        // Abrir selector de ubicación
        ubicacionEditText.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(
                    Place.Field.ID, Place.Field.NAME,
                    Place.Field.ADDRESS, Place.Field.LAT_LNG);

            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY, fields)
                    .build(Publicar.this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });
    }

    // Manejo del resultado del Autocomplete
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Si todo va bien, obtenemos el lugar
                Place place = Autocomplete.getPlaceFromIntent(data);
                String address = place.getAddress();
                ubicacionEditText.setText(address);  // Establecemos la dirección seleccionada
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // Si hubo un error
                Toast.makeText(this, "Error al seleccionar ubicación", Toast.LENGTH_SHORT).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Si el usuario cancela la selección
                Toast.makeText(this, "Selección de ubicación cancelada", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
