package com.example.atcarremote;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.OutputStream;
import java.net.Socket;

import androidx.appcompat.app.AppCompatActivity;

public class AutonomeActivity extends AppCompatActivity {

    private Button startButton;
    private Button pauseButton;
    private Button returnToTelecommandeButton;
    private TextView carStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autonome);

        // Initialisation des éléments de l'interface
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        returnToTelecommandeButton = findViewById(R.id.returnToTelecommande);
        carStatusText = findViewById(R.id.carStatusText);

        // Event listeners
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAutonomousMode();
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseAutonomousMode();
            }
        });

        returnToTelecommandeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToTelecommandeMode();
            }
        });
    }

    // Méthode pour démarrer le mode autonome
    private void startAutonomousMode() {
        // Met à jour l'état de la voiture dans l'interface utilisateur
        carStatusText.setText("Voiture en mode autonome");

        // Envoie la commande "GO" au Raspberry Pi pour activer le mode autonome
        sendCommandToRaspberryPi("GO");
    }

    // Méthode pour mettre en pause le mode autonome (ex: en cas d'urgence)
    private void pauseAutonomousMode() {
        // Met à jour l'état de la voiture dans l'interface utilisateur
        carStatusText.setText("Mode autonome suspendu");

        // Envoie la commande "PAUSE" au Raspberry Pi
        sendCommandToRaspberryPi("PAUSE");
    }

    // Méthode pour revenir au mode télécommande
    private void returnToTelecommandeMode() {
        // Met à jour l'interface utilisateur
        carStatusText.setText("Retour au mode télécommande");

        // Création de l'intent pour démarrer l'activité de télécommande
        Intent intent = new Intent(AutonomeActivity.this, TelecommandeControlActivity.class);  // Remplace par le nom de ta classe d'activité télécommande
        startActivity(intent);  // Lance l'activité de télécommande
        finish();  // Optionnel : termine l'activité actuelle pour ne pas revenir à cette page
    }

    // Méthode pour envoyer la commande au Raspberry Pi via un socket
    private void sendCommandToRaspberryPi(final String command) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("AutonomeActivity", "Tentative de connexion au Raspberry Pi...");
                    // Remplacer par l'adresse IP et le port du Raspberry Pi
                    Socket socket = new Socket("192.168.43.61", 12345);
                    Log.d("AutonomeActivity", "Connexion établie");
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(command.getBytes());  // Envoie la commande (GO, PAUSE, etc.)
                    outputStream.flush();
                    socket.close();  // Ferme la connexion
                    Log.d("AutonomeActivity", "Commande envoyée et connexion fermée");
                } catch (Exception e) {
                    Log.e("AutonomeActivity", "Erreur lors de l'envoi de la commande", e);
                }
            }
        }).start();
    }
}
