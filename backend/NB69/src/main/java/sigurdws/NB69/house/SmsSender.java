package sigurdws.NB69.house;

public interface SmsSender {
    boolean ready();
    String send(String phone, String message) throws Exception;
}
