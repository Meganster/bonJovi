package forumdb.Controller;


import forumdb.DAO.ServiceDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ServiceController {
    @Autowired
    private ServiceDAO dbService;

    @GetMapping("/api/service/status")
    public ResponseEntity<?> getStatus() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(dbService.getStatus());
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/api/service/clear")
    public ResponseEntity<?> clearDatabase() {
        try {
            dbService.clear();
            return ResponseEntity.status(HttpStatus.OK).body(null);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
