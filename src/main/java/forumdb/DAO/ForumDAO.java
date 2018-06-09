package forumdb.DAO;

import forumdb.Model.Forum;
import forumdb.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

//    TODO удалить мусор
//    public void upNumberOfThreads(@NotNull String slug) {
//        jdbcTemplate.update("UPDATE Forum SET threads = threads + 1 WHERE slug = ?::citext;", slug);
//    }

//    public void upNumberOfPosts(@NotNull String slug, @NotNull Integer numberOfPost) {
//        jdbcTemplate.update("UPDATE Forum SET posts = posts + ? WHERE slug = ?;",
//                numberOfPost, slug);
//    }

//    public List<User> getUsers(@NotNull String slugForum, @NotNull Integer limit,
//                               @NotNull String since, @NotNull Boolean desc) {
//        final StringBuilder sql = new StringBuilder("SELECT * FROM \"User\" WHERE \"User\".nickname IN " +
//                "(SELECT POST.author FROM POST WHERE POST.forum='" + slugForum + "'::citext " +
//                "UNION " +
//                "SELECT Thread.author FROM Thread WHERE Thread.forum='" + slugForum + "'::citext)");
//
//        if (!since.isEmpty()) {
//            if (desc == true) {
//                sql.append(" AND \"User\".nickname < '").append(since).append("'::citext");
//            } else {
//                sql.append(" AND \"User\".nickname > '").append(since).append("'::citext");
//            }
//        }
//
//        sql.append(" ORDER BY LOWER(\"User\".nickname)");
//        if (desc) {
//            sql.append(" DESC");
//        }
//
//        if (limit > 0) {
//            sql.append(" LIMIT ").append(limit);
//        }
//
//        return jdbcTemplate.query(sql.toString(), new UserDAO.UserMapper());
//    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<User> getUsers(@NotNull Integer forum_id, @NotNull Integer limit,
                               @NotNull String since, @NotNull Boolean desc) {
        try {
            final List<Object> parametersSQL = new ArrayList<>();
            final StringBuilder sql = new StringBuilder("SELECT id, nickname, fullname, email, about FROM ForumUsers WHERE forum_id = ? ");
            parametersSQL.add(forum_id);

            if (!since.isEmpty()) {
                if (desc == true) {
                    sql.append(" AND nickname < ?::citext ");
                } else {
                    sql.append(" AND nickname > ?::citext ");
                }

                parametersSQL.add(since);
            }
            sql.append(" ORDER BY nickname ");

            if (desc) {
                sql.append(" DESC ");
            }

            if (limit > 0) {
                sql.append(" LIMIT ? ");
                parametersSQL.add(limit);
            }

            return jdbcTemplate.query(sql.toString(), parametersSQL.toArray(), new UserDAO.UserMapper());
        } catch (DataAccessException e) {
            return null;
        }
    }


    public static class ForumMapper implements RowMapper<Forum> {
        @Override
        public Forum mapRow(ResultSet resultSet, int i) throws SQLException {
            final Forum forum = new Forum();
            forum.setId(resultSet.getInt("id"));
            forum.setTitle(resultSet.getString("title"));
            forum.setUser(resultSet.getString("user"));
            forum.setSlug(resultSet.getString("slug"));
            forum.setThreads(resultSet.getInt("threads"));
            forum.setPosts(resultSet.getInt("posts"));

            return forum;
        }
    }
}
