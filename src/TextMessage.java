
public class TextMessage {

    private String messageText;
    private String timestamp;
    private int senderPhoneNumber;
    private String senderName;

    public TextMessage(String messageText, String timestamp, int senderPhoneNumber, String senderName) {
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.senderPhoneNumber = senderPhoneNumber;
        this.senderName = senderName;
    }

    public int getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public void setSenderPhoneNumber(int senderPhoneNumber) {
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
