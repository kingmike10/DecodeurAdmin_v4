import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Classe pour gerer la connexion a la base de donnees Oracle
// On garde une seule connexion ouverte pendant toute l'execution
public class ConnexionBD {

    private static final String URL = "jdbc:oracle:thin:@//gaia.emp.uqtr.ca:1521/coursbd.uqtr.ca";
    private static final String USER = "SMI1002_056";
    private static final String PASSWORD = "83crpu64";

    private static Connection conn = null;

    // Retourne la connexion. Si elle n'existe pas encore, on la cree.
    public static Connection getConnexion() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
            } catch (ClassNotFoundException e) {
                System.out.println("Erreur : driver Oracle introuvable.");
                System.out.println("Verifiez que ojdbc8.jar est dans le classpath.");
                System.exit(1);
            }

            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            // Gestion des transactions :
            // On desactive l'auto-commit pour gerer manuellement
            // les COMMIT et ROLLBACK dans notre code
            conn.setAutoCommit(false);

            // Niveau d'isolation :
            // On utilise READ COMMITTED.
            // Cela empeche les lectures impropres (dirty reads) :
            // si un autre poste est en train de modifier un decodeur
            // sans avoir fait COMMIT, on ne verra pas ses changements.
            // C'est le niveau par defaut d'Oracle mais on le met
            // explicitement pour montrer qu'on en est conscient.
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
        return conn;
    }

    // Fermer la connexion proprement a la fin du programme
    public static void fermer() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Connexion fermee.");
            } catch (SQLException e) {
                System.out.println("Erreur lors de la fermeture : " + e.getMessage());
            }
        }
    }
}
