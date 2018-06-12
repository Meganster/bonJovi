package forumdb.DAO;


import forumdb.Model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//@Transactional(isolation = Isolation.READ_COMMITTED)
@Repository
public class PostDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    //@Transactional(isolation = Isolation.READ_COMMITTED)
    public Long createPost(Post post) {
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            final PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO Post(created, forum, thread, author, parent, message, forum_id) "
                            + "VALUES (?::timestamptz, ?, ?, ?, ?, ?, ?) returning id",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            pst.setString(1, post.getCreated());
            pst.setString(2, post.getForum());
            pst.setLong(3, post.getThread());
            pst.setString(4, post.getAuthor());
            pst.setLong(5, post.getParent());
            pst.setString(6, post.getMessage());
            pst.setLong(7, post.getForumID());

            return pst;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public void addPostToPath(Post parent, Post post) {
        jdbcTemplate.update(con -> {
            final PreparedStatement pst = con.prepareStatement(
                    "UPDATE Post SET path = ?  WHERE id = ?;");

            final ArrayList array = new ArrayList<Object>(Arrays.asList(parent.getPath()));
            array.add(post.getId());

            pst.setArray(1, con.createArrayOf("INT", array.toArray()));
            pst.setLong(2, post.getId());

            return pst;
        });
    }

    public void addPostToPathSelf(Post post) {
        jdbcTemplate.update(con -> {
            final PreparedStatement pst = con.prepareStatement(
                    "UPDATE Post SET path = ?  WHERE id = ?;");

            pst.setArray(1, con.createArrayOf("INT", new Object[]{post.getId()}));
            pst.setLong(2, post.getId());

            return pst;
        });
    }

    public Post getParentPost(@NotNull Long postID, @NotNull Long threadID) {
        return jdbcTemplate.queryForObject("SELECT * FROM Post WHERE thread = ? AND id = ? ORDER BY id;",
                new Object[]{threadID, postID}, new PostMapper());
    }

    public List<Post> getPostBySlugForum(@NotNull String slugForum) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM Post WHERE forum = ").append(slugForum).append("::citext;");
        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

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
        final StringBuilder sql = new StringBuilder("SELECT * FROM Post WHERE thread=").append(threadID);

        if (since > 0) {
            if (desc) {
                sql.append(" AND id < ").append(since);
            } else {
                sql.append(" AND id > ").append(since);
            }
        }
        sql.append(" ORDER BY created ");

        if (desc == true) {
            sql.append(" DESC, id DESC ");
        } else {
            sql.append(", id");
        }

        if (limit > 0) {
            sql.append(" LIMIT ").append(limit).append(";");
        }

        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

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

    //    public List<Post> getParentTreeSortForPosts(@NotNull Long threadID, @NotNull Long since,
//                                                @NotNull Long limit, @NotNull Boolean desc) {
//        final StringBuilder sql = new StringBuilder("SELECT * FROM Post JOIN ");
//
//        if (since > 0) {
//            if (desc == true) {
//                if(limit > 0) {
//                    sql.append(" (SELECT id FROM Post WHERE parent=0 AND thread=").append(threadID)
//                            .append(" AND path[1] < (SELECT path[1] FROM Post WHERE id=").append(since)
//                            .append(") ORDER BY path DESC, thread DESC LIMIT ").append(limit)
//                            .append(") as TT ON thread=").append(threadID)
//                            .append(" and path[1] = TT.id ");
//                } else {
//                    sql.append(" (SELECT id FROM Post WHERE parent=0 AND thread=").append(threadID)
//                            .append(" and path < (SELECT path FROM Post WHERE id=").append(since)
//                            .append(") ORDER BY path DESC, thread DESC LIMIT ").append(limit)
//                            .append(") as TT ON thread=").append(threadID)
//                            .append(" and path[1] = TT.id ");
//                }
//            } else {
//                sql.append(" (SELECT id FROM Post WHERE parent=0 AND thread=").append(threadID)
//                        .append(" and path > (SELECT path FROM Post WHERE id=").append(since)
//                        .append(") ORDER BY path, thread  LIMIT ").append(limit)
//                        .append(") as TT ON thread=").append(threadID)
//                        .append(" and path[1] = TT.id ");
//            }
//        } else if (limit > 0) {
//            if (desc) {
//                sql.append(" (SELECT id FROM Post WHERE parent=0 and thread=").append(threadID)
//                        .append(" ORDER BY path DESC, thread DESC LIMIT ").append(limit).append(") as TT ON thread=")
//                        .append(threadID).append(" AND path[1]=TT.id ");
//            } else {
//                sql.append(" (SELECT id FROM Post WHERE parent=0 and thread=").append(threadID)
//                        .append(" ORDER BY path, thread LIMIT ").append(limit).append(") as TT ON thread=")
//                        .append(threadID).append(" AND path[1]=TT.id ");
//            }
//        }
//
//        sql.append(" ORDER BY path");
//
//        if (desc == true && since == 0) {
//            sql.append("[1] DESC");
//
//            if (limit > 0) {
//                sql.append(", path");
//            }
//        }
//        sql.append(';');
//
//        return jdbcTemplate.query(sql.toString(), new PostMapper());
//    }
    public List<Post> getParentTreeSortForPosts(@NotNull Long threadID, @NotNull Long since,
                                                @NotNull Long limit, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("WITH RECURSIVE recursivetree (id, mypath) AS (" +
                " SELECT id, array_append('{}'::INTEGER[], id) FROM" +
                " (SELECT DISTINCT id FROM Post" +
                " WHERE thread=" + threadID +
                " AND parent=0 ORDER BY id");

        if (desc == true && !(limit > 0 && since > 0)) {
            sql.append(" DESC");
        }

        if (limit > 0) {
            if (!desc && since > 0) {
                sql.append(" DESC");
            }

            sql.append(" LIMIT ").append(limit);
        }

        sql.append(") superParents UNION ALL " +
                "SELECT P.id, array_append(mypath, P.id) FROM Post AS P " +
                "JOIN recursivetree AS R ON R.id=P.parent) " +
                "SELECT P.* FROM recursivetree JOIN Post AS P ON recursivetree.id=P.id");

        if (since > 0) {
            if (desc) {
                if (limit > 0) {
                    sql.append(" WHERE P.thread=").append(threadID).append(" AND recursivetree.mypath[1]").append('<')
                            .append("(SELECT recursivetree.mypath[1] FROM recursivetree WHERE recursivetree.id=").append(since)
                            .append(')');
                } else {
                    sql.append(" WHERE P.thread=").append(threadID).append(" AND recursivetree.mypath").append('<')
                            .append("(SELECT recursivetree.mypath FROM recursivetree WHERE recursivetree.id=").append(since)
                            .append(')');
                }
            } else {
                sql.append(" WHERE P.thread=").append(threadID).append(" AND recursivetree.mypath").append('>')
                        .append("(SELECT recursivetree.mypath FROM recursivetree WHERE recursivetree.id=").append(since)
                        .append(')');
            }
        }

        sql.append(" ORDER BY recursivetree.mypath");

        if (desc == true && since == 0) {
            sql.append("[1] DESC");

            if (limit > 0) {
                sql.append(", recursivetree.mypath");
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
            post.setCreated(resultSet.getTimestamp("created"));
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
