package com.quantumventures;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Credentials loader for Quantum Ventures. This should be the only class that loads credentials common to all Quantum Ventures projects.
 * Other classes for narrower scopes should extend this class.
 * Security and privacy based on Azure KeyVault and Azure Active Directory.
 *
 * @author Eduardo Alves
 * @version 1.0
 */
public class Credentials {

    // Constants

    protected static final String keyVaultName = "quantumkeys";
    protected String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";

    // Instance Fields

    protected SecretClient secretClient;
    private static Credentials credentials;

    // Variables and Fields

    private String pdb_connection_string;
    private final String phoenix_us_storage_connection_string;

    // Constructors

    public Credentials() {
        // Loads Credentials from Azure KeyVault
        getQuantumSecretClient();

        // Quantum DB Connection String
        KeyVaultSecret pdb_server = secretClient.getSecret("QuantumDB-Server");
        KeyVaultSecret pdb_database = secretClient.getSecret("QuantumDB-Database");
        KeyVaultSecret pdb_username = secretClient.getSecret("QuantumDB-username");
        KeyVaultSecret pdb_password = secretClient.getSecret("QuantumDB-password");
        this.pdb_connection_string =
                "jdbc:sqlserver://" + pdb_server.getValue() + ":1433;database=" + pdb_database.getValue() + ";user=" + pdb_username.getValue() + "@quantum;password=" + pdb_password.getValue() +
                        ";encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;MultiSubnetFailover=True";

        // Azure Blob Storage Credentials
        phoenix_us_storage_connection_string = secretClient.getSecret("storage-phoenixus-connection-string").getValue();
    }

    // Getters and Setters

    // Methods

    /**
     * Returns a singleton instance of the Azure Key Vault SecretClient class with the proper credentials.
     *
     * @see SecretClient
     */
    protected void getQuantumSecretClient() {
        if (secretClient == null) {
            secretClient = new SecretClientBuilder()
                    .vaultUrl(keyVaultUri)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        }
    }

    public Connection getQuantumDBConnection() throws SQLException {
        try {
            return DriverManager.getConnection(this.pdb_connection_string);
        } catch (SQLException e) {
            throw new SQLException("Error connecting to Quantum DB.", e);
        }
    }

    public Connection getQuantumDBConnection(boolean verbose) throws SQLException {
        if (verbose) {
            System.out.println("Connecting to Quantum DB...");
        }
        Connection pdb_connection = this.getQuantumDBConnection();
        if(verbose) {
            System.out.println("Connected to Quantum DB.");
        }
        return pdb_connection;
    }

    public Connection getQuantumDBConnection(boolean verbose, boolean read_only) throws SQLException {
        if (!read_only) {
            return this.getQuantumDBConnection(verbose);
        }
        if (verbose) {
            System.out.println("Connecting to Quantum DB (read-only)...");
        }
        this.pdb_connection_string += ";applicationIntent=ReadOnly";
        Connection pdb_connection = this.getQuantumDBConnection();
        if (verbose) {
            System.out.println("Connected to Quantum DB (read-only).");
        }
        return pdb_connection;
    }

    public String getPhoenixUSStorageConnectionString() {
        return phoenix_us_storage_connection_string;
    }

    /**
     * Returns a singleton instance of the Credentials class.
     * @return Credentials
     * @see Credentials
     */
    public static Credentials getInstance() {
        if (credentials == null) {
            credentials = new Credentials();
        }
        return credentials;
    }
}
