import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Scanner;

// =========================================================
// Projet de session - SMI1002 Hiver 2026
// Application console - Interface administrateur
// Gestion de decodeurs TV pour un cablodistributeur
// =========================================================
public class Main {

    static Scanner clavier = new Scanner(System.in);

    public static void main(String[] args) {

        System.out.println("==============================================");
        System.out.println("   Systeme de gestion - DecodeurTR");
        System.out.println("   Interface Administrateur");
        System.out.println("==============================================");

        // Si l'application se ferme brutalement (CTRL+C, crash, etc.)
        // sans avoir fait COMMIT, Oracle annule automatiquement toutes
        // les modifications non confirmees grace a ses ROLLBACK SEGMENTS.
        // Ce shutdown hook s'assure aussi qu'on ferme proprement la connexion.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println("Fermeture detectee - Oracle va annuler les transactions non confirmees.");
            ConnexionBD.fermer();
        }));

        // Tester la connexion avant de commencer
        try {
            Connection conn = ConnexionBD.getConnexion();
            System.out.println("Connexion a Oracle reussie.");
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Impossible de se connecter a Oracle.");
            System.out.println("Erreur : " + e.getMessage());
            System.out.println("Verifiez vos parametres dans ConnexionBD.java");
            return;
        }

        // Menu principal
        int choix = -1;
        while (choix != 0) {
            System.out.println("------ MENU PRINCIPAL ------");
            System.out.println("1. Gestion des clients");
            System.out.println("2. Gestion des decodeurs");
            System.out.println("3. Voir le journal des transactions");
            System.out.println("4. Afficher toute la base de donnees");
            System.out.println("5. Verifier la coherence apres panne");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");
            choix = lireEntier();

            switch (choix) {
                case 1:
                    menuClients();
                    break;
                case 2:
                    menuDecodeurs();
                    break;
                case 3:
                    afficherJournal();
                    break;
                case 4:
                    afficherTouteLaBD();
                    break;
                case 5:
                    verifierCoherence();
                    break;
                case 0:
                    System.out.println("Au revoir.");
                    break;
                default:
                    System.out.println("Choix invalide.");
            }
            System.out.println();
        }

        ConnexionBD.fermer();
        clavier.close();
    }

    // =========================================================
    // MENU CLIENTS
    // =========================================================
    static void menuClients() {
        int choix = -1;
        while (choix != 0) {
            System.out.println();
            System.out.println("--- GESTION DES CLIENTS ---");
            System.out.println("1. Lister tous les clients");
            System.out.println("2. Voir les decodeurs d'un client");
            System.out.println("3. Creer un nouveau client");
            System.out.println("4. Supprimer un client");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            choix = lireEntier();

            switch (choix) {
                case 1:
                    listerClients();
                    break;
                case 2:
                    voirDecodeursDuClient();
                    break;
                case 3:
                    creerClient();
                    break;
                case 4:
                    supprimerClient();
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Choix invalide.");
            }
        }
    }

    // Afficher la liste de tous les clients
    static void listerClients() {
        // cette requete utilise l'index IDX_DECODEUR_CLIENT
        // pour le COUNT dans la jointure avec DECODEUR.
        // Sans index : Oracle ferait un balayage complet de la table DECODEUR.
        // Avec l'index sur ID_CLIENT : Oracle accedera directement aux lignes.
        String sql = "SELECT C.ID_CLIENT, C.NOM_CLIENT, C.ADRESSE, "
                + "COUNT(D.ID_DECODEUR) AS NB_DECODEURS "
                + "FROM CLIENT C "
                + "LEFT JOIN DECODEUR D ON D.ID_CLIENT = C.ID_CLIENT "
                + "GROUP BY C.ID_CLIENT, C.NOM_CLIENT, C.ADRESSE "
                + "ORDER BY C.ID_CLIENT";

        try {
            Connection conn = ConnexionBD.getConnexion();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println();
            System.out.println("ID  | NOM CLIENT                    | ADRESSE                          | DECODEURS");
            System.out.println("----+-------------------------------+----------------------------------+----------");

            boolean vide = true;
            while (rs.next()) {
                vide = false;
                System.out.printf("%-3d | %-29s | %-32s | %d%n",
                        rs.getInt("ID_CLIENT"),
                        rs.getString("NOM_CLIENT"),
                        rs.getString("ADRESSE"),
                        rs.getInt("NB_DECODEURS"));
            }

            if (vide) {
                System.out.println("Aucun client dans la base de donnees.");
            }

            rs.close();
            st.close();

        } catch (SQLException e) {
            System.out.println("Erreur lors de la lecture des clients : " + e.getMessage());
        }
    }

    // Voir les decodeurs et les chaines d'un client
    static void voirDecodeursDuClient() {
        System.out.print("Entrez l'ID du client : ");
        int idClient = lireEntier();

        // Requete 1 : trouver les decodeurs du client
        // utilise l'index IDX_DECODEUR_CLIENT sur ID_CLIENT
        String sqlDec = "SELECT ID_DECODEUR, ADRESSE_IP, ETAT "
                + "FROM DECODEUR "
                + "WHERE ID_CLIENT = ? "
                + "ORDER BY ID_DECODEUR";

        // Requete 2 : trouver les chaines d'un decodeur
        String sqlChaines = "SELECT CHAINE FROM DECODEUR_CHAINES "
                + "WHERE ID_DECODEUR = ? "
                + "ORDER BY CHAINE";

        try {
            Connection conn = ConnexionBD.getConnexion();

            PreparedStatement stDec = conn.prepareStatement(sqlDec);
            stDec.setInt(1, idClient);
            ResultSet rsDec = stDec.executeQuery();

            System.out.println();
            System.out.println("Decodeurs du client #" + idClient + " :");
            System.out.println("----------------------------------------");

            boolean vide = true;
            while (rsDec.next()) {
                vide = false;
                int idDec = rsDec.getInt("ID_DECODEUR");
                System.out.println("  Decodeur #" + idDec
                        + " | IP: " + rsDec.getString("ADRESSE_IP")
                        + " | Etat: " + rsDec.getString("ETAT"));

                // Afficher les chaines de ce decodeur
                PreparedStatement stChaines = conn.prepareStatement(sqlChaines);
                stChaines.setInt(1, idDec);
                ResultSet rsChaines = stChaines.executeQuery();

                System.out.print("     Chaines : ");
                boolean premiereChaine = true;
                while (rsChaines.next()) {
                    if (!premiereChaine)
                        System.out.print(", ");
                    System.out.print(rsChaines.getString("CHAINE"));
                    premiereChaine = false;
                }
                if (premiereChaine)
                    System.out.print("(aucune)");
                System.out.println();

                rsChaines.close();
                stChaines.close();
            }

            if (vide) {
                System.out.println("  Aucun decodeur assigne a ce client.");
            }

            rsDec.close();
            stDec.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // Creer un nouveau client avec son compte utilisateur
    // On passe par la procedure stockee qui gere le COMMIT et ROLLBACK
    static void creerClient() {
        System.out.println();
        System.out.println("--- Creer un nouveau client ---");
        System.out.print("Nom du client : ");
        String nom = clavier.nextLine().trim();
        System.out.print("Adresse : ");
        String adresse = clavier.nextLine().trim();
        System.out.print("Identifiant de connexion : ");
        String identifiant = clavier.nextLine().trim();
        System.out.print("Mot de passe : ");
        String mdp = clavier.nextLine().trim();

        if (nom.isEmpty() || adresse.isEmpty() || identifiant.isEmpty() || mdp.isEmpty()) {
            System.out.println("Tous les champs sont obligatoires.");
            return;
        }

        // Appel de la procedure stockee
        // la procedure utilise COMMIT si tout va bien,
        // et ROLLBACK en cas d'erreur (propriete Atomicite des transactions)
        String sql = "{ CALL PROC_CREER_CLIENT(?, ?, ?, ?, ?) }";

        try {
            Connection conn = ConnexionBD.getConnexion();
            CallableStatement cs = conn.prepareCall(sql);
            cs.setString(1, nom);
            cs.setString(2, adresse);
            cs.setString(3, identifiant);
            cs.setString(4, mdp);
            cs.registerOutParameter(5, Types.VARCHAR);
            cs.execute();

            String message = cs.getString(5);
            System.out.println("Resultat : " + message);
            cs.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // Supprimer un client
    static void supprimerClient() {
        listerClients();
        System.out.print("Entrez l'ID du client a supprimer : ");
        int idClient = lireEntier();

        System.out.print("Confirmer la suppression du client #" + idClient + " ? (o/n) : ");
        String confirmation = clavier.nextLine().trim();
        if (!confirmation.equalsIgnoreCase("o")) {
            System.out.println("Suppression annulee.");
            return;
        }

        String sql = "{ CALL PROC_SUPPRIMER_CLIENT(?, ?) }";

        try {
            Connection conn = ConnexionBD.getConnexion();
            CallableStatement cs = conn.prepareCall(sql);
            cs.setInt(1, idClient);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();

            String message = cs.getString(2);
            System.out.println("Resultat : " + message);
            cs.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // MENU DECODEURS
    // =========================================================
    static void menuDecodeurs() {
        int choix = -1;
        while (choix != 0) {
            System.out.println();
            System.out.println("--- GESTION DES DECODEURS ---");
            System.out.println("1. Lister tous les decodeurs");
            System.out.println("2. Voir les decodeurs libres");
            System.out.println("3. Assigner un decodeur a un client");
            System.out.println("4. Retirer un decodeur d'un client");
            System.out.println("5. Changer l'etat d'un decodeur");
            System.out.println("6. Ajouter une chaine TV");
            System.out.println("7. Retirer une chaine TV");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            choix = lireEntier();

            switch (choix) {
                case 1:
                    listerDecodeurs();
                    break;
                case 2:
                    listerLibres();
                    break;
                case 3:
                    assignerDecodeur();
                    break;
                case 4:
                    retirerDecodeur();
                    break;
                case 5:
                    changerEtat();
                    break;
                case 6:
                    ajouterChaine();
                    break;
                case 7:
                    retirerChaine();
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Choix invalide.");
            }
        }
    }

    // Lister tous les decodeurs avec leur client associe
    static void listerDecodeurs() {
        String sql = "SELECT D.ID_DECODEUR, D.ADRESSE_IP, D.ETAT, C.NOM_CLIENT "
                + "FROM DECODEUR D "
                + "LEFT JOIN CLIENT C ON C.ID_CLIENT = D.ID_CLIENT "
                + "ORDER BY D.ID_DECODEUR";

        try {
            Connection conn = ConnexionBD.getConnexion();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println();
            System.out.println("ID  | ADRESSE IP     | ETAT        | CLIENT");
            System.out.println("----+----------------+-------------+------------------------------");

            while (rs.next()) {
                String nomClient = rs.getString("NOM_CLIENT");
                if (nomClient == null)
                    nomClient = "(libre)";
                System.out.printf("%-3d | %-14s | %-11s | %s%n",
                        rs.getInt("ID_DECODEUR"),
                        rs.getString("ADRESSE_IP"),
                        rs.getString("ETAT"),
                        nomClient);
            }

            rs.close();
            st.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // Lister seulement les decodeurs libres (via la vue)
    static void listerLibres() {
        // On utilise la vue VUE_DECODEURS_LIBRES creee dans schema.sql
        String sql = "SELECT * FROM VUE_DECODEURS_LIBRES";

        try {
            Connection conn = ConnexionBD.getConnexion();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println();
            System.out.println("Decodeurs disponibles (non assignes) :");
            System.out.println("ID  | ADRESSE IP     | ETAT");
            System.out.println("----+----------------+------------");

            boolean vide = true;
            while (rs.next()) {
                vide = false;
                System.out.printf("%-3d | %-14s | %s%n",
                        rs.getInt("ID_DECODEUR"),
                        rs.getString("ADRESSE_IP"),
                        rs.getString("ETAT"));
            }

            if (vide) {
                System.out.println("Aucun decodeur libre disponible.");
            }

            rs.close();
            st.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // Assigner un decodeur a un client
    // Concurrence :
    // C'est l'operation la plus sensible aux acces concurrents.
    // Deux admins sur deux postes differents pourraient voir le
    // meme decodeur libre et essayer de l'assigner en meme temps.
    // Pour eviter ca, la procedure stockee utilise SELECT FOR UPDATE
    // qui pose un verrou sur la ligne avant de verifier et modifier.
    static void assignerDecodeur() {
        listerLibres();
        System.out.print("ID du decodeur a assigner : ");
        int idDec = lireEntier();

        listerClients();
        System.out.print("ID du client : ");
        int idClient = lireEntier();

        String sql = "{ CALL PROC_ASSIGNER_DECODEUR(?, ?, ?) }";

        try {
            Connection conn = ConnexionBD.getConnexion();
            CallableStatement cs = conn.prepareCall(sql);
            cs.setInt(1, idDec);
            cs.setInt(2, idClient);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();

            System.out.println("Resultat : " + cs.getString(3));
            cs.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // Retirer un decodeur de son client
    static void retirerDecodeur() {
        listerDecodeurs();
        System.out.print("ID du decodeur a retirer : ");
        int idDec = lireEntier();

        String sql = "{ CALL PROC_RETIRER_DECODEUR(?, ?) }";

        try {
            Connection conn = ConnexionBD.getConnexion();
            CallableStatement cs = conn.prepareCall(sql);
            cs.setInt(1, idDec);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();

            System.out.println("Resultat : " + cs.getString(2));
            cs.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // Changer l'etat d'un decodeur (EN_LIGNE ou HORS_LIGNE)
    // on fait un COMMIT explicite apres l'UPDATE
    static void changerEtat() {
        listerDecodeurs();
        System.out.print("ID du decodeur : ");
        int idDec = lireEntier();

        System.out.println("Choisir le nouvel etat :");
        System.out.println("1. EN_LIGNE");
        System.out.println("2. HORS_LIGNE");
        System.out.print("Votre choix : ");
        int choixEtat = lireEntier();

        String nouvelEtat;
        if (choixEtat == 1) {
            nouvelEtat = "EN_LIGNE";
        } else if (choixEtat == 2) {
            nouvelEtat = "HORS_LIGNE";
        } else {
            System.out.println("Choix invalide.");
            return;
        }

        String sql = "UPDATE DECODEUR SET ETAT = ? WHERE ID_DECODEUR = ?";

        try {
            Connection conn = ConnexionBD.getConnexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nouvelEtat);
            ps.setInt(2, idDec);

            int lignesModifiees = ps.executeUpdate();

            if (lignesModifiees == 0) {
                System.out.println("Decodeur introuvable.");
                conn.rollback();
            } else {
                // Chapitre 14 : COMMIT pour confirmer la transaction
                conn.commit();
                System.out.println("Etat mis a jour : " + nouvelEtat);
            }

            ps.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
            try {
                // Chapitre 14 : ROLLBACK en cas d'erreur
                ConnexionBD.getConnexion().rollback();
            } catch (SQLException e2) {
                System.out.println("Erreur ROLLBACK : " + e2.getMessage());
            }
        }
    }

    // Ajouter une chaine TV a un decodeur
    static void ajouterChaine() {
        System.out.print("ID du decodeur : ");
        int idDec = lireEntier();

        System.out.print("Nom de la chaine (ex: TVA, RDS, CNN) : ");
        String chaine = clavier.nextLine().trim().toUpperCase();

        // Verifier si la chaine existe deja (eviter les doublons)
        String sqlVerif = "SELECT COUNT(*) FROM DECODEUR_CHAINES "
                + "WHERE ID_DECODEUR = ? AND CHAINE = ?";

        String sqlInsert = "INSERT INTO DECODEUR_CHAINES (ID_DECODEUR, CHAINE) VALUES (?, ?)";

        try {
            Connection conn = ConnexionBD.getConnexion();

            PreparedStatement psVerif = conn.prepareStatement(sqlVerif);
            psVerif.setInt(1, idDec);
            psVerif.setString(2, chaine);
            ResultSet rs = psVerif.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            psVerif.close();

            if (count > 0) {
                System.out.println("Cette chaine existe deja sur ce decodeur.");
                return;
            }

            PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
            psInsert.setInt(1, idDec);
            psInsert.setString(2, chaine);
            psInsert.executeUpdate();

            // COMMIT apres l'insertion
            conn.commit();
            System.out.println("Chaine '" + chaine + "' ajoutee au decodeur #" + idDec);
            psInsert.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
            try {
                ConnexionBD.getConnexion().rollback();
            } catch (SQLException e2) {
                System.out.println("Erreur ROLLBACK : " + e2.getMessage());
            }
        }
    }

    // Retirer une chaine TV d'un decodeur
    static void retirerChaine() {
        System.out.print("ID du decodeur : ");
        int idDec = lireEntier();

        // Afficher les chaines actuelles
        String sqlChaines = "SELECT CHAINE FROM DECODEUR_CHAINES "
                + "WHERE ID_DECODEUR = ? ORDER BY CHAINE";

        try {
            Connection conn = ConnexionBD.getConnexion();

            PreparedStatement ps = conn.prepareStatement(sqlChaines);
            ps.setInt(1, idDec);
            ResultSet rs = ps.executeQuery();

            System.out.println("Chaines du decodeur #" + idDec + " :");
            boolean vide = true;
            while (rs.next()) {
                vide = false;
                System.out.println("  - " + rs.getString("CHAINE"));
            }
            rs.close();
            ps.close();

            if (vide) {
                System.out.println("  Ce decodeur n'a aucune chaine.");
                return;
            }

            System.out.print("Nom de la chaine a retirer : ");
            String chaine = clavier.nextLine().trim().toUpperCase();

            String sqlDelete = "DELETE FROM DECODEUR_CHAINES "
                    + "WHERE ID_DECODEUR = ? AND CHAINE = ?";

            PreparedStatement psDel = conn.prepareStatement(sqlDelete);
            psDel.setInt(1, idDec);
            psDel.setString(2, chaine);
            int lignes = psDel.executeUpdate();

            if (lignes == 0) {
                System.out.println("Chaine introuvable sur ce decodeur.");
                conn.rollback();
            } else {
                conn.commit();
                System.out.println("Chaine '" + chaine + "' retiree.");
            }
            psDel.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
            try {
                ConnexionBD.getConnexion().rollback();
            } catch (SQLException e2) {
                System.out.println("Erreur ROLLBACK : " + e2.getMessage());
            }
        }
    }

    // =========================================================
    // JOURNAL DES TRANSACTIONS
    // Afficher l'historique de toutes les operations
    // =========================================================
    static void afficherJournal() {
        // le journal permet de savoir qui a fait quoi
        // et quand, pour la recuperation en cas de panne
        // utilise l'index IDX_JOURNAL_DATE pour le tri
        String sql = "SELECT ID_JOURNAL, DATE_HEURE, TYPE_OPERATION, "
                + "TABLE_MODIFIEE, DESCRIPTION "
                + "FROM VUE_JOURNAL "
                + "WHERE ROWNUM <= 30";

        try {
            Connection conn = ConnexionBD.getConnexion();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println();
            System.out.println("--- JOURNAL DES TRANSACTIONS (30 dernieres) ---");
            System.out.println("ID  | DATE                | TYPE   | TABLE      | DESCRIPTION");
            System.out.println("----+---------------------+--------+------------+---------------------------");

            boolean vide = true;
            while (rs.next()) {
                vide = false;
                String desc = rs.getString("DESCRIPTION");
                if (desc != null && desc.length() > 40) {
                    desc = desc.substring(0, 40) + "...";
                }
                System.out.printf("%-3d | %-19s | %-6s | %-10s | %s%n",
                        rs.getInt("ID_JOURNAL"),
                        rs.getString("DATE_HEURE"),
                        rs.getString("TYPE_OPERATION"),
                        rs.getString("TABLE_MODIFIEE"),
                        desc);
            }

            if (vide) {
                System.out.println("Le journal est vide.");
            }

            rs.close();
            st.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // AFFICHER TOUTE LA BASE DE DONNEES
    // =========================================================
    static void afficherTouteLaBD() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  CONTENU COMPLET DE LA BASE DE DONNEES");
        System.out.println("========================================");

        System.out.println();
        System.out.println(">> TABLE CLIENT :");
        listerClients();

        System.out.println();
        System.out.println(">> TABLE DECODEUR :");
        listerDecodeurs();

        System.out.println();
        System.out.println(">> DECODEURS LIBRES :");
        listerLibres();

        System.out.println();
        System.out.println(">> JOURNAL (10 dernieres entrees) :");
        afficherJournalCourt();
    }

    // Version courte du journal pour l'affichage complet
    static void afficherJournalCourt() {
        String sql = "SELECT ID_JOURNAL, DATE_HEURE, TYPE_OPERATION, TABLE_MODIFIEE, DESCRIPTION "
                + "FROM VUE_JOURNAL WHERE ROWNUM <= 10";
        try {
            Connection conn = ConnexionBD.getConnexion();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                System.out.printf("  [%d] %s | %s sur %s : %s%n",
                        rs.getInt("ID_JOURNAL"),
                        rs.getString("DATE_HEURE"),
                        rs.getString("TYPE_OPERATION"),
                        rs.getString("TABLE_MODIFIEE"),
                        rs.getString("DESCRIPTION"));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // VERIFICATION DE COHERENCE APRES PANNE
    //
    // Apres une panne, Oracle recupere automatiquement via ses
    // REDO LOG et ROLLBACK SEGMENTS. Mais l'admin peut aussi
    // verifier manuellement la coherence de la BD :
    // - Est-ce que tout client a bien un utilisateur associe ?
    // - Est-ce qu'un decodeur est assigne a un client qui existe ?
    // - Quelles etaient les dernieres operations avant la panne ?
    // =========================================================
    static void verifierCoherence() {
        System.out.println();
        System.out.println("--- VERIFICATION DE COHERENCE APRES PANNE ---");
        System.out.println("(Chapitre 14 : recuperation et fiabilite)");
        System.out.println();

        try {
            Connection conn = ConnexionBD.getConnexion();
            Statement st = conn.createStatement();
            ResultSet rs;
            boolean problemeTrouve = false;

            // Verification 1 : clients sans utilisateur associe
            // Si l'appli a plante apres INSERT CLIENT mais avant
            // INSERT UTILISATEUR, on aurait un client orphelin.
            // Avec nos transactions (autoCommit=false + ROLLBACK),
            // ca ne devrait jamais arriver.
            System.out.println("1. Clients sans compte utilisateur :");
            String sql1 = "SELECT C.ID_CLIENT, C.NOM_CLIENT "
                    + "FROM CLIENT C "
                    + "LEFT JOIN UTILISATEUR U ON U.ID_CLIENT = C.ID_CLIENT "
                    + "WHERE U.ID_UTILISATEUR IS NULL";
            rs = st.executeQuery(sql1);
            boolean ok1 = true;
            while (rs.next()) {
                ok1 = false;
                problemeTrouve = true;
                System.out.println("   PROBLEME : client #" + rs.getInt("ID_CLIENT")
                        + " (" + rs.getString("NOM_CLIENT") + ") n'a pas de compte utilisateur !");
            }
            rs.close();
            if (ok1)
                System.out.println("   OK - Aucun probleme detecte.");

            // Verification 2 : decodeurs assignes a un client inexistant
            // Ne devrait pas arriver grace aux contraintes FK,
            // mais on verifie quand meme
            System.out.println();
            System.out.println("2. Decodeurs assignes a un client inexistant :");
            String sql2 = "SELECT D.ID_DECODEUR, D.ADRESSE_IP, D.ID_CLIENT "
                    + "FROM DECODEUR D "
                    + "WHERE D.ID_CLIENT IS NOT NULL "
                    + "AND NOT EXISTS (SELECT 1 FROM CLIENT C WHERE C.ID_CLIENT = D.ID_CLIENT)";
            rs = st.executeQuery(sql2);
            boolean ok2 = true;
            while (rs.next()) {
                ok2 = false;
                problemeTrouve = true;
                System.out.println("   PROBLEME : decodeur #" + rs.getInt("ID_DECODEUR")
                        + " pointe vers un client #" + rs.getInt("ID_CLIENT") + " qui n'existe pas !");
            }
            rs.close();
            if (ok2)
                System.out.println("   OK - Aucun probleme detecte.");

            // Verification 3 : dernieres operations dans le journal
            // Permet de savoir ce qui s'est passe avant la panne
            System.out.println();
            System.out.println("3. Dernieres operations enregistrees avant la panne :");
            String sql3 = "SELECT ID_JOURNAL, DATE_HEURE, TYPE_OPERATION, "
                    + "TABLE_MODIFIEE, DESCRIPTION "
                    + "FROM VUE_JOURNAL WHERE ROWNUM <= 5";
            rs = st.executeQuery(sql3);
            while (rs.next()) {
                System.out.println("   [" + rs.getString("DATE_HEURE") + "] "
                        + rs.getString("TYPE_OPERATION") + " sur "
                        + rs.getString("TABLE_MODIFIEE") + " : "
                        + rs.getString("DESCRIPTION"));
            }
            rs.close();

            System.out.println();
            if (!problemeTrouve) {
                System.out.println("=> Base de donnees coherente.");
                System.out.println("   Les mecanismes de recuperation Oracle (REDO LOG +");
                System.out.println("   ROLLBACK SEGMENTS) et nos transactions ont fonctionne.");
            } else {
                System.out.println("=> Des problemes ont ete detectes.");
                System.out.println("   Consultez le journal pour identifier ce qui s'est passe.");
            }

            st.close();

        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // UTILITAIRE : lire un entier au clavier
    // =========================================================
    static int lireEntier() {
        try {
            String ligne = clavier.nextLine().trim();
            return Integer.parseInt(ligne);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
