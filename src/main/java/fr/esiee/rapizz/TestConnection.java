package fr.esiee.rapizz;

import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        try {
            try (var _ = DBConnection.getConnection()) {
                System.out.println("✅ Connexion à la base réussie !");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }
}