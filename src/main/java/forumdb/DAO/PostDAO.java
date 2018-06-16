package forumdb.DAO;


import forumdb.Model.Post;
import forumdb.Model.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;

import static forumdb.Controller.PostController.MAX_LONG;;


//@Transactional(isolation = Isolation.READ_COMMITTED)
@Repository
public class PostDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    UserDAO userService;

    public Array getPathById(Long id) {
        return jdbcTemplate.queryForObject("SELECT path FROM Post WHERE id = ?;", Array.class, id);
    }

    public Long generateID() {
        return jdbcTemplate.queryForObject("SELECT nextval(pg_get_serial_sequence('Post', 'id'))", Long.class);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Post> CreatePosts(List<Post> posts, Thread thread) {
        final String currentTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        for (Post post : posts) {
            try {
                userService.getUser(post.getAuthor());
            } catch (DataAccessException e) {
                return null;
            }

            Array path = null;
            Post parentPost;

            if (post.getParent() == null) {
                post.setParent(0L);
                parentPost = null;
            } else {
                try {
                    parentPost = getPostById(post.getParent());
                } catch (DataAccessException e) {
                    // родительский пост
                    parentPost = null;
                }
            }

            if (post.getParent() != 0 && parentPost == null) {
                // не нашли родителя, хотя он должен быть
                throw new RuntimeException();
            } else {
                if (post.getParent() != 0 && parentPost != null && !parentPost.getThread().equals(thread.getId())) {
                    // не нашли такую ветку обсуждений
                    throw new RuntimeException();
                }

                if (parentPost != null) {
                    if (post.getParent() != 0) {
                        path = getPathById(parentPost.getId());
                    }
                }

                post.setCreated(currentTime);
                post.setForum(thread.getForum());
                post.setIsEdited(false);
                post.setThread(thread.getId());
                post.setForumID(thread.getForumID());
                try {
                    post.setId(generateID());
                } catch (Exception e) {
                    System.out.println("Error in generate function");
                }

                jdbcTemplate.update(
                        "INSERT INTO Post (id, author, created, forum, forum_id, isEdited, message, parent, path, thread)" +
                                " VALUES (?, ?, ?::TIMESTAMPTZ, ?, ?, ?, ?, ?, array_append(?, ?::INTEGER), ?);",
                        post.getId(), post.getAuthor(), post.getCreated(), post.getForum(), post.getForumID(),
                        post.getIsEdited(), post.getMessage(), post.getParent(), path, post.getId(), post.getThread());
            }
        }

        return posts;
    }

//    public Post getParentPost(@NotNull Long postID, @NotNull Long threadID) {
//        return jdbcTemplate.queryForObject("SELECT * FROM Post WHERE thread = ? AND id = ? ORDER BY id;",
//                new Object[]{threadID, postID}, new PostMapper());
//    }
//
//    public List<Post> getPostBySlugForum(@NotNull String slugForum) {
//        final StringBuilder sql = new StringBuilder();
//        sql.append("SELECT * FROM Post WHERE forum = ").append(slugForum).append("::citext;");
//        return jdbcTemplate.query(sql.toString(), new PostMapper());
//    }

    public Post getPostById(@NotNull Long id) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM Post WHERE id = ").append(id).append(";");

        return jdbcTemplate.queryForObject(sql.toString(), new PostMapper());
    }

    public void update(@NotNull Post post, @NotNull Post changedPost) {
        final String message = changedPost.getMessage();
        if (message == null || message.isEmpty() || message.equals(post.getMessage())) {
            return;
        }

        jdbcTemplate.update("UPDATE Post SET message = ?, isEdited = TRUE WHERE id = ?;", message, post.getId());
    }

    public List<Post> getFlatSortForPosts(@NotNull Long threadID, @NotNull Long since,
                                          @NotNull Long limit, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM Post WHERE thread=" + threadID);

        if (since > 0) {
            if (desc == true) {
                sql.append(" AND id<").append(since);
            } else {
                sql.append(" AND id>").append(since);
            }
        }

        if (desc == true) {
            sql.append(" ORDER BY created DESC, id DESC");
        } else {
            sql.append(" ORDER BY created, id");
        }

        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        sql.append(';');

        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

    public List<Post> getTreeSortForPosts(@NotNull Long threadID, @NotNull Long since,
                                          @NotNull Long limit, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM Post WHERE thread=? ");

        if (desc == true) {
            if (since != 0 && !since.equals(MAX_LONG)) {
                sql.append("AND path<(SELECT path FROM Post WHERE id=?)");
            } else {
                sql.append("AND path[1]<? ");
            }

            sql.append("ORDER BY path DESC ");
        } else {
            if (since != 0 && !since.equals(MAX_LONG)) {
                sql.append("AND path>(SELECT path FROM Post WHERE id=?)");
            } else {
                sql.append("AND path[1]>? ");
            }

            sql.append(" ORDER BY path");
        }
        sql.append(" LIMIT ? ");

        return jdbcTemplate.query(sql.toString(), new Object[]{threadID, since, limit}, new PostMapper());
    }

    public List<Post> getParentTreeSortForPosts(@NotNull Long threadID, @NotNull Long since,
                                                @NotNull Long limit, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM Post WHERE thread = ? AND path[1] IN (SELECT DISTINCT path[1] FROM Post ");

        if (desc == true) {
            if (since != 0 && !since.equals(MAX_LONG)) {
                sql.append("WHERE thread = ? AND path[1] < (SELECT path[1] FROM Post WHERE id = ?) ORDER BY path[1] DESC LIMIT ?) ORDER BY path[1] DESC ");
            } else {
                sql.append("WHERE thread = ? AND path[1] <  ? ORDER BY path[1] DESC LIMIT ?) ORDER BY path[1] DESC ");
            }
        } else {
            if (since != 0 && !since.equals(MAX_LONG)) {
                sql.append(" WHERE thread = ? AND path[1] > (SELECT path[1] FROM Post WHERE id = ?) ORDER BY path[1] LIMIT ?) ORDER BY path[1] ");
            } else {
                sql.append(" WHERE thread = ? AND path[1] > ? ORDER BY path[1] LIMIT ?) ORDER BY path[1]  ");
            }
        }
        sql.append(" , path ");

        return jdbcTemplate.query(sql.toString(), new Object[]{threadID, threadID, since, limit}, new PostMapper());
    }


    public static class PostMapper implements RowMapper<Post> {
        @Override
        public Post mapRow(ResultSet resultSet, int i) throws SQLException {
            final Timestamp timestamp = resultSet.getTimestamp("created");
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            final Post post = new Post();
            post.setForum(resultSet.getString("forum"));
            post.setAuthor(resultSet.getString("author"));
            post.setThread(resultSet.getLong("thread"));
            post.setCreated(dateFormat.format(timestamp.getTime()));
            post.setMessage(resultSet.getString("message"));
            post.setIsEdited(resultSet.getBoolean("isEdited"));
            post.setParent(resultSet.getLong("parent"));
            post.setId(resultSet.getLong("id"));
            post.setForumID(resultSet.getLong("forum_id"));

            try {
                post.setPath((Object[]) resultSet.getArray("path").getArray());
            } catch (NullPointerException e) {
                post.setPath(null);
            }

            return post;
        }
    }
}
