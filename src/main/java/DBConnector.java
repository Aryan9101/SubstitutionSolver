import org.ini4j.Ini;
import utils.Util;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;

public class DBConnector {
    private Connection connection;

    public DBConnector() throws ClassNotFoundException, IOException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");

        /*
        I have chosen to omit db_info.ini from my GitHub repo because I would rather not have my MySQL root password on
        display in public.
         */
        Ini ini = new Ini(new File("db_info.ini"));
        String user = ini.get("DEFAULT", "USER");
        String password = ini.get("DEFAULT", "PASSWORD");
        String host = ini.get("DEFAULT", "HOST");
        String port = ini.get("DEFAULT", "PORT");
        String jdbcURL = String.format("jdbc:mysql://%s:%s/subsolver", host, port);

        try {
            connection = DriverManager.getConnection(jdbcURL, user, password);
            System.out.println("Connection successful!");
        } catch (SQLException err) {
            System.out.println("Connection to database failed!");
        }

        String created = ini.get("DEFAULT", "CREATED");
        if (!Boolean.parseBoolean(created)){
            createTable("word_patterns", "word_list.txt");
            createTable("word_patterns_mini", "word_list_mini.txt");
        }
    }

    public void createTable(String table, String wordFileName) throws IOException, SQLException {
        String query;
        try{
            query = String.format("CREATE TABLE %s(word VARCHAR(1023), pattern VARCHAR(1023))", table);
            connection.createStatement().executeUpdate(query);
        } catch (SQLException err) {
            System.out.println("Table word_patterns_mini already exists!");
        }

        System.out.println("Creating table...");
        BufferedReader br = new BufferedReader(new FileReader(wordFileName));
        String word = br.readLine();
        while (word != null){
            if (Util.isAlphabetic(word.toUpperCase())) {
                String pattern = Util.getPattern(word);
                query = String.format("INSERT INTO %s(word, pattern) VALUE ('%s', '%s')", table, word, pattern);
                connection.createStatement().executeUpdate(query);
            }
            word = br.readLine();
        }
        System.out.println("Table created!");
        br.close();
    }

    public int getCount(String table) throws SQLException {
        String query = String.format("SELECT COUNT(*) FROM %s", table);
        ResultSet result = connection.prepareStatement(query).executeQuery();
        result.next();
        return result.getInt(1);
    }

    public ArrayList<String> getWordsFromPattern(String pattern, String table) throws SQLException {
        String query = String.format("SELECT word FROM %s WHERE pattern = '%s'", table, pattern);
        ResultSet result = connection.prepareStatement(query).executeQuery();
        ArrayList<String> words = new ArrayList<>();
        while (result.next()) {
            words.add(result.getString("word"));
        }
        return words;
    }

    public void disconnect() throws SQLException {
        if (connection != null){
            connection.close();
            connection = null;
        }
        System.out.println("Connection to MySQL disconnected!");
    }
}
