package forumdb.Model;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class User {
    private Integer id;
    private String about;
    private String email;
    private String fullname;
    private String nickname;

    public User(String nickname, String fullname, String about, String email) {
        this.nickname = nickname;
        this.fullname = fullname;
        this.about = about;
        this.email = email;
    }

    public User(User user) {
        this.nickname = user.nickname;
        this.fullname = user.fullname;
        this.about = user.about;
        this.email = user.email;
    }

    public User(){

    }

    public Integer getId(){
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getFullname() {
        return fullname;
    }

    public String getAbout() {
        return about;
    }

    public String getEmail() {
        return email;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ObjectNode toObjectNode(ObjectMapper mapper) {
        final ObjectNode result = mapper.createObjectNode();
        result.put("about", about);
        result.put("email", email);
        result.put("fullname", fullname);
        result.put("nickname", nickname);
        result.put("id", id);

        return result;
    }
}
