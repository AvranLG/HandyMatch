package com.example.app;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
public class Retrieve_Password extends AppCompatActivity {
    TextInputEditText textEmail_text;
    FirebaseAuth auth;
    Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retrieve_password);
        textEmail_text = (TextInputEditText) findViewById(R.id.emailRecuperarText);
        resetButton = (Button) findViewById(R.id.btnRecuperar);
        auth = FirebaseAuth.getInstance();
    }

    private boolean CorreoValido(String correo) {
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            textEmail_text.setError("Correo electrónico inválido");
            return false;
        } else {
            textEmail_text.setError(null);
        }
        return true;
    }

    public void resetPwd(View v)
    {
        String email = textEmail_text.getText().toString().trim();
        if (CorreoValido(email)) {
            auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(Retrieve_Password.this,
                                "Te hemos enviado instrucciones para " +
                                        "restablecer tu contraseña",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Retrieve_Password.this,
                                "Error al enviar correo electrónico " +
                                        "para reestablecer contraseña",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public void abrirLogin(View v) {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }
}