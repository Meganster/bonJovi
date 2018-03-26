package forumdb.DAO;


import forumdb.Model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PostDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createPost(@NotNull Post post) {
        List<String> existFieldsNames = new ArrayList<>();
        List<Object> existFieldsTypes = new ArrayList<>();

        Class checkedPost = Post.class;
        for (Field field : checkedPost.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                if (field.get(post) != null) {
                    existFieldsNames.add(field.getName());
                    existFieldsTypes.add(field.getType().cast(field.get(post)));
                }
            } catch (IllegalAccessException error) {
                System.out.println(error);
            }
        }

        final StringBuilder sqlNameRows = new StringBuilder();
        final StringBuilder sqlParameters = new StringBuilder();
        for (String nameRow : existFieldsNames) {
            sqlNameRows.append(nameRow).append(", ");
        }
        for (Object valueRow : existFieldsTypes) {
            sqlParameters.append(" '").append(valueRow.toString()).append("', ");
        }

        sqlNameRows.delete(sqlNameRows.length() - 2, sqlNameRows.length());
        sqlParameters.delete(sqlParameters.length() - 2, sqlParameters.length());

        final StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO Post (").append(sqlNameRows).append(") VALUES (").append(sqlParameters).append(");");
        jdbcTemplate.update(sql.toString());
    }

    public Integer getMaxPostId() {
        final Integer maxID = jdbcTemplate.queryForObject("SELECT max(id) FROM Post;", Integer.class);
        if (maxID == null) {
            return 0;
        } else {
            return maxID;
        }
    }

    public Post getParentPost(@NotNull Integer postID, @NotNull Integer threadID) {
        return jdbcTemplate.queryForObject("SELECT * FROM Post WHERE thread = ? AND id = ? ORDER BY id;",
                new Object[]{threadID, postID}, new PostMapper());
    }

    public List<Post> getPostBySlugForum(@NotNull String slugForum) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM Post WHERE forum = ").append(slugForum).append("::citext;");
        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

    public Post getPostById(@NotNull Integer id) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM Post WHERE id = ").append(id).append(";");
        return jdbcTemplate.queryForObject(sql.toString(), new PostMapper());
    }

    public List<Post> getNewPosts(@NotNull Integer id) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM Post WHERE id > ").append(id).append(" ORDER BY id;");
        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

    public void update(@NotNull Post post, @NotNull Post changedPost) {
        final String message = changedPost.getMessage();
        if (message == null || message.isEmpty() || message.equals(post.getMessage())) {
            return;
        }

        jdbcTemplate.update("UPDATE Post SET message = ?, isEdited = TRUE WHERE id = ?;", message, post.getId());
    }

    public List<Post> getFlatSortForPosts(@NotNull Integer threadID, @NotNull Integer since,
                                          @NotNull Integer limit, @NotNull Boolean desc) {
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

    public List<Post> getTreeSortForPosts(@NotNull Integer threadID, @NotNull Integer since,
                                          @NotNull Integer limit, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("WITH RECURSIVE recursivetree (id, path) AS (" +
                " SELECT id, array_append('{}'::INTEGER[], id) FROM Post WHERE parent=0 AND thread=" + threadID +
                "UNION ALL SELECT P.id, array_append(path, P.id) FROM Post AS P " +
                "JOIN recursivetree AS R ON R.id=P.parent AND P.thread=" + threadID +
                " ) SELECT P.* FROM recursivetree JOIN Post AS P ON recursivetree.id=P.id");

        if (since > 0) {
            if (desc) {
                sql.append(" WHERE P.thread=").append(threadID).append("AND recursivetree.path ").append('<')
                        .append(" (SELECT recursivetree.path FROM recursivetree WHERE recursivetree.id=").append(since)
                        .append(')');
            } else {
                sql.append(" WHERE P.thread=").append(threadID).append("AND recursivetree.path ").append('>')
                        .append(" (SELECT recursivetree.path FROM recursivetree WHERE recursivetree.id=").append(since)
                        .append(')');
            }
        }

        sql.append(" ORDER BY recursivetree.path");

        if (desc == true) {
            sql.append("  DESC, P.id DESC");
        }

        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        sql.append(';');

        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

    public List<Post> getParentTreeSortForPosts(@NotNull Integer threadID, @NotNull Integer since,
                                                @NotNull Integer limit, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("WITH RECURSIVE recursivetree (id, path) AS (" +
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
                "SELECT P.id, array_append(path, P.id) FROM Post AS P " +
                "JOIN recursivetree AS R ON R.id=P.parent) " +
                "SELECT P.* FROM recursivetree JOIN Post AS P ON recursivetree.id=P.id");

        if (since > 0) {
            if (desc) {
                if (limit > 0) {
                    sql.append(" WHERE P.thread=").append(threadID).append(" AND recursivetree.path[1]").append('<')
                            .append("(SELECT recursivetree.path[1] FROM recursivetree WHERE recursivetree.id=").append(since)
                            .append(')');
                } else {
                    sql.append(" WHERE P.thread=").append(threadID).append(" AND recursivetree.path").append('<')
                            .append("(SELECT recursivetree.path FROM recursivetree WHERE recursivetree.id=").append(since)
                            .append(')');
                }
            } else {
                sql.append(" WHERE P.thread=").append(threadID).append(" AND recursivetree.path").append('>')
                        .append("(SELECT recursivetree.path FROM recursivetree WHERE recursivetree.id=").append(since)
                        .append(')');
            }
        }

        sql.append(" ORDER BY recursivetree.path");

        if (desc == true && since == 0) {
            sql.append("[1] DESC");

            if (limit > 0) {
                sql.append(", recursivetree.path");
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
            post.setThread(resultSet.getInt("thread"));
            post.setCreated(resultSet.getTimestamp("created"));
            post.setMessage(resultSet.getString("message"));
            post.setIsEdited(resultSet.getBoolean("isEdited"));
            post.setParent(resultSet.getInt("parent"));
            post.setId(resultSet.getInt("id"));
            return post;
        }
    }
}
