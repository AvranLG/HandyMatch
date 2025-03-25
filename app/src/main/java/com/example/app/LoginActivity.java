package com.example.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private TextInputLayout emailContainer;
    private EditText emailText;
    private EditText passwordText;
    private TextView ErrContra;
    private TextView ErrCorreo;

    // Google Sign-In variables
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private static final int RC_SIGN_IN = 9001; // Código de solicitud para Google Sign-In

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // Asegúrate de haber configurado el web client ID
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");

        emailContainer = findViewById(R.id.emailContainer);
        emailText = findViewById(R.id.emailText);
        passwordText = findViewById(R.id.passwordText);
        ErrContra = findViewById(R.id.ErrContra);
        ErrCorreo = findViewById(R.id.ErrCorreo);

        ErrCorreo.setVisibility(View.INVISIBLE);
        ErrContra.setVisibility(View.INVISIBLE);

        // Configurar el color de la barra de estado
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#ffffff"));

        // Manejar el botón de retroceso con OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoSalida();
            }
        });

        // Configurar el botón de imagen para iniciar sesión con Google
        ImageView googleSignInButton = findViewById(R.id.google_login);  // Asegúrate de que el ID coincida
        googleSignInButton.setOnClickListener(view -> signInWithGoogle());
    }

    private void mostrarDialogoSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir de HandyMatch")
                .setMessage("¿Seguro que quieres salir de HandyMatch?")
                .setPositiveButton("Sí", (dialog, which) -> finishAffinity()) // Cierra la app
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Cancela el cierre
                .show();
    }

    public void abrirRegistro(View v) {
        Intent i = new Intent(this, RegistroActivity.class);
        startActivity(i);
    }

    public void abrirMain(View v) {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    public void login(View v) {
        ErrCorreo.setVisibility(View.INVISIBLE);
        ErrContra.setVisibility(View.INVISIBLE);
        String email = emailText.getText().toString().trim();
        String password = passwordText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese ambos campos", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userFound = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String dbEmail = snapshot.child("email").getValue(String.class);
                    String dbPassword = snapshot.child("contrasena").getValue(String.class);

                    if (dbEmail != null && dbEmail.equals(email)) {
                        //Correo encontrado
                        userFound = true;
                        if (dbPassword != null && dbPassword.equals(password)) {
                            //La contrasñea coincide e ingresa
                            Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            //Contraseña incorrecta
                            ErrContra.setVisibility(View.VISIBLE);
                        }
                        break;
                    }
                }

                if (!userFound) {
                    //Correo no encontrado
                    ErrCorreo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void abrirLogin(View v) {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }

    public void abrirDireccion(View v) {
        Intent i = new Intent(this, DireccionActivity.class);
        startActivity(i);
    }

    public void abrirHome(View v) {
        Intent i = new Intent(this, HomeActivity.class);
        startActivity(i);
    }
    // Método de autenticación con Google
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Obtener el resultado de la actividad de Google Sign-In
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Autenticación con Firebase usando el token de Google
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        com.google.firebase.auth.AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Si la autenticación es exitosa
                        Toast.makeText(LoginActivity.this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Autenticación fallida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}