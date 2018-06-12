package forumdb.Model;


public class InfoAboutDB {
    Long forum;
    Long post;
    Long thread;
    Long user;

    public InfoAboutDB() {
        user = 0L;
        forum = 0L;
        post = 0L;
        thread = 0L;
    }

    public Long getForum() {
        return forum;
    }

    public Long getUser() {
        return user;
    }

    public Long getPost() {
        return post;
    }

    public Long getThread() {
        return thread;
    }


    public void setForum(Long countForums) {
        this.forum = countForums;
    }

    public void setPost(Long countPosts) {
        this.post = countPosts;
    }

    public void setThread(Long countThreads) {
        this.thread = countThreads;
    }

    public void setUser(Long countUsers) {
        this.user = countUsers;
    }
}