package it.polimi.tiw.controllers.Create;

import it.polimi.tiw.DAO.FolderDAO;
import it.polimi.tiw.DAO.SubFolderDAO;
import it.polimi.tiw.beans.Folder;
import it.polimi.tiw.beans.SubFolder;
import it.polimi.tiw.beans.User;
import it.polimi.tiw.utils.ConnectionHandler;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

@WebServlet("/CreateSubFolder")
public class CreateSubFolder extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
        ServletContext servletContext = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        boolean creationOK = true;
        //redirect to login if not logged in
        String path = getServletContext().getContextPath();
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("currentUser") == null) {
            response.sendRedirect(path);
            return;
        }

        String folderName = request.getParameter("folderName");
        String subFolderName = request.getParameter("subFolderName");


        //to repeat client side
        if (subFolderName == null || subFolderName.length() <= 3 || folderName == null || folderName.length()<=3) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Name format error");
            return;
        }

        boolean exists=true;
        FolderDAO folderDAO = new FolderDAO(connection);
        SubFolderDAO subFolderDAO = new SubFolderDAO(connection);
        //check if folder exists
        try{
            exists=folderDAO.existsFolder(((User) session.getAttribute("currentUser")).getUsername(), folderName);
        }catch(SQLException e ){
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database checking folders");
        }
        if(exists==false){
            creationOK=false;
            ctx.setVariable("FolderError", "you are creating a sub folder in an inexistent folder");
        }
        //check folder name univocity
        if(creationOK) {

            try {
                exists = subFolderDAO.existsSubFolder(((User) session.getAttribute("currentUser")).getUsername(), folderName, subFolderName);
            } catch (SQLException e) {
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database checking folders");
            }

            if(exists){
                ctx.setVariable("subFolderNameError", "you already have a sub folder with this name in this folder");
                creationOK=false;
            }
        }

        if(creationOK) {
            //insert in database and return to HomePage
            try {
                SubFolder subFolder = new SubFolder();
                subFolder.setUsername(((User) session.getAttribute("currentUser")).getUsername());
                subFolder.setFolderName(folderName);
                subFolder.setSubFolderName(subFolderName);
                subFolder.setDate(new Date());
                subFolderDAO.insertSubFolder(subFolder);
            } catch (SQLException e) {
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database update subfolders");
            }
            ctx.setVariable("creationOK", "sub folder added");
            path="/goToHomePage";
        }else{
            System.out.println("creation non ok");
            path="/WEB-INF/contentManagerPage.html";
        }
        templateEngine.process(path, ctx, response.getWriter());
    }

    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
