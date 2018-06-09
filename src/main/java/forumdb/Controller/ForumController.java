package forumdb.Controller;

import forumdb.DAO.ForumDAO;
import forumdb.DAO.ThreadDAO;
import forumdb.DAO.UserDAO;
import forumdb.Model.Forum;
import forumdb.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ForumController {
    @Autowired
    private ForumDAO forumService;
    @Autowired
    private UserDAO userService;
    @Autowired
    private ThreadDAO threadService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @PostMapping(value = "/api/forum/create")
    public ResponseEntity<?> createForum(@RequestBody Forum forum) {
        try {
            final Forum existForum = forumService.getForum(forum.getSlug());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(existForum);
        } catch (DataAccessException error) {
            try {
                final User existUser = userService.getUser(forum.getUser());

                forumService.create(forum.getTitle(), existUser.getNickname(), forum.getSlug());
                return ResponseEntity.status(HttpStatus.CREATED).body(forumService.getForum(forum.getSlug()));
            } catch (DataAccessException errorSecond) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find user with nickname: " + forum.getUser()));
            }
        }
    }

    @GetMapping(value = "api/forum/{slug}/details")
    public ResponseEntity<?> getForum(@PathVariable("slug") String slug) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(forumService.getForum(slug));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find forum with slug: " + slug));
        }
    }

    @GetMapping(value = "api/forum/{slug}/threads")
    public ResponseEntity<?> getThreads(@PathVariable("slug") String forumSlug,
                                        @RequestParam(value = "since", defaultValue = "") String since,
                                        @RequestParam(value = "limit", defaultValue = "0") Integer limit,
                                        @RequestParam(value = "desc", defaultValue = "false") Boolean desc) {
        try {
            forumService.getForum(forumSlug);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find forum with slug: " + forumSlug));
        }

        return ResponseEntity.status(HttpStatus.OK).body(threadService.getThreads(forumSlug, limit, since, desc));
    }

    @GetMapping(value = "api/forum/{slug}/users")
    public ResponseEntity<?> getUsers(@PathVariable String slug,
                                      @RequestParam(value = "limit", defaultValue = "0") Integer limit,
                                      @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
                                      @RequestParam(value = "since", defaultValue = "") String since) {
        final Forum forum;
        try {
            forum = forumService.getForum(slug);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find forum with slug: " + slug));
        }

        return ResponseEntity.status(HttpStatus.OK).body(forumService.getUsers(forum.getId(), limit, since, desc));
    }
}
