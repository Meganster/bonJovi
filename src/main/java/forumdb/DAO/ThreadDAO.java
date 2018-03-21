package forumdb.DAO;


import forumdb.Model.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@Repository
public class ThreadDAO {

    @Autowired
    JdbcTemplate jdbcTemplate;

    //TODO rewrite method (IMPORTANT!!!)
    public void createThread(Thread thread) throws DataAccessException {
        List<String> existFieldsNames = new ArrayList<String>();
        List<Object> existFieldsTypes = new ArrayList<Object>();

        Class chkThread = Thread.class;
        for (Field field : chkThread.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                if (field.get(thread) != null) {
                    existFieldsNames.add(field.getName());
                    existFieldsTypes.add(field.getType().cast(field.get(thread)));
                }
            } catch (IllegalAccessException error) {
                System.out.println(error);
            }
        }

        StringBuilder sqlNameRows = new StringBuilder();
        StringBuilder sqlParameters = new StringBuilder();
        for (String nameRow : existFieldsNames) {
            sqlNameRows.append(nameRow + ", ");
        }

        for (Object valueRow : existFieldsTypes) {
            sqlParameters.append(" '" + valueRow.toString() + "', ");
        }

        sqlNameRows.delete(sqlNameRows.length() - 2, sqlNameRows.length());
        sqlParameters.delete(sqlParameters.length() - 2, sqlParameters.length());

        jdbcTemplate.update("INSERT INTO Thread (" + sqlNameRows + ") VALUES (" + sqlParameters + ')');
    }

    public Thread getThread(String nickname, String slugForum, String title) {
        return jdbcTemplate.queryForObject("SELECT * FROM Thread WHERE author = ?::citext AND forum = ?::citext AND title = ?",
                new Object[]{nickname, slugForum, title}, new ThreadMapper());
    }

    public Thread getThread(String slugThread) {
        final String sql = "SELECT * FROM Thread WHERE slug = ?::citext";
        return jdbcTemplate.queryForObject(sql, new Object[]{slugThread}, new ThreadMapper());
    }

    public Thread getThreadID(Integer threadID) {
        final String sql = "SELECT * FROM Thread WHERE id =" + threadID;
        return jdbcTemplate.queryForObject(sql, new ThreadMapper());
    }

    public List<Thread> getThreads(String slugForum, Integer limit, String since, Boolean desc) {
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

    public Thread getThreadSlugOrId(String slugOrId) {
        try {
            try {
                final int threadID = Integer.parseInt(slugOrId);
                String sql = "SELECT * FROM Thread WHERE id = ?";
                return jdbcTemplate.queryForObject(sql, new Object[]{threadID}, new ThreadMapper());
            } catch (DataAccessException e) {
                return null;
            }
        } catch (NumberFormatException e) {
            try {
                final String sql = "SELECT * FROM Thread WHERE slug = ?::citext";
                return jdbcTemplate.queryForObject(sql, new Object[]{slugOrId}, new ThreadMapper());
            } catch (DataAccessException err) {
                return null;
            }
        }
    }

    public Integer getVote(Integer userID, Integer threadID) {
        try {
            final String sql = "SELECT vote FROM UserVoteForThreads WHERE user_id = ? AND thread_id = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{userID, threadID}, Integer.class);
        } catch (DataAccessException e) {
            return null;
        }
    }

    public void vote(Integer threadID, Integer userID, Integer key, Integer voteStatus) throws DataAccessException {
        final String sql1 = "UPDATE Thread SET votes = votes + ? WHERE id = ?";
        jdbcTemplate.update(sql1, key, threadID);

        final String sql2;
        if (voteStatus == 0) {
            sql2 = "INSERT INTO UserVoteForThreads (user_id, thread_id, vote) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql2, userID, threadID, key);
        } else {
            sql2 = "UPDATE UserVoteForThreads SET vote = ? WHERE thread_id = ? AND user_id = ?";
            jdbcTemplate.update(sql2, key, threadID, userID);
        }
    }

    public void update(Integer threadID, Thread changedThread) {
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
