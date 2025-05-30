
public class TestConnection {
    public static void main(String[] args) {
        try {
            var conn = DBConnection.getConnection();
            System.out.println("✅ Connexion à la base réussie !");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }
}