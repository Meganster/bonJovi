package forumdb.Controller;


import forumdb.DAO.ForumDAO;
import forumdb.DAO.ThreadDAO;
import forumdb.DAO.UserDAO;
import forumdb.Model.Forum;
import forumdb.Model.Thread;
import forumdb.Model.User;
import forumdb.Model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;


@RestController
public class ThreadController {
    @Autowired
    ThreadDAO threadTemplate;
    @Autowired
    ForumDAO forumTemplate;
    @Autowired
    UserDAO userTemplate;

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @PostMapping(value = "/api/forum/{slug}/create")
    public Thread createThread(@PathVariable("slug") String slug, @RequestBody Thread thread, HttpServletResponse response){
        User user;
        Forum forum;

        try{
            forum = forumTemplate.getForum(slug);
            user = userTemplate.getUser(thread.getAuthor());
        }catch (DataAccessException error){
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        try {
            thread.setForum(forum.getSlug());
            threadTemplate.createThread(thread);
            response.setStatus(HttpServletResponse.SC_CREATED);
            forumTemplate.upNumberOfThreads(forum.getSlug());
            return threadTemplate.getThread(thread.getAuthor(), slug, thread.getTitle());
        } catch (DataAccessException error) {
            Thread existThread = threadTemplate.getThread(thread.getSlug());
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return existThread;
        }
    }

    @PostMapping(value = "/api/thread/{slug_or_id}/vote")
    public Thread voteForThread(@PathVariable("slug_or_id") String slugOrId, @RequestBody Vote vote, HttpServletResponse response) {

        Thread thread = threadTemplate.getThreadSlugOrId(slugOrId);
        if (thread == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        try {
            User user = userTemplate.getUser(vote.getNickname());
            Integer voteStatus = threadTemplate.getVote(user.getId(), thread.getId());
            if(voteStatus == null) {
                voteStatus = 0;
            }

            switch(voteStatus) {
                case 0: switch (vote.getVoice()) {
                            case 1:
                                threadTemplate.vote(thread.getId(), user.getId(),1, voteStatus);
                                break;
                            case -1:
                                threadTemplate.vote(thread.getId(), user.getId(),-1, voteStatus);
                                break;
                        }
                        break;

                case 1: if(vote.getVoice() == -1){
                            threadTemplate.vote(thread.getId(), user.getId(),-2, voteStatus);
                        }
                        break;
                case -1: if(vote.getVoice() == 1){
                            threadTemplate.vote(thread.getId(), user.getId(),2, voteStatus);
                         }
                         break;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            return threadTemplate.getThread(thread.getSlug());

        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    @GetMapping(value = "api/thread/{slug_or_id}/details")
    public Thread getDetails(@PathVariable("slug_or_id") String slugOrId, HttpServletResponse response) {
        Thread thread = threadTemplate.getThreadSlugOrId(slugOrId);
        if (thread == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } else{
            response.setStatus(HttpServletResponse.SC_OK);
            return thread;
        }
    }

    @PostMapping(value = "api/thread/{slug_or_id}/details")
    public Thread updateThread(@PathVariable("slug_or_id") String slugOrId, @RequestBody Thread changedThread, HttpServletResponse response) {
        Thread thread = threadTemplate.getThreadSlugOrId(slugOrId);
        if (thread == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        if(thread.getTitle() == changedThread.getTitle() &&
                thread.getMessage() == changedThread.getMessage())
            return thread;

        threadTemplate.update(thread.getId(), changedThread);
        return threadTemplate.getThreadID(thread.getId());
    }
}
