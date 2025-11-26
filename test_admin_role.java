import java.sql.*;
import java.util.UUID;

// Simple test to manually update a user to SUPER_ADMIN role
class TestAdminRole {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/iscm_iam";
        String username = "postgres";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Get the user ID for admin@example.com
            String userId = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM users WHERE email = ?")) {
                stmt.setString(1, "admin@example.com");
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getString("id");
                }
            }

            if (userId != null) {
                // Get SUPER_ADMIN role ID
                String superAdminRoleId = null;
                try (PreparedStatement roleStmt = conn.prepareStatement(
                    "SELECT id FROM roles WHERE name = 'SUPER_ADMIN'")) {
                    ResultSet roleRs = roleStmt.executeQuery();
                    if (roleRs.next()) {
                        superAdminRoleId = roleRs.getString("id");
                    }
                }

                if (superAdminRoleId != null) {
                    // Remove existing USER role assignment
                    try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM user_roles WHERE user_id = ?")) {
                        deleteStmt.setString(1, userId);
                        deleteStmt.executeUpdate();
                    }

                    // Insert SUPER_ADMIN role assignment
                    try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by, tenant_id) VALUES (?, ?, NOW(), ?, ?)")) {
                        insertStmt.setString(1, userId);
                        insertStmt.setString(2, superAdminRoleId);
                        insertStmt.setString(3, userId);
                        insertStmt.executeUpdate();
                    }

                    System.out.println("Successfully updated user " + userId + " to SUPER_ADMIN role");
                } else {
                    System.out.println("SUPER_ADMIN role not found in database");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}