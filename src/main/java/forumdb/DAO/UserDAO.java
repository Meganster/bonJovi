package forumdb.DAO;


import forumdb.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@Repository
public class UserDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void create(String email, String nickname, String fullname, String about) throws DataAccessException {
        String sql = "INSERT INTO \"User\" (email, nickname, fullname, about) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, email, nickname, fullname, about);
    }

    public void updateProfile(String email, String fullname, String about, String nickname) throws DataAccessException {
        Boolean conditionEmail = email!=null && !email.isEmpty();
        Boolean conditionAbout = about!=null && !about.isEmpty();
        Boolean conditionFullname = fullname!=null && !fullname.isEmpty();

        StringBuilder sql = new StringBuilder("UPDATE \"User\" SET");

            if(conditionEmail){
                sql.append(" email='" + email +"'::citext");
            }

            if(conditionAbout){
                if(conditionEmail){
                    sql.append(",");
                }
                sql.append(" about='" + about + "'");
            }

            if(conditionFullname){
                if(sql.length() > 17) {
                    sql.append(",");
                }
                sql.append(" fullname='" + fullname + "'");
            }

            if(sql.length() > 17) {
                sql.append(" WHERE nickname='" + nickname + "'::citext;");
                //System.out.print("\n\n" + sql.toString() + "\n\n");
                jdbcTemplate.update(sql.toString());
            }
    }

    public User getUser(String nickname) throws DataAccessException {
        String sql = "SELECT * FROM \"User\" WHERE nickname = ?::citext";
        User user = jdbcTemplate.queryForObject(sql, new Object[]{nickname}, new UserMapper());
        return user;
    }

    public ArrayList<User> getUsers(String nickname, String email) throws DataAccessException {
        String sql = "SELECT * FROM \"User\" WHERE email = ?::citext OR nickname = ?::citext";

        ArrayList<User> resultArray = (ArrayList<User>)jdbcTemplate.query(sql, new Object[]{email, nickname}, new UserMapper());
        return resultArray;
    }

    public static class UserMapper implements RowMapper<User> {

        @Override
        public User mapRow(ResultSet resultSet, int i) throws SQLException {
            User user = new User();
            user.setEmail(resultSet.getString("email"));
            user.setNickname(resultSet.getString("nickname"));
            user.setFullname(resultSet.getString("fullname"));
            user.setAbout(resultSet.getString("about"));
            user.setId(resultSet.getInt("id"));

            return user;
        }
    }
}
