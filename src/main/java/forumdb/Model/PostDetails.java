package forumdb.Model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class PostDetails {
    private User author;
    private Forum forum;
    private Post post;
    private Thread thread;

    @JsonCreator
    public PostDetails(
            @JsonProperty("author") User author,
            @JsonProperty("forum") Forum forum,
            @JsonProperty("post") Post post,
            @JsonProperty("thread") Thread thread) {
        this.author = author;
        this.forum = forum;
        this.post = post;
        this.thread = thread;
    }

    @JsonCreator
    public PostDetails(@JsonProperty("post") Post post) {
        this.post = post;
        this.author = null;
        this.forum = null;
        this.thread = null;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setForum(Forum forum) {
        this.forum = forum;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public User getAuthor() {
        return author;
    }

    public Forum getForum() {
        return forum;
    }

    public Post getPost() {
        return post;
    }

    public Thread getThread() {
        return thread;
    }
}
