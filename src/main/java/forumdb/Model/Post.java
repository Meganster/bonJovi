package forumdb.Model;

import java.sql.Timestamp;


public class Post {
    private String author;
    private String created;
    private String forum;
    private Long id;
    private Boolean isEdited;
    private String message;
    private Long parent;
    private Long thread;
    private Long forum_id;
    private Object[] path;

    public Long getId() {
        return id;
    }

    public Long getParent() {
        return parent;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public Boolean getIsEdited() {
        return isEdited;
    }

    public String getForum() {
        return forum;
    }

    public Long getThread() {
        return thread;
    }

    public String getCreated() {
        return created;
    }

    public Long getForumID() {
        return forum_id;
    }


    public void setAuthor(String author) {
        this.author = author;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIsEdited(Boolean edited) {
        isEdited = edited;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public void setThread(Long thread) {
        this.thread = thread;
    }

    public void setCreated(Timestamp created) {
        this.created = created.toInstant().toString();
    }

    public void setForumID(Long forum_id) {
        this.forum_id = forum_id;
    }


    public Object[] getPath() {
        return path;
    }

    public void setPath(Object[] path) {
        this.path = path;
    }
}
