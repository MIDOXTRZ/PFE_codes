package com.example.atcarremote;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ModeSelectionActivity extends AppCompatActivity {

    private static final String RASPBERRY_IP = "192.168.43.61";  // Remplacez par l'IP de votre Raspberry Pi
    private static final int RASPBERRY_PORT = 12345;  // Port sur lequel le Raspberry Pi écoute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selection);

        Button telecommandeButton = findViewById(R.id.buttonTelecommande);
        Button autonomeButton = findViewById(R.id.buttonAutonome);

        // Action pour le bouton Mode Télécommande
        telecommandeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Envoie un signal au Raspberry Pi pour activer le mode Télécommande
                sendModeToRaspberry("Mode Telecommande");
                // Navigation vers l'activité télécommande
                Intent intent = new Intent(ModeSelectionActivity.this, TelecommandeControlActivity.class);
                startActivity(intent);
            }
        });

        // Action pour le bouton Mode Autonome
        autonomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Envoie un signal au Raspberry Pi pour activer le mode Autonome
                sendModeToRaspberry("Mode Autonome");
                // Navigation vers l'activité autonome
                Intent intent = new Intent(ModeSelectionActivity.this, AutonomeActivity.class);
                startActivity(intent);
            }
        });
    }

    // Fonction pour envoyer le mode sélectionné au Raspberry Pi via TCP
    private void sendModeToRaspberry(final String mode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Crée une connexion socket TCP avec le Raspberry Pi
                    Socket socket = new Socket(RASPBERRY_IP, RASPBERRY_PORT);

                    // Crée un flux de sortie pour envoyer les données
                    OutputStream os = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(os, true);

                    // Envoie le mode au Raspberry Pi
                    writer.println(mode);

                    // Ferme la connexion après l'envoi du message
                    socket.close();

                    // Vérifie la réponse du serveur (ou simplement la réussite de l'envoi)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ModeSelectionActivity.this, "Mode " + mode + " activé", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ModeSelectionActivity.this, "Erreur de connexion avec le Raspberry Pi", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}
