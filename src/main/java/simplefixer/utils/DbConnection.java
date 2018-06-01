package simplefixer.utils;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import simplefixer.constant.Constants;

public class DbConnection implements AutoCloseable {
    private final Driver driver;

    public DbConnection(String uri) {
        Config noSSL = Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig();
        driver = GraphDatabase.driver(uri, AuthTokens.basic(Constants.USERNAME, Constants.PASSWORD), noSSL);
    }

    public DbConnection(String uri, String username, String password) {
        Config noSSL = Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig();
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), noSSL);
    }

    public Driver getDriver() {
        return driver;
    }

    @Override
    public void close() throws Exception {
        System.out.println("Closing database connection");
        driver.close();
    }
}
