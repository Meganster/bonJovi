package forumdb.Controller;

import forumdb.DAO.ForumDAO;
import forumdb.DAO.ThreadDAO;
import forumdb.DAO.UserDAO;
import forumdb.Model.Forum;
import forumdb.Model.Thread;
import forumdb.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
public class ForumController {
    @Autowired
    private ForumDAO forumTemplate;
    @Autowired
    private UserDAO userTemplate;
    @Autowired
    private ThreadDAO threadTemplate;

    @PostMapping(value = "/api/forum/create")
    public Forum createForum(@RequestBody Forum forum, HttpServletResponse response) {
        try {
            final Forum existForum = forumTemplate.getForum(forum.getSlug());
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return existForum;
        } catch (DataAccessException error) {
            try {
                final User existUser = userTemplate.getUser(forum.getUser());

                forumTemplate.create(forum.getTitle(), existUser.getNickname(), forum.getSlug());
                response.setStatus(HttpServletResponse.SC_CREATED);
                return forumTemplate.getForum(forum.getSlug());
            } catch (DataAccessException error1) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }
        }
    }

    @GetMapping(value = "api/forum/{slug}/details")
    public Forum getForum(@PathVariable("slug") String slug, HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            return forumTemplate.getForum(slug);
        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    @GetMapping(value = "api/forum/{slug}/threads")
    public List<Thread> getThreads(@PathVariable("slug") String forumSlug,
                                   @RequestParam(value = "since", defaultValue = "") String since,
                                   @RequestParam(value = "limit", defaultValue = "0") Integer limit,
                                   @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
                                   HttpServletResponse response) {
        try {
            forumTemplate.getForum(forumSlug);
        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        return threadTemplate.getThreads(forumSlug, limit, since, desc);
    }

    @GetMapping(value = "api/forum/{slug}/users")
    public List<User> getUsers(@PathVariable String slug,
                               @RequestParam(value = "limit", defaultValue = "0") Integer limit,
                               @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
                               @RequestParam(value = "since", defaultValue = "") String since,
                               HttpServletResponse response) {
        try {
            forumTemplate.getForum(slug);
        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }


        return forumTemplate.getUsers(slug, limit, since, desc);
    }
}
