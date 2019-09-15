
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;

class CallsBackup {

    static Connection conn = null;
    static Statement st = null;
    static ResultSet rs = null;
    //phoneNumbers (a linked list that stores all the phone numbers) is a data structure that saves the user from
    //having to confirm more than once whether or not to allow the program to create a new contact. Without the
    //phoneNumbers linked list, the program might ask the user multiple times if he/she would like to add a contact
    //to the database (this would happen if more than one message was sent/received from the same contact).
    public static LinkedList<String> phoneNumbers = new LinkedList<>();

    public static void main(String[] args) throws IOException, SQLException {
        // Get the path, replacing common invalid characters such as quotes.
        String path = new MySQLMethods().fixFilePath(args[0]);

        //Read through the new text messages.
        File file = new File(path);
        if (!file.exists()) { //we might not want to add text to a file that already existed
            System.out.println("File does not exist.");
            throw new FileNotFoundException("Path to phone calls XML file does not exist.");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.

            String currLine;  //The line in the file currently being viewed by the program. The xml file is
            //broken up by lines; so, one line represents a single text message.
            System.out.println();
            System.out.println("Getting ready to backup phone calls. Click OK to continue. This may take a few minutes.");

            int beginTimeMillis = (int) System.currentTimeMillis();
            while ((currLine = br.readLine()) != null) {                
                if (!currLine.contains("(Unknown)") && currLine.contains("duration")) {   // Line contains a call, and that call is from a contact.
                    conn = new MySQLMethods().getConnection();
                    if (conn == null) {
                        System.out.println("\nUnable to connect to the database. \nPlease check the connection. \nTry manually starting MySQL server. \nYou may need to delete the aria_log.* file, \nlocated in C:\\xampp\\mysql\\data");
                        return;
                    }
                    try {
                        String phoneNumber = currLine.substring(currLine.indexOf("call number=\"") + 13, currLine.indexOf("\" duration"));
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
                            String query = "SELECT COUNT(*) FROM phone_calls WHERE call_timestamp = '" + new MySQLMethods().createSQLTimestamp(callTimestamp) + "'; ";

                            st = conn.createStatement();
                            rs = st.executeQuery(query);

                            // iterate through the java resultset
                            boolean exists = false;
                            while (rs.next()) {
                                if (!rs.getString(1).equals("0")) {  //The call exists in the database
                                    exists = true;
                                }
                            }

                            if (exists) {  // don't insert a call into the database if it already exists
                                continue;
                            }  // else, go on to insertion
                        } catch (Exception e) {
                            System.err.println("Exception trying to see if the call exists: " + e.getMessage());
                        } finally {
                            new MySQLMethods().closeResultSet(rs);
                            new MySQLMethods().closeStatement(st);
                        }
                        try {  // now try inserting the call into the database
                            Class.forName("com.mysql.jdbc.Driver");

                            // First, check to see that the contact exists in the database.
                            if (!phoneNumberConsidered(phoneNumber)) {
                                new MySQLMethods().handleContact(contactName, phoneNumber);
                            }

                            String sql = "INSERT INTO phone_calls (contact_id, call_timestamp, duration, incoming) VALUES ((SELECT id FROM contacts WHERE person_name = '" + contactName + "'), '" + new MySQLMethods().createSQLTimestamp(callTimestamp) + "', " + duration + ", " + incoming + "); ";

                            PreparedStatement preparedStatement = conn.prepareStatement(sql);
                            preparedStatement.executeUpdate();
                            System.out.println(sql);
                            preparedStatement.close();
                        } catch (SQLException sqle) {
                            System.out.println("SQL Exception: " + sqle);
                        } catch (ClassNotFoundException cnfe) {
                            System.out.println("ClassNotFoundException: " + cnfe);
                        }
                    } catch (Exception ex) {
                        System.out.println("Exception : " + ex);
                    } finally {
                        new MySQLMethods().closeConnection(conn);
                    }
                }
            }
            int endTimeMillis = (int) System.currentTimeMillis();
            System.out.println("Finished backing up phone calls. That took " + secondsFormatted((endTimeMillis - beginTimeMillis)/1000) + ".");

            // Try to update the timestamp for the phone calls backup.
            new MySQLMethods().updateBackup("phone calls");
            
        } catch (IOException ex) {
            System.out.println("IOException : " + ex);
        }
    }

    public static boolean phoneNumberConsidered(String phoneNumber) {
        for (int i = 0; i < phoneNumbers.size(); i++) {
            if (phoneNumbers.get(i).equals(phoneNumber)) {
                return true;
            }
        }

        // We have now considered this phone number. Add it to the list.
        phoneNumbers.add(phoneNumber);
        return false;
    }

    public static String fixForInsertion(String sql) {
        sql = sql.replace("\'", "\\'");
        sql = sql.replace("&amp;", "&");
        return sql;
    }
    
    // secondsFormatted: converts seconds to hours, minutes, and seconds.
    // For example: input 95 seconds would return "1 minute, 35 seconds"
    public static String secondsFormatted(int seconds) {
        if (seconds == 0) {  // Avoid further processing if there are zero seconds.
            return "0 seconds";
        }
        try {
            int minutes = seconds / 60;    // Extract minutes out of the seconds.
            seconds %= 60;               // Make seconds less than 60.
            int hours = minutes / 60;      // Extract hours out of seconds.
            minutes %= 60;               // Make minutes less than 60. 
            
            String hoursString = "";
            String minutesString = "";
            String secondsString = "";

            
            if (hours > 0) {
                if (hours == 1) {
                    hoursString = hours + " hour, ";
                } else {
                    hoursString = hours + " hours, ";
                }
            }
            if (minutes > 0) {
                if (minutes == 1) {
                    minutesString = minutes + " minute, ";
                } else {
                    minutesString = minutes + " minutes, ";
                }
            }
            if (seconds > 0) {
                if (seconds == 1) {
                    secondsString = seconds + " second";
                } else {
                    secondsString = seconds + " seconds";
                }
            }
            String duration = hoursString + minutesString + secondsString;  // Put the entire result in one string so we can check if a comma needs to be removed at the end.
            if (duration.charAt(duration.length() - 2) == ',') {
                return duration.substring(0, duration.length() - 2);
            } else {
                return duration;
            }
        } catch (Exception ex) {
            System.out.println("Exception trying to format total time spent on the phone: " + ex);
        }
        return null;
    }
    
}
