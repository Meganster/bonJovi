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
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;



//@Transactional
@Repository
public class PostDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    UserDAO userService;

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public Array getPathByPostID(Long id) {
        final String sql = "SELECT path FROM Post WHERE id = ?;";
        return jdbcTemplate.queryForObject(sql, Array.class, id);
    }

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public Long getNextID() {
        final String sql = "SELECT nextval(pg_get_serial_sequence('Post', 'id'))";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Post> CreatePostsFromList(List<Post> posts, Thread thread) {
        final String timeForCreated = ZonedDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .toString();

        for (Post post : posts) {
            try {
                userService.getUser(post.getAuthor());
            } catch (DataAccessException e) {
                return null;
            }

            Array path = null;
            Post parent;

            if (post.getParent() == null) {
                post.setParent(0L);
                parent = null;
            } else {
                try {
                    parent = getPostById(post.getParent());
                } catch (DataAccessException e) {
                    // родительский пост
                    parent = null;
                }
            }

            if (post.getParent() != 0 && parent == null) {
                // не нашли родителя, хотя он должен быть
                throw new RuntimeException();
            } else {
                final Boolean haveParent = post.getParent() != 0 && parent != null;
                if (haveParent && !parent.getThread().equals(thread.getId())) {
                    // не нашли такую ветку обсуждений
                    throw new RuntimeException();
                }

                if (parent != null) {
                    if (post.getParent() != 0) {
                        path = getPathByPostID(parent.getId());
                    }
                }

                post.setForumID(thread.getForumID());
                post.setCreated(timeForCreated);
                post.setForum(thread.getForum());
                post.setThread(thread.getId());
                post.setIsEdited(false);

                try {
                    post.setId(getNextID());
                } catch (Exception e) {
                    System.out.println("Error in generate function");
                }

                jdbcTemplate.update(
                        "INSERT INTO Post (path, id, author, created, thread, forum, forum_id, isEdited, message, parent)" +
                                " VALUES (array_append(?, ?::INTEGER), ?, ?, ?::timestamptz, ?, ?, ?, ?, ?, ?);",
                        path, post.getId(), post.getId(), post.getAuthor(),
                        post.getCreated(), post.getThread(),
                        post.getForum(), post.getForumID(),
                        post.getIsEdited(), post.getMessage(), post.getParent());
            }
        }

        return posts;
    }

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public Post getPostById(@NotNull Long id) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM Post WHERE id=").append(id).append(";");

        return jdbcTemplate.queryForObject(sql.toString(), new PostMapper());
    }

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public void update(@NotNull Post post, @NotNull Post changedPost) {
        final String message = changedPost.getMessage();
        if (message == null || message.isEmpty() || message.equals(post.getMessage())) {
            return;
        }

        jdbcTemplate.update("UPDATE Post SET message=?, isEdited=TRUE WHERE id=?;", message, post.getId());
    }

    //@Transactional(isolation = Isolation.READ_COMMITTED)
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

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Post> getTreeSortForPosts(@NotNull Long threadID, @NotNull Long since,
                                          @NotNull Long limit, @NotNull Boolean desc) {
        List<Object> myObj = new ArrayList<>();
        final StringBuilder sql = new StringBuilder("SELECT * FROM Post WHERE thread=").append(threadID);
        myObj.add(threadID);

        if (since > 0) {
            if (desc == true) {
                sql.append(" AND path < (SELECT path FROM Post WHERE id=").append(since).append(") ");
            } else {
                sql.append(" AND path > (SELECT path FROM Post WHERE id=").append(since).append(") ");
            }

            myObj.add(since);
        }
        sql.append(" ORDER BY path ");

        if (desc == true) {
            sql.append(" DESC, id DESC ");
        }

        if (limit > 0) {
            sql.append(" LIMIT ").append(limit).append(";");
        }

        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Post> getParentTreeSortForPosts(@NotNull Long threadID, @NotNull Long since,
                                                @NotNull Long limit, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM Post JOIN ");

        if (since > 0) {
            if (desc == true) {
                if(limit > 0) {
                    sql.append(" (SELECT id FROM Post WHERE parent=0 AND thread=").append(threadID)
                            .append(" AND path[1] < (SELECT path[1] FROM Post WHERE id=").append(since)
                            .append(") ORDER BY path DESC, thread DESC LIMIT ").append(limit)
                            .append(") as TT ON thread=").append(threadID)
                            .append(" and path[1] = TT.id ");
                } else {
                    sql.append(" (SELECT id FROM Post WHERE parent=0 AND thread=").append(threadID)
                            .append(" and path < (SELECT path FROM Post WHERE id=").append(since)
                            .append(") ORDER BY path DESC, thread DESC LIMIT ").append(limit)
                            .append(") as TT ON thread=").append(threadID)
                            .append(" and path[1] = TT.id ");
                }
            } else {
                sql.append(" (SELECT id FROM Post WHERE parent=0 AND thread=").append(threadID)
                        .append(" and path > (SELECT path FROM Post WHERE id=").append(since)
                        .append(") ORDER BY path, thread  LIMIT ").append(limit)
                        .append(") as TT ON thread=").append(threadID)
                        .append(" and path[1] = TT.id ");
            }
        } else if (limit > 0) {
            if (desc) {
                sql.append(" (SELECT id FROM Post WHERE parent=0 and thread=").append(threadID)
                        .append(" ORDER BY path DESC, thread DESC LIMIT ").append(limit).append(") as TT ON thread=")
                        .append(threadID).append(" AND path[1]=TT.id ");
            } else {
                sql.append(" (SELECT id FROM Post WHERE parent=0 and thread=").append(threadID)
                        .append(" ORDER BY path, thread LIMIT ").append(limit).append(") as TT ON thread=")
                        .append(threadID).append(" AND path[1]=TT.id ");
            }
        }

        sql.append(" ORDER BY path");

        if (desc == true && since == 0) {
            sql.append("[1] DESC");

            if (limit > 0) {
                sql.append(", path");
            }
        }
        sql.append(';');

        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }


    public static class PostMapper implements RowMapper<Post> {
        @Override
        public Post mapRow(ResultSet resultSet, int i) throws SQLException {
            final Post post = new Post();
            post.setForum(resultSet.getString("forum"));
            post.setAuthor(resultSet.getString("author"));
            post.setThread(resultSet.getLong("thread"));
            final Timestamp timestamp = resultSet.getTimestamp("created");
            final SimpleDateFormat formatOfCreatedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            formatOfCreatedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
            post.setCreated(formatOfCreatedDate.format(timestamp.getTime()));
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
