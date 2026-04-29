# Projet de session - SMI1002 Hiver 2026
# Gestion de decodeurs TV - Interface administrateur

=====================================================
  IMPORTANT : LIRE ENTIEREMENT AVANT DE COMMENCER
=====================================================


## PREREQUIS - Installer avant tout

1. Java JDK (version 11 ou plus)
   Verifier en tapant dans un terminal : java -version
   Si pas installe : https://www.oracle.com/java/technologies/downloads/

2. Oracle SQL Developer
   Disponible sur le portail UQTR

3. VPN FortiVPN
   OBLIGATOIRE pour se connecter a Oracle
   Sans VPN, le programme ne peut pas demarrer


## STRUCTURE DU PROJET

Apres extraction du zip, vous devez avoir :

DecodeurAdmin_v4\               <- dossier principal
    DecodeurAdmin_v4\           <- entrer dans CE dossier
        src\
            ConnexionBD.java
            Main.java
        out\
            ConnexionBD.class
            Main.class
        ojdbc11.jar
        schema.sql
        README.txt

ATTENTION : il y a deux niveaux de dossiers DecodeurAdmin_v4.
Toutes les commandes doivent etre executees dans le dossier
interieur (celui qui contient ojdbc11.jar et schema.sql).


=====================================================
  ETAPE 1 - CONNECTER LE VPN
=====================================================

Connecter FortiVPN AVANT de faire quoi que ce soit.
Si le VPN n'est pas connecte, le programme affichera :
"Erreur : Unknown host gaia.emp.uqtr.ca"
et refusera de demarrer.


=====================================================
  ETAPE 2 - CREER LA BASE DE DONNEES
=====================================================

1. Ouvrir SQL Developer

2. Creer une connexion avec ces parametres :
   - Hote           : gaia.emp.uqtr.ca
   - Port           : 1521
   - Nom de service : coursbd.uqtr.ca
   - Utilisateur    : SMI1002_056
   - Mot de passe   : 83crpu64

3. Ouvrir le fichier schema.sql depuis SQL Developer

4. Appuyer sur F5 pour tout executer
   (Ne pas utiliser le bouton Play, utiliser F5)

5. A la fin du script, verifier que vous voyez :
   Clients   : 3
   Decodeurs : 12
   Libres    : 6
   Journal   : 15

   Si vous ne voyez pas ces chiffres, le script a eu
   des erreurs. Verifiez que vous etes bien connecte
   avec le bon compte Oracle.


=====================================================
  ETAPE 3 - LANCER L'APPLICATION
=====================================================

1. Ouvrir un terminal PowerShell

2. Naviguer vers le BON dossier (le dossier interieur) :

   cd C:\Users\VotreNom\Downloads\DecodeurAdmin_v4\DecodeurAdmin_v4

   Remplacer "VotreNom" par votre nom d'utilisateur Windows.
   Pour trouver votre chemin exact, regardez dans l'Explorateur
   de fichiers ou votre dossier Downloads.

3. Verifier que vous etes au bon endroit :

   dir

   Vous devez voir ojdbc11.jar et schema.sql dans la liste.
   Si vous ne les voyez pas, vous etes dans le mauvais dossier.

4. Lancer l'application :

   java -cp "out;ojdbc11.jar" Main

5. Vous devez voir :

   ==============================================
      Systeme de gestion - DecodeurTR
      Interface Administrateur
   ==============================================
   Connexion a Oracle reussie.
   ------ MENU PRINCIPAL ------
   1. Gestion des clients
   2. Gestion des decodeurs
   3. Voir le journal des transactions
   4. Afficher toute la base de donnees
   5. Verifier la coherence apres panne
   0. Quitter
   Votre choix :


=====================================================
  ERREURS COURANTES
=====================================================

Erreur : "Unknown host gaia.emp.uqtr.ca"
Solution : Connecter le VPN FortiVPN et reessayer.

Erreur : "Could not find or load main class Main"
Solution : Vous etes dans le mauvais dossier.
           Faire : cd DecodeurAdmin_v4
           puis relancer : java -cp "out;ojdbc11.jar" Main

Erreur : "driver Oracle introuvable"
Solution : Le fichier ojdbc11.jar n'est pas dans le dossier.
           Verifier avec : dir *.jar

Erreur : "error: file not found: src\ConnexionBD.java"
Solution : Vous melangez les commandes. La commande pour
           compiler est javac. La commande pour lancer est java.
           Voir ci-dessous si vous devez recompiler.


=====================================================
  SI VOUS DEVEZ RECOMPILER (optionnel)
=====================================================

Seulement si vous avez modifie le code source.
Les fichiers .class sont deja compiles dans le dossier out\.

Compiler :
  javac -cp ".;ojdbc11.jar" src\ConnexionBD.java src\Main.java -d out

Lancer :
  java -cp "out;ojdbc11.jar" Main
