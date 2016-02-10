package au.csiro.data61.docktimizer.hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 */
public class EntityManagerHelper {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(EntityManagerHelper.class);


    private static String jdbcDriver = "org.mariadb.jdbc.Driver";

    private static EntityManagerHelper instance;
    private EntityManager entityManager;

    public static EntityManagerHelper getInstance() {
        if (instance == null) {
            instance = new EntityManagerHelper();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        Properties properties = new Properties();
        Properties prop = new Properties();
        try {

            String mysqlPropertyFile = System.getenv("MYSQL_PROPERTY_FILE");
            if (mysqlPropertyFile != null) {
                prop.load(new FileInputStream(mysqlPropertyFile));
            } else {

                prop.load(instance.getClass().getClassLoader().
                        getResourceAsStream("mysql-config/mysql.properties"));
            }
            String mysql_tcp_addr = System.getenv("MYSQL_PORT_3306_TCP");

            if (mysql_tcp_addr == null || mysql_tcp_addr.isEmpty()) {
                LOG.info("environment variable $MYSQL_PORT_3306_TCP not set");
                String mysqlTcpAddr = prop.getProperty("MYSQL_TCP_ADDR");
                if (mysqlTcpAddr.startsWith("$")) {
                    mysqlTcpAddr = System.getenv(mysqlTcpAddr.replace("$", ""));
                }
                String mysqlTcpPort = prop.getProperty("MYSQL_TCP_PORT");
                if (mysqlTcpPort.startsWith("$")) {
                    mysqlTcpPort = System.getenv(mysqlTcpPort.replace("$", ""));
                }
                mysql_tcp_addr = mysqlTcpAddr + ":" + mysqlTcpPort;
            } else {
                mysql_tcp_addr = mysql_tcp_addr.replaceAll("tcp://", "");
                LOG.info("environment variable $MYSQL_PORT_3306_TCP set : " + mysql_tcp_addr);
            }


            String mysqlUserName = prop.getProperty("MYSQL_USER_NAME");
            String mysqlUserPassword = prop.getProperty("MYSQL_USER_PASSWORD");
            String mysqlDatabaseName = prop.getProperty("MYSQL_DATABASE_NAME");


            LOG.info(mysql_tcp_addr);
            LOG.info(mysqlUserName);
            LOG.info(mysqlUserPassword);
            LOG.info(mysqlDatabaseName);

            String value = "jdbc:mysql://" + mysql_tcp_addr + "/" + mysqlDatabaseName;
            LOG.info(value);

            properties.put("javax.persistence.jdbc.url", value);
            properties.put("javax.persistence.jdbc.user", mysqlUserName);
            properties.put("javax.persistence.jdbc.password", mysqlUserPassword);

            try {
                Class.forName(jdbcDriver);

                Connection conn = DriverManager.getConnection("jdbc:mysql://" + mysql_tcp_addr + "/?user=" + mysqlUserName + "" +
                        "&password=" + mysqlUserPassword);
                Statement s = conn.createStatement();
                int Result = s.executeUpdate("CREATE DATABASE IF NOT EXISTS " + mysqlDatabaseName + ";");
            } catch (Exception e) {
                e.printStackTrace();
            }

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("dockerPU", properties);
            entityManager = emf.createEntityManager();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized EntityManager getEntityManager() {
        return entityManager;
    }
}
