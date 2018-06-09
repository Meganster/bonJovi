package forumdb.DAO;


import forumdb.Model.InfoAboutDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class ServiceDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public InfoAboutDB getStatus() throws DataAccessException {
        final InfoAboutDB currentStatus = new InfoAboutDB();
        currentStatus.setForum(jdbcTemplate.queryForObject("SELECT count(*) FROM Forum;", Integer.class));
        currentStatus.setPost(jdbcTemplate.queryForObject("SELECT count(*) FROM Post;", Integer.class));
        currentStatus.setThread(jdbcTemplate.queryForObject("SELECT count(*) FROM Thread;", Integer.class));
        currentStatus.setUser(jdbcTemplate.queryForObject("SELECT count(*) FROM \"User\";", Integer.class));

        return currentStatus;
    }

    public void clear() throws DataAccessException {
        jdbcTemplate.update("TRUNCATE \"User\", Forum, Thread, Post, UserVoteForThreads CASCADE;");
    }
}
