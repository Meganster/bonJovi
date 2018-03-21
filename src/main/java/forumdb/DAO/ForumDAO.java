package forumdb.DAO;

import forumdb.Model.Forum;
import forumdb.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


@Repository
public class ForumDAO {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public void create(String title, String user, String slug) throws DataAccessException {
        final String sql = "INSERT INTO Forum (title, \"user\", slug) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, title, user, slug);
    }

    public Forum getForum(String slug) throws DataAccessException {
        final String sql = "SELECT * FROM Forum WHERE slug = ?::citext";
        return jdbcTemplate.queryForObject(sql, new Object[]{slug}, new ForumMapper());
    }

    public void upNumberOfThreads(String slug) {
        final String sql = "UPDATE Forum SET threads = threads + 1 WHERE slug = ?::citext";
        jdbcTemplate.update(sql, slug);
    }

    public void upNumberOfPosts(String slug, int numberOfPost) {
        final String sql = "UPDATE Forum SET posts = posts + ? WHERE slug = ?";
        jdbcTemplate.update(sql, numberOfPost, slug);
    }

    public List<User> getUsers(String slugForum, Integer limit, String since, Boolean desc) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM \"User\" WHERE \"User\".nickname IN " +
                "(SELECT POST.author FROM POST WHERE POST.forum='" + slugForum + "'::citext " +
                "UNION " +
                "SELECT Thread.author FROM Thread WHERE Thread.forum='" + slugForum + "'::citext)");

        if (!since.isEmpty()) {
            if (desc == true) {
                sql.append(" AND \"User\".nickname < '").append(since).append("'::citext");
            } else {
                sql.append(" AND \"User\".nickname > '").append(since).append("'::citext");
            }
        }

        sql.append(" ORDER BY LOWER(\"User\".nickname)");
        if (desc) {
            sql.append(" DESC");
        }

        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        return jdbcTemplate.query(sql.toString(), new UserDAO.UserMapper());
    }


    public static class ForumMapper implements RowMapper<Forum> {

        @Override
        public Forum mapRow(ResultSet resultSet, int i) throws SQLException {
            final Forum forum = new Forum();
            forum.setTitle(resultSet.getString("title"));
            forum.setUser(resultSet.getString("user"));
            forum.setSlug(resultSet.getString("slug"));
            forum.setThreads(resultSet.getInt("threads"));
            forum.setPosts(resultSet.getInt("posts"));

            return forum;
        }
    }
}
