package forumdb.DAO;


import forumdb.Model.Thread;
import forumdb.Model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


@Repository
public class ThreadDAO {
    @Autowired
    JdbcTemplate jdbcTemplate;


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer createThread(@NotNull Thread thread) throws DataAccessException {
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            final PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO Thread(title, author, forum, message, slug, votes, created)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?::timestamptz) returning id;",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            pst.setString(1, thread.getTitle());
            pst.setString(2, thread.getAuthor());
            pst.setString(3, thread.getForum());
            pst.setString(4, thread.getMessage());
            pst.setString(5, thread.getSlug());
            pst.setInt(6, thread.getVotes());
            pst.setString(7, thread.getCreated());

            return pst;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    public List<Thread> getThreads(@NotNull String slugForum,
                                   @NotNull Integer limit, @NotNull String since, @NotNull Boolean desc) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM Thread WHERE forum = '" + slugForum + "'::citext");
        if (!since.isEmpty()) {
            if (desc == true) {
                sql.append(" AND created <= '").append(since).append("'::timestamptz");
            } else {
                sql.append(" AND created >= '").append(since).append("'::timestamptz");
            }
        }

        sql.append(" ORDER BY created");
        if (desc) {
            sql.append(" DESC");
        }

        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        return jdbcTemplate.query(sql.toString(), new ThreadMapper());
    }

    public Thread getThreadByID(@NotNull Integer id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM Thread WHERE id = ?;",
                    new Object[]{id}, new ThreadMapper());
        } catch (DataAccessException e) {
            return null;
        }
    }

    public Thread getThreadBySlug(@NotNull String slug) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM Thread WHERE slug = ?::citext;",
                    new Object[]{slug}, new ThreadMapper());
        } catch (DataAccessException e) {
            return null;
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void vote(Thread thread, Vote vote) {
            final String sql = "INSERT INTO UserVoteForThreads (user_id, thread_id, vote) " +
                    "SELECT( SELECT id FROM \"User\" WHERE nickname=?) AS uid, " +
                    "?, ? ON CONFLICT (user_id, thread_id) " +
                    "DO UPDATE SET vote = EXCLUDED.vote;";
            jdbcTemplate.update(sql, vote.getNickname(), thread.getId(), vote.getVoice());
    }

    public void update(@NotNull Integer threadID, @NotNull Thread changedThread) {
        final StringBuilder sql = new StringBuilder("UPDATE Thread");

        final String title = changedThread.getTitle();
        Boolean addedTitle = false;
        Boolean addedMessage = false;

        if (title != null && !title.isEmpty()) {
            sql.append(" SET title='").append(title).append("'");
            addedTitle = true;
        }

        final String message = changedThread.getMessage();
        if (message != null && !message.isEmpty()) {
            if (addedTitle) {
                sql.append(',');
            } else {
                sql.append(" SET");
            }

            sql.append(" message='").append(message).append("'");
            addedMessage = true;
        }

        if (addedMessage || addedTitle) {
            sql.append(" WHERE id=").append(threadID);
            jdbcTemplate.update(sql.toString());
        }
    }


    public static class ThreadMapper implements RowMapper<Thread> {

        @Override
        public Thread mapRow(ResultSet resultSet, int i) throws SQLException {
            final Thread thread = new Thread();
            thread.setTitle(resultSet.getString("title"));
            thread.setSlug(resultSet.getString("slug"));
            thread.setAuthor(resultSet.getString("author"));
            thread.setForum(resultSet.getString("forum"));
            thread.setMessage(resultSet.getString("message"));
            thread.setCreated(resultSet.getTimestamp("created"));
            thread.setId(resultSet.getInt("id"));
            thread.setVotes(resultSet.getInt("votes"));

            return thread;
        }
    }
}
