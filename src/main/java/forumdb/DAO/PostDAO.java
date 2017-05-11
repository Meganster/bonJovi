package forumdb.DAO;

import forumdb.Model.Post;
import forumdb.Model.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class PostDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createPost(Post post) {
        List<String> existFieldsNames = new ArrayList<String>();
        List<Object> existFieldsTypes = new ArrayList<Object>();

        Class chkPost = Post.class;
        for (Field field : chkPost.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                if (field.get(post) != null) {
                    existFieldsNames.add(field.getName());
                    existFieldsTypes.add(field.getType().cast(field.get(post)));
                }
            }catch (IllegalAccessException error){
                System.out.println(error);
            }
        }

        StringBuilder sqlNameRows = new StringBuilder();
        StringBuilder sqlParameters = new StringBuilder();
        for (String nameRow: existFieldsNames) {
            sqlNameRows.append(nameRow + ", ");
        }
        for (Object valueRow: existFieldsTypes) {
            sqlParameters.append(" '" + valueRow.toString() + "', ");
        }

        sqlNameRows.delete(sqlNameRows.length()-2, sqlNameRows.length());
        sqlParameters.delete(sqlParameters.length()-2, sqlParameters.length());

        String sql = "INSERT INTO Post (" + sqlNameRows + ") VALUES (" + sqlParameters + ")";
        //System.out.println("\n\n" + sql + "\n\n");
        jdbcTemplate.update(sql);
    }

    public Integer getMaxPostId() {
        final String sql = "SELECT max(id) FROM Post";

        Integer maxID = jdbcTemplate.queryForObject(sql, Integer.class);
        if(maxID == null) {
            return 0;
        } else {
            return maxID;
        }
    }

    public Post getParentPost(Integer postID, Integer threadID) {
        String sql = "SELECT * FROM Post WHERE thread = ? AND id = ? ORDER BY id";
        return jdbcTemplate.queryForObject(sql, new Object[]{threadID, postID}, new PostMapper());
    }

    public List<Post> getPostBySlugForum(String slugForum){
        String sql = "SELECT * FROM Post WHERE forum = " + slugForum + "::citext";
        return jdbcTemplate.query(sql, new PostMapper());
    }

    public Post getPostById(Integer id) {
        String sql = "SELECT * FROM Post WHERE id = " + id;
        return jdbcTemplate.queryForObject(sql, new PostMapper());
    }

    public List<Post> getNewPosts(int id) {
        String sql = "SELECT * FROM Post WHERE id > " + id + " ORDER BY id";
        return jdbcTemplate.query(sql, new PostMapper());
    }

    public void update(Post post, Post changedPost) {
        String message = changedPost.getMessage();
        if (message == null || message.isEmpty() || message.equals(post.getMessage())) {
            return;
        }

        String SQL = "UPDATE Post SET message = ?, isEdited = TRUE WHERE id = ?";
        jdbcTemplate.update(SQL, message, post.getId());
    }

    public List<Post> getFlatSortForPosts(Thread thread, Integer marker, Integer limit, Boolean desc) {
        Integer id = thread.getId();
        StringBuilder sql = new StringBuilder( "SELECT * FROM Post Where thread=" + id + " ORDER BY");

        if (desc == true) {
            sql.append(" created DESC, id DESC");
        } else {
            sql.append(" created, id");
        }

        if (limit > 0) {
            sql.append(" LIMIT " + limit);
        }

        if(marker > 0) {
            sql.append(" OFFSET " + marker);
        }

        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

    public List<Post> getTreeSortForPosts(Thread threadModel, Integer marker, Integer limit, Boolean desc) {
        Integer id = threadModel.getId();

        StringBuilder sql = new StringBuilder( "WITH RECURSIVE recursivetree (id, path) AS (" +
                " SELECT id, array_append('{}'::INTEGER[], id) FROM Post WHERE parent=0 AND thread=" + id +
                "UNION ALL SELECT P.id, array_append(path, P.id) FROM Post AS P "+
                "JOIN recursivetree AS R ON R.id=P.parent AND P.thread="+ id +
                " ) SELECT P.* FROM recursivetree JOIN Post AS P ON recursivetree.id=P.id ORDER BY recursivetree.path");

        if (desc == true) {
            sql.append(" DESC");
        }

        if (limit > 0) {
            sql.append(" LIMIT " + limit);
        }

        if(marker > 0) {
            sql.append(" OFFSET " + marker);
        }

        return jdbcTemplate.query(sql.toString(), new PostMapper());
    }

    public List<Post> getParentTreeSortForPosts(Thread threadModel, Integer marker, Integer limit, Boolean desc) {
        Integer id = threadModel.getId();

        StringBuilder sql = new StringBuilder("WITH RECURSIVE recursivetree (id, path) AS (" +
                " SELECT id, array_append('{}'::INTEGER[], id) FROM" +
                " (SELECT DISTINCT id FROM Post" +
                " WHERE thread=" + id +
                " AND parent=0 ORDER BY id");

        if (desc == true) {
            sql.append(" DESC");
        }

        if (limit > 0) {
            sql.append(" LIMIT " + limit);
        }

        if(marker > 0) {
            sql.append(" OFFSET " + marker);
        }

        sql.append( ") superParents UNION ALL " +
                "SELECT P.id, array_append(path, P.id) FROM Post AS P " +
                "JOIN recursivetree AS R ON R.id=P.parent) " +
                "SELECT P.* FROM recursivetree JOIN Post AS P ON recursivetree.id=P.id " +
                "ORDER BY recursivetree.path");

        if (desc == true) {
            sql.append(" DESC");
        }

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
