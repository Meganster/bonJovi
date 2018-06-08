package forumdb.Model;


public class InfoAboutDB {
    int forum;
    int post;
    int thread;
    int user;

    public InfoAboutDB() {
        user = 0;
        forum = 0;
        post = 0;
        thread = 0;
    }

    public int getForum() {
        return forum;
    }

    public int getUser() {
        return user;
    }

    public int getPost() {
        return post;
    }

    public int getThread() {
        return thread;
    }


    public void setForum(int countForums) {
        this.forum = countForums;
    }

    public void setPost(int countPosts) {
        this.post = countPosts;
    }

    public void setThread(int countThreads) {
        this.thread = countThreads;
    }

    public void setUser(int countUsers) {
        this.user = countUsers;
    }
}