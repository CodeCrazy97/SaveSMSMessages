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
        File file = new File("C:\\Users\\Ethan_2\\Documents\\Projects\\Java\\SMS\\calls-20190525094728.xml");
        if (!file.exists()) { //we might not want to add text to a file that already existed
            System.out.println("File does not exist.");
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
            
            System.out.println("About to start backing up your phone calls.");
            System.out.println("This program will NOT remove any phone calls already in the database.");
            System.out.println("This may take a few minutes.\n");
            
            while ((currLine = br.readLine()) != null) {
                if (!currLine.contains("(Unknown)") && currLine.contains("duration")) {   // Line contains a call, and that call is from a contact.
                    //create the connection to the database
                    Connection conn = new MySQLConnection().getConnection();

                    try {
                        String duration = currLine.substring(currLine.indexOf("duration=\"") + 10, currLine.indexOf("\" date="));
                        String callTimestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.indexOf("\" contact_name="));
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
                            String query = "SELECT COUNT(*) FROM phonecalls WHERE call_timestamp = '" + createSQLTimestamp(callTimestamp) + "'; ";
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
                            System.err.println("Exception trying to see if the call exists: " + e.getMessage());
                        }
                        try {  // now try inserting the call into the database
                            Class.forName("com.mysql.jdbc.Driver");

                            String sql = "INSERT INTO phonecalls (contactname, call_timestamp, duration, incoming) VALUES ('" + contactName + "', '" + createSQLTimestamp(callTimestamp) + "', " + duration + ", " + incoming + "); ";

                            PreparedStatement preparedStatement = conn.prepareStatement(sql);
                            preparedStatement.executeUpdate();

                            System.out.println("SQL insert statement: " + sql);
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

    // createSQLTimestamp: creates an SQL timestamp datatype.
    // timestamp: a string date time of the form "Apr 12, 2017 4:01:03 PM"
    // The return value for the above input would be 2017-04-12 16:01:03
    public static String createSQLTimestamp(String timestamp) throws Exception {
        String fixedTimestamp = "";
        // Get year.
        int indexOfComma = timestamp.indexOf(",");
        fixedTimestamp = timestamp.substring(indexOfComma + 2, indexOfComma + 6) + "-";

        // Get the month.
        switch (timestamp.substring(0, 3)) {
            case "Jan":
                fixedTimestamp += "01-";
                break;
            case "Feb":
                fixedTimestamp += "02-";
                break;
            case "Mar":
                fixedTimestamp += "03-";
                break;
            case "Apr":
                fixedTimestamp += "04-";
                break;
            case "May":
                fixedTimestamp += "05-";
                break;
            case "Jun":
                fixedTimestamp += "06-";
                break;
            case "Jul":
                fixedTimestamp += "07-";
                break;
            case "Aug":
                fixedTimestamp += "08-";
                break;
            case "Sep":
                fixedTimestamp += "09-";
                break;
            case "Oct":
                fixedTimestamp += "10-";
                break;
            case "Nov":
                fixedTimestamp += "11-";
                break;
            case "Dec":
                fixedTimestamp += "12-";
                break;
            default:
                throw new Exception("Something is wrong with the month!!!");
        }

        // Get the day.
        fixedTimestamp += timestamp.substring(timestamp.indexOf(" ") + 1, indexOfComma) + " ";

        // Get the time.
        // First, find out if it was morning (AM) or evening (PM).
        int hour = Integer.parseInt(timestamp.substring(indexOfComma + 7, timestamp.indexOf(":")));
        if (timestamp.substring(timestamp.length() - 2).equals("PM")) {
            if (hour == 12) {  // Don't add 12 - it is the afternoon, but the hour is 12 o'clock
                fixedTimestamp += hour;
            } else {
                fixedTimestamp += hour + 12;
            }
        } else if (hour == 12) { // Subtract twelve from the hour. The hour is midnight.
            fixedTimestamp += hour - 12;
        } else {
            fixedTimestamp += hour;
        }
        // Get minutes and seconds.
        fixedTimestamp += timestamp.substring(timestamp.indexOf(":"), timestamp.lastIndexOf(" "));

        return fixedTimestamp;
    }

    public static String fixForInsertion(String sql) {
        sql = sql.replace("&amp;", "&");
        return sql;
    }
}
