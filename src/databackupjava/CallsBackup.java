package databackupjava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.Scanner;

class CallsBackup {

    public static void main(String[] args) throws IOException, SQLException {

        //Read through the new text messages.
        Scanner keyboard = new Scanner(System.in);
        File file = new File("C:\\Users\\Ethan_2\\Documents\\Projects\\Java\\SMS\\SMS\\calls-20190516103355.xml");
        if (!file.exists()) { //we might not want to add text to a file that already existed
            System.out.println("File does not exist.");
            System.exit(0);
        }

        Scanner input = new Scanner(System.in);
        System.out.println("Please confirm that you would like to backup your calls (y/n)");
        String response = input.next();
        if (response.equals("n") || response.equals("N")) {
            System.out.println("Exiting the program. Nothing was backed up.");
            System.exit(0);
        }

        //phoneNumbers (a linked list that stores all the phone numbers) is a data structure that saves the user from
        //having to confirm more than once whether or not to allow the program to create a new contact. Without the
        //phoneNumbers linked list, the program might ask the user multiple times if he/she would like to add a contact
        //to the database (this would happen if more than one message was sent/received from the same contact).
        LinkedList< String> phoneNumbers = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.

            String currLine;  //The line in the file currently being viewed by the program. The xml file is
            //broken up by lines; so, one line represents a single text message.

            while ((currLine = br.readLine()) != null) {
                if (!currLine.contains("(Unknown)") && currLine.contains("duration")) {   // Line contains a call, and that call is from a contact.
                    //create the connection to the database
                    Connection conn = DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/sms", "root", "");
                    try {

                        String duration = currLine.substring(currLine.indexOf("duration=\"") + 10, currLine.indexOf("\" date="));
                        String date = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.indexOf("\" contact_name="));
                        String contactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.indexOf("\" />"));
                        contactName = fixForInsertion(contactName);
                        String incomingInt = currLine.substring(currLine.indexOf("type=\"") + 6, currLine.indexOf("\" presentation"));
                        int incoming = 1;  // 1 = true, 0 = false
                        if (incomingInt.equals("2")) {  // I dialed the number.
                            incoming = 0;
                        }
                        String myDriver = "org.gjt.mm.mysql.Driver";

                        Class.forName(myDriver);

                        try {  // check that the call does not already xist in the database
                            String query = "SELECT COUNT(*) FROM phonecalls WHERE calldate = '" + date + "'; ";
                            System.out.println("query: " + query);
                            // create the java statement
                            Statement st = conn.createStatement();

                            // execute the query, and get a java resultset
                            ResultSet rs = st.executeQuery(query);

                            // iterate through the java resultset
                            boolean exists = false;
                            while (rs.next()) {
                                if (!rs.getString(1).equals("0")) {  //The call exists in the database
                                    exists = true;
                                }
                            }
                            rs.close();
                            st.close();
                            if (exists) {  // don't insert a call into the database if it already exists
                                continue;
                            }  // else, go on to insertion
                        } catch (Exception e) {
                            System.err.println("Got an exception! ");
                            System.err.println(e.getMessage());
                        }
                        try {  // now try inserting the call into the database
                            Class.forName("com.mysql.jdbc.Driver");

                            String sql = "INSERT INTO phonecalls (contactname, calldate, duration, incoming) VALUES ('" + contactName + "', '" + date + "', " + duration + ", " + incoming + "); ";
                            System.out.println("SQL insert statement: " + sql);
                            PreparedStatement preparedStatement = conn.prepareStatement(sql);
                            preparedStatement.executeUpdate();

                        } catch (SQLException sqle) {
                            System.out.println("SQL Exception: " + sqle);
                        } catch (ClassNotFoundException cnfe) {
                            System.out.println("ClassNotFoundException: " + cnfe);
                        }
                    } catch (Exception ex) {
                        System.out.println("Exception : " + ex);
                    } finally {
                        conn.close();
                    }
                }
            }
        } catch (SQLException ex) {
            System.out.println(" SQL Exception : " + ex);
        } catch (IOException ex) {
            System.out.println("IOException : " + ex);
        }
    }

    public static String fixForInsertion(String sql) {
        sql = sql.replace("&amp;", "&");
        return sql;
    }
}
