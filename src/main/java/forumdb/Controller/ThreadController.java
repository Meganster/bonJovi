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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class ThreadController {
    @Autowired
    ThreadDAO threadService;
    @Autowired
    ForumDAO forumService;
    @Autowired
    UserDAO userService;

    @PostMapping(value = "/api/forum/{slug}/create")
    public ResponseEntity<?> createThread(@PathVariable("slug") String slug, @RequestBody Thread thread) {
        Forum forum;

        try {
            forum = forumService.getForum(slug);
            final User user = userService.getUser(thread.getAuthor());
        } catch (DataAccessException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find thread author by nickname: " + thread.getAuthor()));
        }

        try {
            thread.setForum(forum.getSlug());
            threadService.createThread(thread);
            forumService.upNumberOfThreads(forum.getSlug());
            return ResponseEntity.status(HttpStatus.CREATED).body(threadService.getThread(thread.getAuthor(), slug, thread.getTitle()));
        } catch (DataAccessException error) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(threadService.getThreadBySlug(thread.getSlug()));
        }
    }

    @PostMapping(value = "/api/thread/{slug_or_id}/vote")
    public ResponseEntity<?> voteForThread(@PathVariable("slug_or_id") String slugOrId, @RequestBody Vote vote) {
        Thread thread;
        try {
            final int threadID = Integer.parseInt(slugOrId);
            thread = threadService.getThreadByID(threadID);
        } catch (NumberFormatException e) {
            thread = threadService.getThreadBySlug(slugOrId);
        }

        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find thread by slug: " + slugOrId));
        }

        try {
            final User user = userService.getUser(vote.getNickname());
            Integer voteStatus = threadService.getVote(user.getId(), thread.getId());
            if (voteStatus == null) {
                voteStatus = 0;
            }

            switch (voteStatus) {
                case 0:
                    switch (vote.getVoice()) {
                        case 1:
                            threadService.vote(thread.getId(), user.getId(), 1, voteStatus);
                            break;
                        case -1:
                            threadService.vote(thread.getId(), user.getId(), -1, voteStatus);
                            break;
                    }
                    break;

                case 1:
                    if (vote.getVoice() == -1) {
                        threadService.vote(thread.getId(), user.getId(), -2, voteStatus);
                    }
                    break;
                case -1:
                    if (vote.getVoice() == 1) {
                        threadService.vote(thread.getId(), user.getId(), 2, voteStatus);
                    }
                    break;
            }

            return ResponseEntity.status(HttpStatus.OK).body(threadService.getThreadBySlug(thread.getSlug()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find user by nickname: " + thread.getAuthor()));
        }
    }

    @GetMapping(value = "api/thread/{slug_or_id}/details")
    public ResponseEntity<?> getDetails(@PathVariable("slug_or_id") String slugOrId) {
        Thread thread;
        try {
            final int threadID = Integer.parseInt(slugOrId);
            thread = threadService.getThreadByID(threadID);
        } catch (NumberFormatException e) {
            thread = threadService.getThreadBySlug(slugOrId);
        }

        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find thread by slug: " + slugOrId));
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(thread);
        }
    }

    @PostMapping(value = "api/thread/{slug_or_id}/details")
    public ResponseEntity<?> updateThread(@PathVariable("slug_or_id") String slugOrId, @RequestBody Thread changedThread) {
        Thread thread;
        try {
            final int threadID = Integer.parseInt(slugOrId);
            thread = threadService.getThreadByID(threadID);
        } catch (NumberFormatException e) {
            thread = threadService.getThreadBySlug(slugOrId);
        }

        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find thread by slug: " + slugOrId));
        }

        if (thread.getTitle().equals(changedThread.getTitle()) &&
                thread.getMessage().equals(changedThread.getMessage())) {
            return ResponseEntity.status(HttpStatus.OK).body(thread);
        }

        threadService.update(thread.getId(), changedThread);
        return ResponseEntity.status(HttpStatus.OK).body(threadService.getThreadByID(thread.getId()));
    }
}
