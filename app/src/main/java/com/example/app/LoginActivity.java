package com.example.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

        // Cambiar el color de la barra de estado
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#ffffff")); // Cambia el color
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

    public void abrirDireccion(View v){
        Intent i = new Intent(this,DireccionActivity.class);
        startActivity(i);
    }
}