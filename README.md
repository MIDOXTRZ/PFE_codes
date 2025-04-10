# PFE_codes
Voiture autonome et télécommandée basée sur Raspberry Pi (vision par caméra) et ESP32 (contrôle des moteurs), avec mode manuel et suivi de ligne intelligent.
# Autonomous RC Car - Raspberry Pi & ESP32  

Ce projet consiste en une **voiture autonome télécommandée**, combinant un **Raspberry Pi 4** (traitement d'image et serveur Flask) et un **ESP32** (contrôle des moteurs et suivi de ligne).  

## Fonctionnalités  
- **2 modes de conduite** :  
  - **Télécommandé** : Contrôle via une application mobile (mouvements + flux vidéo en direct).  
  - **Autonome** : Suivi de ligne avec capteurs TCRT5000 + détection de panneaux stop (arrêt automatique).  
- **Stack technique** :  
  - Raspberry Pi (Python/OpenCV/Flask) pour la vision par ordinateur.  
  - ESP32 (C++/Arduino) pour le contrôle des moteurs et capteurs.  
  - Communication série UART/SPI entre les microcontrôleurs.  

## Structure du Projet  
- `/raspberrypi` : Code Python (serveur Flask, traitement d'image).  
- `/esp32` : Code Arduino (contrôle PWM, lecture capteurs).  
- `/mobile-app` : Code de l'application (Android Studio).  

## Utilisation  
1. Flasher l'ESP32 avec le firmware fourni.  
2. Lancer le serveur Flask sur le Raspberry Pi.  
3. Connecter l'application mobile au réseau local.  

