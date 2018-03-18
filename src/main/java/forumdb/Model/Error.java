package forumdb.Model;

public class Error {
    private String message;


    public Error() {

    }

    public Error(String message) {
        this.message = message;
    }

    public Error(Error Error) {
        this.message = Error.message;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }
}
