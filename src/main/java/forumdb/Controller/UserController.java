package forumdb.Controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import forumdb.DAO.ForumDAO;
import forumdb.DAO.UserDAO;
import forumdb.Model.Forum;

import forumdb.Model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.util.ArrayList;

@RestController
public class UserController {

    private static final ObjectMapper mapperData = new ObjectMapper();
    @Autowired
    private UserDAO userTemplate;


    @PostMapping(value = "/api/user/{nickname}/create")
    public String createUser(@PathVariable("nickname") String nickname, @RequestBody User user, HttpServletResponse response) {
        try {
            userTemplate.create(user.getEmail(), nickname,
                    user.getFullname(), user.getAbout());
            response.setStatus(HttpServletResponse.SC_CREATED);

            String result = userTemplate.getUser(nickname).toObjectNode(mapperData).toString();
            return result;
        } catch (DataAccessException error) {
            final ArrayList<User> existUsers = userTemplate.getUsers(nickname, user.getEmail());
            final StringWriter result = new StringWriter();
            response.setStatus(HttpServletResponse.SC_CONFLICT);

            //TODO rewrite type of this exception
            try {
                mapperData.writeValue(result, existUsers);
                return result.toString();
            }catch (Throwable e){
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                System.out.println(e);
                return null;
            }
        }
    }

    @GetMapping(value = "/api/user/{nickname}/profile")
    public User getUser(@PathVariable("nickname") String nickname, HttpServletResponse response) {
        try {
            final User user = userTemplate.getUser(nickname);
            response.setStatus(HttpServletResponse.SC_OK);
            return user;
        } catch (DataAccessException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            System.out.println(e);
            return null;
        }
    }

    @PostMapping(value = "/api/user/{nickname}/profile")
    public User updateUser(@PathVariable("nickname") String nickname, @RequestBody User user, HttpServletResponse response) {
        try {
            final User userForChange = userTemplate.getUser(nickname);
            String newEmail = null;

            try {
                if(user.getEmail() != null && userForChange.getEmail() != user.getEmail()){
                    newEmail = user.getEmail();
                    ArrayList<User> emailList = userTemplate.getUsers("", newEmail);

                    if( !emailList.isEmpty() ) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        return null;
                    }
                }

                userTemplate.updateProfile(newEmail, user.getFullname(), user.getAbout(), nickname);
                response.setStatus(HttpServletResponse.SC_OK);
                return userTemplate.getUser(nickname);
            }catch (DataAccessException error){
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                return null;
            }
        } catch (DataAccessException error1) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }
}