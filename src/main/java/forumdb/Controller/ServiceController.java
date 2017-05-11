package forumdb.Controller;


import forumdb.DAO.ServiceDAO;
import forumdb.Model.InfoAboutDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;


@RestController
public class ServiceController {
    @Autowired
    private ServiceDAO serviceTemplate;

    @GetMapping("/api/service/status")
    public InfoAboutDB getStatus(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            return serviceTemplate.getStatus();
        }catch (DataAccessException e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            System.out.println(e);
            return null;
        }
    }

    @PostMapping("/api/service/clear")
    public void clearDatabase(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            serviceTemplate.clear();
        }catch (DataAccessException e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            System.out.println(e);
        }
    }
}
