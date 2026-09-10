package sigurdws.NB69.house;

public interface EmailSender {
    boolean ready();
    String send(String email, String subject, String message, String key) throws Exception;
}
