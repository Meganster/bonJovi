package forumdb.Model;


import java.sql.Timestamp;

public class Thread {
    private String author;
    private String created;
    private String forum;
    private Long id;
    private String message;
    private String title;
    private String slug;
    private Long votes;
    private Long forum_id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getForum() {
        return forum;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getVotes() {
        return votes;
    }

    public void setVotes(Long votes) {
        this.votes = votes;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created.toInstant().toString();
    }

    public Long getForumID() {
        return forum_id;
    }

    public void setForumID(Long forum_id) {
        this.forum_id = forum_id;
    }
}