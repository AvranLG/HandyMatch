package com.example.app;

import android.app.ProgressDialog;  // Importa la clase ProgressDialog
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

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

    private ProgressDialog progressDialog;  // Variable para el ProgressDialog

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

        // Inicializar el ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Iniciando sesión...");  // Mensaje que se mostrará
        progressDialog.setCancelable(false);  // No permite cancelar el ProgressDialog tocando fuera de él

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

        Button btnLogin = findViewById(R.id.btnLogin);
        ProgressBar loginProgress = findViewById(R.id.loginProgressBar);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese ambos campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cerrar sesión de Google si está activa
        FirebaseAuth.getInstance().signOut();

        // Mostrar el ProgressBar y ocultar el texto del botón
        btnLogin.setText("");
        loginProgress.setVisibility(View.VISIBLE);

        // Iniciar sesión con Firebase Authentication
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Ocultar el ProgressBar y restaurar el texto del botón
                    btnLogin.setText("Iniciar sesión");
                    loginProgress.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                        ErrContra.setVisibility(View.VISIBLE);
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

    public void abrirRetrieve(View v) {
        Intent i = new Intent(this, Retrieve_Password.class);
        startActivity(i);
    }

    // Metodo de autenticación con Google
    private void signInWithGoogle() {
        // Mostrar el ProgressDialog cuando el usuario haga click en el botón de Google Sign-In
        progressDialog.show();

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
                progressDialog.dismiss();  // Cerrar el ProgressDialog en caso de fallo
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
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Verificar si el usuario existe en la base de datos
                        if (user != null) {
                            String userId = user.getUid();
                            checkUserExistsAndSave(userId, acct);
                        }

                        Toast.makeText(LoginActivity.this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                        progressDialog.dismiss();  // Cerrar el ProgressDialog cuando se haya hecho login exitoso
                    } else {
                        Toast.makeText(LoginActivity.this, "Autenticación fallida", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();  // Cerrar el ProgressDialog si hay fallo
                    }
                });
    }

    // Metodo para verificar si el usuario existe y guardarlo si no
    private void checkUserExistsAndSave(String userId, GoogleSignInAccount acct) {
        DatabaseReference userRef = databaseReference.child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // El usuario no existe, crear uno nuevo
                    saveUserData(userId, acct);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Error al verificar usuario", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();  // Cerrar el ProgressDialog si ocurre un error
            }
        });
    }

    // Metodo para guardar los datos del usuario en la base de datos
    private void saveUserData(String userId, GoogleSignInAccount acct) {
        // Obtener datos del usuario
        String email = acct.getEmail();
        String nombre = acct.getGivenName(); // Nombre
        String apellidos = acct.getFamilyName(); // Apellidos
        String photoUrl = acct.getPhotoUrl() != null ? acct.getPhotoUrl().toString() : ""; // URL de la foto

        // Crear mapa de datos
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("nombre", nombre != null ? nombre : "");
        userData.put("apellidos", apellidos != null ? apellidos : "");
        userData.put("googleId", acct.getId());
        userData.put("uid", userId); // Guardamos el UID de Firebase Authentication
        userData.put("tipoLogin", "google"); // Para diferenciar el tipo de inicio de sesión
        userData.put("imagenUrl", photoUrl); // Guardar la URL de la foto de perfil

        // Guardar en la base de datos usando el UID como clave
        databaseReference.child(userId).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    // Datos guardados exitosamente
                    Toast.makeText(LoginActivity.this, "Perfil creado con éxito", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Error al guardar los datos
                    Toast.makeText(LoginActivity.this, "Error al crear perfil", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();  // Cerrar el ProgressDialog si hay error al guardar datos
                });
    }


    public void abrirTerminos(View view) {
        Intent i = new Intent(this, activity_terminos.class);
        startActivity(i);
    }

}
