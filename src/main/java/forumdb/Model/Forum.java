package forumdb.Model;

public class Forum {
    private Long id;
    private String title;
    private String user;
    private String slug;
    private Long posts;
    private Long threads;

    //simple constructor
    public Forum() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setPosts(Long posts) {
        this.posts = posts;
    }

    public void setThreads(Long threads) {
        this.threads = threads;
    }


    public String getTitle() {
        return title;
    }

    public String getUser() {
        return user;
    }

    public String getSlug() {
        return slug;
    }

    public Long getPosts() {
        return posts;
    }

    public Long getThreads() {
        return threads;
    }
}
