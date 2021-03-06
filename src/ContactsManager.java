
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/*
This class holds the contacts from the database as well as those contained in the
SMS and Phone Calls XML files.

There are methods for handling contacts that already exist and contacts that don't 
already exist.
 */
public class ContactsManager {

    private ConcurrentHashMap databaseContacts;
    private ConcurrentHashMap xmlFileContacts;
    private ConcurrentHashMap duplicates;

    public ContactsManager() {
        databaseContacts = new MySQLMethods().getContacts();
        xmlFileContacts = new ConcurrentHashMap();
        duplicates = new ConcurrentHashMap();
    }

    public void handleContact(Contact c) {
        String key = c.getPhoneNumber() + c.getPersonName();
        xmlFileContacts.put(key, c);
        if (databaseContacts.containsKey(key)) { // This contact is already in the database. Add it to the duplicates.
            duplicates.put(key, c);
        }
    }

    public void showContacts() {
        // Strip away duplicates so that we can see the different contact values between the database and the XML file. 
        Set<Map.Entry<String, Contact>> entrySet3 = databaseContacts.entrySet();
        for (Map.Entry<String, Contact> entry3 : entrySet3) {
            System.out.println(entry3.getValue().getPersonName());
            System.out.println(entry3.getValue().getPhoneNumber() + "\n\n");
        }
    }

    // Below method removes the duplicated contacts that are in the xmlFileContacts
    // and databaseContacts (if a contact appears in both, then it is removed from
    // both).
    public void removeDuplicates() {
        // Strip away duplicates so that we can see the different contact values between the database and the XML file. 
        Set<Map.Entry<String, Contact>> entrySet3 = duplicates.entrySet();
        for (Map.Entry<String, Contact> entry3 : entrySet3) {
            xmlFileContacts.remove(entry3.getKey());
            databaseContacts.remove(entry3.getKey());
        }
    }

    // This method inserts any new contacts into the database.
    public void updateDatabase() {
        // Remove the duplicate contacts from both XML and Database maps.
        removeDuplicates();
        // Now, update any contacts that have had a name change.
        updateContacts();

        try {
            // Before inserting any text messages, add any new contacts discovered in the XML file to the database.
            if (xmlFileContacts.size() > 0) {
                Set<Map.Entry<String, Contact>> entrySet = xmlFileContacts.entrySet();
                String sql = "INSERT INTO contacts (phone_number, person_name) VALUES ";
                for (Map.Entry<String, Contact> entry : entrySet) {
                    // Create the multiple insert string.
                    // (Using a batch insert instead of creating a single
                    // insert for each contact will improve performance.)
                    sql += "(" + entry.getValue().getPhoneNumber() + ", '" + entry.getValue().getPersonName() + "'), ";
                }

                if (sql.contains("'")) { // The sql statement has something to insert.
                    // Chop of the last comma, replace it with a semicolon.
                    sql = sql.substring(0, sql.lastIndexOf(",")) + ";";

                    new MySQLMethods().executeSQL(sql);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception trying to create the multiple inserts for contacts: " + e);
        }
    }

    public void updateContacts() {
        // Now, see if any of the "new" contacts are old contacts that were given a new name.
        Set<Map.Entry<String, Contact>> entrySet4 = databaseContacts.entrySet();
        for (Map.Entry<String, Contact> entry4 : entrySet4) {
            // If we find a contact with same phone number but different name in xmlFileContacts, then we just need to update the contact name.
            Set<Map.Entry<String, Contact>> entrySet5 = xmlFileContacts.entrySet();
            for (Map.Entry<String, Contact> entry5 : entrySet5) {
                if (entry5.getValue().getPhoneNumber() == entry4.getValue().getPhoneNumber()) {
                    new MySQLMethods().updateContactName(entry5.getValue());
                    xmlFileContacts.remove(entry5.getKey());
                }
            }
        }
    }

    public Map<String, Contact> getDatabaseContacts() {
        return databaseContacts;
    }

    public Map<String, Contact> getXmlFileContacts() {
        return xmlFileContacts;
    }

    public Map<String, Contact> getDuplicates() {
        return duplicates;
    }

}
