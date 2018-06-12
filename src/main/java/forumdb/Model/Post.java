package forumdb.Model;

import java.sql.Timestamp;


public class Post {
    private String author;
    private String created;
    private String forum;
    private Integer id;
    private Boolean isEdited;
    private String message;
    private Integer parent;
    private Integer thread;
    private Integer forum_id;
    private Object[] path;

    public Integer getId() {
        return id;
    }

    public Integer getParent() {
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

    public Integer getThread() {
        return thread;
    }

    public String getCreated() {
        return created;
    }

    public Integer getForumID() {
        return forum_id;
    }


    public void setAuthor(String author) {
        this.author = author;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setIsEdited(Boolean edited) {
        isEdited = edited;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public void setThread(Integer thread) {
        this.thread = thread;
    }

    public void setCreated(Timestamp created) {
        this.created = created.toInstant().toString();
    }

    public void setForumID(Integer forum_id) {
        this.forum_id = forum_id;
    }


    public Object[] getPath() {
        return path;
    }

    public void setPath(Object[] path) {
        this.path = path;
    }
}
