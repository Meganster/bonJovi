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


@Transactional
@Repository
public class ForumDAO {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void create(@NotNull String title, @NotNull String user,
                       @NotNull String slug) throws DataAccessException {
        jdbcTemplate.update("INSERT INTO Forum (title, \"user\", slug) VALUES (?, ?, ?);",
                title, user, slug);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Forum getForum(@NotNull String slug) throws DataAccessException {
        return jdbcTemplate.queryForObject("SELECT * FROM Forum WHERE slug = ?::citext;",
                new Object[]{slug}, new ForumMapper());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<User> getUsers(@NotNull Long forum_id, @NotNull Long limit,
                               @NotNull String since, @NotNull Boolean desc) {
        try {
            final List<Object> parametersSQL = new ArrayList<>();
            final StringBuilder sql = new StringBuilder("SELECT id, nickname, fullname, email, about FROM ForumUsers WHERE forum_id=? ");
            parametersSQL.add(forum_id);

            if (!since.isEmpty()) {
                if (desc == true) {
                    sql.append(" AND nickname<?::citext ");
                } else {
                    sql.append(" AND nickname>?::citext ");
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
            forum.setId(resultSet.getLong("id"));
            forum.setTitle(resultSet.getString("title"));
            forum.setUser(resultSet.getString("user"));
            forum.setSlug(resultSet.getString("slug"));
            forum.setThreads(resultSet.getLong("threads"));
            forum.setPosts(resultSet.getLong("posts"));

            return forum;
        }
    }
}
