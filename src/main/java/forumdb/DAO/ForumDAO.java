package forumdb.DAO;

import forumdb.Model.Forum;
import forumdb.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


@Repository
public class ForumDAO {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public void create(@NotNull String title, @NotNull String user,
                       @NotNull String slug) throws DataAccessException {
        jdbcTemplate.update("INSERT INTO Forum (title, \"user\", slug) VALUES (?, ?, ?);",
                title, user, slug);
    }

    public Forum getForum(@NotNull String slug) throws DataAccessException {
        return jdbcTemplate.queryForObject("SELECT * FROM Forum WHERE slug = ?::citext;",
                new Object[]{slug}, new ForumMapper());
    }

    public void upNumberOfThreads(@NotNull String slug) {
        jdbcTemplate.update("UPDATE Forum SET threads = threads + 1 WHERE slug = ?::citext;", slug);
    }

    public void upNumberOfPosts(@NotNull String slug, @NotNull Integer numberOfPost) {
        jdbcTemplate.update("UPDATE Forum SET posts = posts + ? WHERE slug = ?;",
                numberOfPost, slug);
    }

    public List<User> getUsers(@NotNull String slugForum, @NotNull Integer limit,
                               @NotNull String since, @NotNull Boolean desc) {
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
