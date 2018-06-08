package forumdb.Model;

public class Forum {
    private String title;
    private String user;
    private String slug;
    private Integer posts;
    private Integer threads;

    /*//full constructor
    public Forum(String title, String user, String slug, Integer posts, Integer threads) {
        this.title = title;
        this.user = user;
        this.slug = slug;
        this.posts = posts;
        this.threads = threads;
    }

    //min constructor
    public Forum(String title, String user, String slug) {
        this.title = title;
        this.user = user;
        this.slug = slug;
    }*/

    //simple constructor
    public Forum() {
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

    public void setPosts(Integer posts) {
        this.posts = posts;
    }

    public void setThreads(Integer threads) {
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

    public Integer getPosts() {
        return posts;
    }

    public Integer getThreads() {
        return threads;
    }
}
