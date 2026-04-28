# Projet de session - SMI1002 Hiver 2026
# Gestion de decodeurs TV - Interface administrateur

## Structure du projet

```
DecodeurAdmin_v4/
├── ojdbc11.jar            <- Driver Oracle (requis)
├── out/
│   ├── ConnexionBD.class  <- Fichiers compiles
│   └── Main.class
├── schema.sql             <- Script de creation de la base de donnees
├── README.txt             <- Ce fichier
└── src/
    ├── ConnexionBD.java   <- Gestion de la connexion Oracle
    └── Main.java          <- Application principale
```

---

## Prerequis

- Java JDK installe (version 11 ou plus)
- Oracle SQL Developer installe
- VPN FortiVPN connecte (reseau UQTR)

---

## Etape 1 - Creer la base de donnees

1. Connecter le VPN FortiVPN
2. Ouvrir SQL Developer et se connecter avec :
   - Hote            : gaia.emp.uqtr.ca
   - Port            : 1521
   - Nom de service  : coursbd.uqtr.ca
   - Utilisateur     : SMI1002_056
   - Mot de passe    : 83crpu64
3. Ouvrir le fichier schema.sql
4. Appuyer sur F5 pour tout executer
5. Verifier a la fin que vous voyez :
   Clients   : 3
   Decodeurs : 12
   Libres    : 6
   Journal   : 15

---

## Etape 2 - Lancer l'application

Les fichiers sont deja compiles dans le dossier out/.
Ouvrir un terminal dans le dossier DecodeurAdmin_v4 et taper :

  java -cp "out;ojdbc11.jar" Main

---

## Etape 3 - Si vous modifiez le code source

Si vous avez modifie ConnexionBD.java ou Main.java, recompilez avec :

  javac -cp ".;ojdbc11.jar" src\ConnexionBD.java src\Main.java -d out

Puis relancez :

  java -cp "out;ojdbc11.jar" Main

---

## Fonctionnalites disponibles

1. Gestion des clients
   - Lister tous les clients
   - Voir les decodeurs et chaines d'un client
   - Creer un nouveau client
   - Supprimer un client

2. Gestion des decodeurs
   - Lister tous les decodeurs
   - Voir les decodeurs libres
   - Assigner un decodeur a un client
   - Retirer un decodeur d'un client
   - Changer l'etat d'un decodeur
   - Ajouter ou retirer une chaine TV

3. Journal des transactions
   - Historique de toutes les operations effectuees

4. Afficher toute la base de donnees
   - Vue complete de toutes les tables

5. Verifier la coherence apres panne
   - Verification de l'integrite des donnees
