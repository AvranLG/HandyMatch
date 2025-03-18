package com.example.handymatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AppCompatActivity;
public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextInputLayout emailContainer = findViewById(R.id.emailContainer);
        EditText emailText = findViewById(R.id.emailText);

        emailText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    emailContainer.setHint(null); // Borra el hint al presionar
                } else {
                    emailContainer.setHint(getString(R.string.email)); // Restaura el hint si pierde el foco sin texto
                }
            }
        });

    }
    public void abrirRegistro(View v){
        Intent i = new Intent(this,RegistroActivity.class);
        startActivity(i);
    }

    public void abrirMain(View v){
        Intent i = new Intent(this,MainActivity.class);
        startActivity(i);
    }

    public void abrirLogin(View v){
        Intent i = new Intent(this,LoginActivity.class);
        startActivity(i);
    }
}