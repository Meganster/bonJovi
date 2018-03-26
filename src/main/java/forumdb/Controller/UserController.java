package forumdb.Controller;

import forumdb.DAO.UserDAO;

import forumdb.Model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

@RestController
public class UserController {
    @Autowired
    private UserDAO userTemplate;


    @PostMapping(value = "/api/user/{nickname}/create")
    public ResponseEntity<?> createUser(@PathVariable("nickname") String nickname, @RequestBody User user) {
        try {
            userTemplate.create(user.getEmail(), nickname, user.getFullname(), user.getAbout());

            return ResponseEntity.status(HttpStatus.CREATED).body(userTemplate.getUser(nickname));
        } catch (DataAccessException error) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(userTemplate.getUsers(nickname, user.getEmail()));
        }
    }

    @GetMapping(value = "/api/user/{nickname}/profile")
    public ResponseEntity<?> getUser(@PathVariable("nickname") String nickname) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userTemplate.getUser(nickname));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find user by nickname: " + nickname));
        }
    }

    @PostMapping(value = "/api/user/{nickname}/profile")
    public ResponseEntity<?> updateUser(@PathVariable("nickname") String nickname, @RequestBody User user) {
        try {
            final User userForChange = userTemplate.getUser(nickname);
            String newEmail = null;

            try {
                if (user.getEmail() != null && !userForChange.getEmail().equals(user.getEmail())) {
                    newEmail = user.getEmail();
                    final ArrayList<User> emailList = userTemplate.getUsers("", newEmail);

                    if (!emailList.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error("Can't find user by nickname: " + nickname));
                    }
                }

                userTemplate.updateProfile(newEmail, user.getFullname(), user.getAbout(), nickname);
                return ResponseEntity.status(HttpStatus.OK).body(userTemplate.getUser(nickname));
            } catch (DataAccessException error) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error("Can't find user by nickname: " + nickname));
            }
        } catch (DataAccessException error1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("Can't find user by nickname: " + nickname));
        }
    }
}