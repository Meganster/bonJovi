package forumdb.DAO;


import forumdb.Model.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@Repository
public class ThreadDAO {

    @Autowired
    JdbcTemplate jdbcTemplate;

    //TODO rewrite method
    public void createThread(Thread thread) throws DataAccessException{
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

        String sql = "INSERT INTO Thread (" + sqlNameRows + ") VALUES (" + sqlParameters + ")";
        //System.out.println("\n\n" + sql + "\n\n");
        jdbcTemplate.update(sql);
    }

    public Thread getThread(String nickname, String slugForum, String title) {
        String sql = "SELECT * FROM Thread WHERE author = ?::citext AND forum = ?::citext AND title = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{nickname, slugForum, title}, new ThreadMapper());
    }

    public Thread getThread(String slugThread) {
        String sql = "SELECT * FROM Thread WHERE slug = ?::citext";
        return jdbcTemplate.queryForObject(sql, new Object[]{slugThread}, new ThreadMapper());
    }

    public Thread getThreadID(Integer threadID) {
        String sql = "SELECT * FROM Thread WHERE id =" + threadID;
        return jdbcTemplate.queryForObject(sql, new ThreadMapper());
    }

    public List<Thread> getThreads(String slugForum, Integer limit, String since, Boolean desc) {
        StringBuilder sql = new StringBuilder("SELECT * FROM Thread WHERE forum = '" + slugForum + "'::citext");
        if (!since.isEmpty()) {
            if (desc == true) {
                sql.append(" AND created <= '" + since + "'::timestamptz");
            } else {
                sql.append(" AND created >= '" + since + "'::timestamptz");
            }
        }

        sql.append(" ORDER BY created");
        if (desc) {
            sql.append(" DESC");
        }

        if (limit > 0) {
            sql.append(" LIMIT " + limit);
        }
        //System.out.print("\n\n" + sql + "\n\n");
        return jdbcTemplate.query(sql.toString(), new ThreadMapper());
    }

    public Thread getThreadSlugOrId(String slugOrId) {
        try {
            try {
                int threadID = Integer.parseInt(slugOrId);
                String SQL = "SELECT * FROM Thread WHERE id = ?";
                return jdbcTemplate.queryForObject(SQL, new Object[]{threadID}, new ThreadMapper());
            } catch (DataAccessException e) {
                return null;
            }
        } catch (NumberFormatException e) {
            try {
                String sql = "SELECT * FROM Thread WHERE slug = ?::citext";
                return jdbcTemplate.queryForObject(sql, new Object[]{slugOrId}, new ThreadMapper());
            } catch (DataAccessException err) {
                return null;
            }
        }
    }

    public Integer getVote(Integer userID, Integer threadID) {
        try {
            String SQL = "SELECT vote FROM UserVoteForThreads WHERE user_id = ? AND thread_id = ?";
            return jdbcTemplate.queryForObject(SQL, new Object[]{userID, threadID}, Integer.class);
        } catch (DataAccessException e) {
            return null;
        }
    }

    public void vote(Integer threadID, Integer userID, Integer key, Integer voteStatus) throws DataAccessException {
            String sql1 = "UPDATE Thread SET votes = votes + ? WHERE id = ?";
            jdbcTemplate.update(sql1, key, threadID);

            String sql2;
            if ( voteStatus == 0 ) {
                sql2 = "INSERT INTO UserVoteForThreads (user_id, thread_id, vote) VALUES (?, ?, ?)";
                jdbcTemplate.update(sql2, userID, threadID, key);
            } else {
                sql2 = "UPDATE UserVoteForThreads SET vote = ? WHERE thread_id = ? AND user_id = ?";
                jdbcTemplate.update(sql2, key, threadID, userID);
            }
    }

    public void update(Integer threadID, Thread changedThread) {
        StringBuilder sql = new StringBuilder( "UPDATE Thread");

        String title = changedThread.getTitle();
        Boolean addTitle = false, addMessage =false;
        if(title != null && !title.isEmpty()) {
            sql.append(" SET title='" + title + "'");
            addTitle = true;
        }

        String message = changedThread.getMessage();
        if(message != null && !message.isEmpty()) {
            if(addTitle){
                sql.append(",");
            } else {
                sql.append(" SET");
            }

            sql.append(" message='" + message + "'");
            addMessage = true;
        }

        if(addMessage || addTitle) {
            sql.append(" WHERE id=" + threadID);
            jdbcTemplate.update(sql.toString());
        }
    }





    public static class ThreadMapper implements RowMapper<Thread> {

        @Override
        public Thread mapRow(ResultSet resultSet, int i) throws SQLException {
            Thread thread = new Thread();
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
