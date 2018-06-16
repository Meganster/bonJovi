package forumdb.Controller;

import forumdb.DAO.ForumDAO;
import forumdb.DAO.PostDAO;
import forumdb.DAO.ThreadDAO;
import forumdb.DAO.UserDAO;
import forumdb.Model.*;
import forumdb.Model.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.Error;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;


@RestController
public class PostController {
    @Autowired
    UserDAO userService;
    @Autowired
    ForumDAO forumService;
    @Autowired
    PostDAO postService;
    @Autowired
    ThreadDAO threadService;

    public static final Long MAX_LONG = 2000000L;

    @PostMapping(value = "/api/thread/{slug_or_id}/create")
    public ResponseEntity<?> createPosts(@PathVariable("slug_or_id") String slugOrId, @RequestBody List<Post> posts) {
        Thread thread;
        try {
            final Long threadID = Long.parseLong(slugOrId);
            thread = threadService.getThreadByID(threadID);
        } catch (NumberFormatException e) {
            thread = threadService.getThreadBySlug(slugOrId);
        }

        if (thread != null) {
            try {
                posts = postService.CreatePosts(posts, thread);

                if (posts == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find post thread by id " + slugOrId));
                } else {
                    return ResponseEntity.status(HttpStatus.CREATED).body(posts);
                }
            } catch (RuntimeException error) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error("Parent post was created in another thread"));
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find post thread by id " + slugOrId));
        }
    }

    @PostMapping(value = "api/post/{id}/details")
    public ResponseEntity<?> updatePost(@PathVariable("id") Long id, @RequestBody Post changedPost) {
        final Post post;
        try {
            post = postService.getPostById(id);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find post with id: " + id));
        }

        postService.update(post, changedPost);
        return ResponseEntity.status(HttpStatus.OK).body(postService.getPostById(post.getId()));
    }

    @GetMapping(value = "api/post/{id}/details")
    public ResponseEntity<?> getPostDetails(@PathVariable("id") Long id,
                                            @RequestParam(value = "related", defaultValue = "") String[] related) {

        final Post post;
        try {
            post = postService.getPostById(id);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find post with id: " + id));
        }

        final PostDetails postDetails = new PostDetails(post);
        if (related == null) {
            return ResponseEntity.status(HttpStatus.OK).body(postDetails);
        } else {
            if (Arrays.asList(related).contains("user")) {
                postDetails.setAuthor(userService.getUser(post.getAuthor()));
            }

            if (Arrays.asList(related).contains("thread")) {
                postDetails.setThread(threadService.getThreadByID(post.getThread()));
            }

            if (Arrays.asList(related).contains("forum")) {
                postDetails.setForum(forumService.getForum(post.getForum()));
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(postDetails);
    }

    @GetMapping(value = "api/thread/{slug_or_id}/posts")
    public ResponseEntity<?> getPosts(@PathVariable("slug_or_id") String slugOrId,
                                      @RequestParam(value = "since", defaultValue = "0") Long since,
                                      @RequestParam(value = "limit", defaultValue = "0") Long limit,
                                      @RequestParam(value = "sort", defaultValue = "flat") String sort,
                                      @RequestParam(value = "desc", defaultValue = "false") Boolean desc) {

        Thread thread;
        try {
            final Long threadID = Long.parseLong(slugOrId);
            thread = threadService.getThreadByID(threadID);
        } catch (NumberFormatException e) {
            thread = threadService.getThreadBySlug(slugOrId);
        }

        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find thread by slug: " + slugOrId));
        }

        List<Post> resultPosts = null;
        if (sort.equals("flat")) {
            resultPosts = postService.getFlatSortForPosts(thread.getId(), since, limit, desc);
        }

        if (sort.equals("tree")) {
            if (limit == 0) {
                limit = MAX_LONG;
            }

            if (since == 0) {
                if (desc == true) {
                    since = MAX_LONG;
                }
            }

            resultPosts = postService.getTreeSortForPosts(thread.getId(), since, limit, desc);
        }

        if (sort.equals("parent_tree")) {
            if (limit == 0) {
                limit = MAX_LONG;
            }

            if (since == 0) {
                if (desc == true) {
                    since = MAX_LONG;
                }
            }

            resultPosts = postService.getParentTreeSortForPosts(thread.getId(), since, limit, desc);
        }

        return ResponseEntity.status(HttpStatus.OK).body(resultPosts);
    }
}
