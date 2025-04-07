package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputEditText;

import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Publicar extends AppCompatActivity {

    private TextInputEditText fechaHoraEditText;
    private TextInputEditText ubicacionEditText;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publicar);

        // Inicializar Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "TU_API_KEY_DE_GOOGLE_MAPS");
        }

        fechaHoraEditText = findViewById(R.id.fechaHora);
        ubicacionEditText = findViewById(R.id.ubicacion);

        // Abrir selector de ubicación
        ubicacionEditText.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(Publicar.this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });

        // Lógica de fecha y hora (como ya hicimos antes)
        fechaHoraEditText.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(Publicar.this,
                    (view, yearSelected, monthSelected, dayOfMonth) -> {
                        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(Publicar.this,
                                (timeView, hourOfDay, minuteOfHour) -> {
                                    String fechaHora = String.format("%02d/%02d/%04d %02d:%02d",
                                            dayOfMonth, monthSelected + 1, yearSelected, hourOfDay, minuteOfHour);
                                    fechaHoraEditText.setText(fechaHora);
                                }, hour, minute, true);
                        timePickerDialog.show();
                    }, year, month, day);

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        //Codigo para la lista seleccionable
        AutoCompleteTextView spinnerCategoria = findViewById(R.id.spinnerCategoria);

        String[] categorias = {"Hogar", "Mascotas", "Eventos", "Clases", "Reparaciones", "Otros"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categorias
        );

        spinnerCategoria.setAdapter(adapter);

// Forzar la apertura del dropdown al primer clic
        spinnerCategoria.setOnTouchListener((v, event) -> {
            spinnerCategoria.showDropDown();  // Forzar la apertura del dropdown
            return false;  // Deja que el clic pase al comportamiento por defecto
        });





    }

    // Recibir el resultado de la ubicación
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                ubicacionEditText.setText(place.getAddress()); // o place.getName()
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, "Error al seleccionar ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
