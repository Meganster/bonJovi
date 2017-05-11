package forumdb.Controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import forumdb.DAO.ForumDAO;
import forumdb.DAO.PostDAO;
import forumdb.DAO.ThreadDAO;
import forumdb.DAO.UserDAO;
import forumdb.Model.*;
import forumdb.Model.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.List;


@RestController
public class PostController {
    @Autowired
    UserDAO userTemplate;
    @Autowired
    ForumDAO forumTemplate;
    @Autowired
    PostDAO postTemplate;
    @Autowired
    ThreadDAO threadTemplate;

    private static final ObjectMapper mapperData = new ObjectMapper();

    @PostMapping(value = "/api/thread/{slug_or_id}/create")
    public List<Post> createPosts(@PathVariable("slug_or_id") String slugOrId, @RequestBody List<Post> posts, HttpServletResponse response) {

        Thread thread = threadTemplate.getThreadSlugOrId(slugOrId);
        if (thread == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        try {
            Forum forum = forumTemplate.getForum(thread.getForum());
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            Integer oldMaxPostID = postTemplate.getMaxPostId();

            for (Post post : posts) {
                if(post.getForum() != null) {
                    if (post.getForum() != thread.getForum()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        return postTemplate.getPostBySlugForum(thread.getForum());
                    }
                }

                post.setForum(forum.getSlug());
                post.setThread(thread.getId());
                post.setCreated(currentTime);

                try {
                    userTemplate.getUser(post.getAuthor());
                } catch (DataAccessException e) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return null;
                }

                Integer parentId = post.getParent();
                if(parentId != null && !parentId.equals(0)) {
                    postTemplate.getParentPost(parentId, thread.getId());
                }

                postTemplate.createPost(post);
            }

            forumTemplate.upNumberOfPosts(forum.getSlug(), posts.size());
            response.setStatus(HttpServletResponse.SC_CREATED);
            return postTemplate.getNewPosts(oldMaxPostID);
        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return null;
        }
    }

    @PostMapping(value = "api/post/{id}/details")
    public Post updatePost(@PathVariable("id") Integer id, @RequestBody Post changedPost, HttpServletResponse response) {
        Post post;
        try {
            post = postTemplate.getPostById(id);
        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        postTemplate.update(post, changedPost);
        return postTemplate.getPostById(post.getId());
    }

    @GetMapping(value = "api/post/{id}/details")
    public PostDetails getPostDetails(@PathVariable("id") Integer id,
                                      @RequestParam(value = "related", defaultValue = "") String[] related,
                                      HttpServletResponse response) {

        Post post;
        try {
            post = postTemplate.getPostById(id);
        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        PostDetails postDetails = new PostDetails(post);
        if (related == null) {
            return postDetails;
        }

        for (String key : related) {
            if (key.equals("user")) {
                postDetails.setAuthor( userTemplate.getUser(post.getAuthor()) );;
            }

            if (key.equals("thread")) {
                postDetails.setThread( threadTemplate.getThreadID(post.getThread()) );;
            }

            if (key.equals("forum")) {
                postDetails.setForum( forumTemplate.getForum(post.getForum()) );
            }
        }

        return postDetails;
    }

    //TODO check this shit
    @GetMapping(value = "api/thread/{slug_or_id}/posts")
    public ObjectNode getPosts(@PathVariable("slug_or_id") String slugOrId,
                               @RequestParam(value = "marker", defaultValue = "0") Integer marker,
                               @RequestParam(value = "limit", defaultValue = "0") Integer limit,
                               @RequestParam(value = "sort", defaultValue = "flat") String sort,
                               @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
                               HttpServletResponse response) {

        final Thread thread = threadTemplate.getThreadSlugOrId(slugOrId);
        if (thread == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        List<Post> resultPosts = null;
        if (sort.equals("flat")) {
            resultPosts = postTemplate.getFlatSortForPosts(thread, marker, limit, desc);
            marker += resultPosts.size();
        }

        if (sort.equals("tree")) {
            resultPosts = postTemplate.getTreeSortForPosts(thread, marker, limit, desc);
            marker += resultPosts.size();
        }

        if (sort.equals("parent_tree")) {
            resultPosts = postTemplate.getParentTreeSortForPosts(thread, marker, limit, desc);
            for (Post post : resultPosts) {
                if(post.getParent() == 0 || post.getParent() == null) {
                    marker++;
                }
            }
        }

        final ArrayNode postsJSON = mapperData.createArrayNode();
        for (Post post : resultPosts) {
            postsJSON.add(mapperData.convertValue(post, JsonNode.class));
        }

        final ObjectNode sortedPostsJSON = mapperData.createObjectNode();
        sortedPostsJSON.put("marker", marker.toString());
        sortedPostsJSON.set("posts", postsJSON);
        return sortedPostsJSON;
    }
}
