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

    @PostMapping(value = "/api/thread/{slug_or_id}/create")
    public ResponseEntity<?> createPosts(@PathVariable("slug_or_id") String slugOrId, @RequestBody List<Post> posts) {

        Thread thread;
        try {
            final int threadID = Integer.parseInt(slugOrId);
            thread = threadService.getThreadByID(threadID);
        } catch (NumberFormatException e) {
            thread = threadService.getThreadBySlug(slugOrId);
        }

        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find post thread by id " + slugOrId));
        }


        try {
            final Forum forum = forumService.getForum(thread.getForum());
            final Timestamp currentTime = new Timestamp(System.currentTimeMillis());

            for (Post post : posts) {
                if (post.getForum() != null) {
                    if (!post.getForum().equals(thread.getForum())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(postService.getPostBySlugForum(thread.getForum()));
                    }
                }

                post.setForum(forum.getSlug());
                post.setThread(thread.getId());
                post.setCreated(currentTime);

                try {
                    userService.getUser(post.getAuthor());
                } catch (DataAccessException e) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find post thread by id " + slugOrId));
                }

                final Integer parentId = post.getParent();
                if (parentId != null && !parentId.equals(0)) {
                    postService.getParentPost(parentId, thread.getId());
                } else {
                    post.setParent(0);
                }

                final Integer postID = postService.createPost(post);
                post.setId(postID);
            }

            //forumService.upNumberOfPosts(forum.getSlug(), posts.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(posts);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error("Parent post was created in another thread"));
        }
    }

    @PostMapping(value = "api/post/{id}/details")
    public ResponseEntity<?> updatePost(@PathVariable("id") Integer id, @RequestBody Post changedPost) {
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
    public ResponseEntity<?> getPostDetails(@PathVariable("id") Integer id,
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
        }

        for (String key : related) {
            if (key.equals("user")) {
                postDetails.setAuthor(userService.getUser(post.getAuthor()));
            }

            if (key.equals("thread")) {
                postDetails.setThread(threadService.getThreadByID(post.getThread()));
            }

            if (key.equals("forum")) {
                postDetails.setForum(forumService.getForum(post.getForum()));
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(postDetails);
    }

    @GetMapping(value = "api/thread/{slug_or_id}/posts")
    public ResponseEntity<?> getPosts(@PathVariable("slug_or_id") String slugOrId,
                                      @RequestParam(value = "since", defaultValue = "0") Integer since,
                                      @RequestParam(value = "limit", defaultValue = "0") Integer limit,
                                      @RequestParam(value = "sort", defaultValue = "flat") String sort,
                                      @RequestParam(value = "desc", defaultValue = "false") Boolean desc) {

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

        List<Post> resultPosts = null;
        if (sort.equals("flat")) {
            resultPosts = postService.getFlatSortForPosts(thread.getId(), since, limit, desc);
        }

        if (sort.equals("tree")) {
            resultPosts = postService.getTreeSortForPosts(thread.getId(), since, limit, desc);
        }

        if (sort.equals("parent_tree")) {
            resultPosts = postService.getParentTreeSortForPosts(thread.getId(), since, limit, desc);
        }

        return ResponseEntity.status(HttpStatus.OK).body(resultPosts);
    }
}
