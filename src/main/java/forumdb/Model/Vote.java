package forumdb.Model;


public class Vote {
    private String nickname;
    private Integer voice;


    public String getNickname() {
        return nickname;
    }

    public Integer getVoice() {
        return voice;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setVoice(int voice) {
        this.voice = voice;
    }
}
