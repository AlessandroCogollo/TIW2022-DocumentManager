package it.polimi.tiw.controllers;

import it.polimi.tiw.DAO.UserDAO;
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
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/Subscribe")
public class Subscribe extends HttpServlet {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String name = request.getParameter("name");
        String password1 = request.getParameter("password1");
        String password2 = request.getParameter("password2");
        boolean registationOK=true;
        UserDAO dao = new UserDAO(connection);
        String path =null;
        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        //to repeat client side
        if(username == null || username.length()<=3 ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username too short");
            return;
        }
        if(email == null || email.length()<=3 || !email.contains("@")){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email format error");
            return;
        }
        if(name == null || name.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Name format error");
            return;
        }
        if(password1 == null || password1.length()<=3 ){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password format error");
            return;
        }
        if(password1!=null && !password1.equals(password2)){
            ctx.setVariable("passwordDifferentError", "passwords have different values");
            registationOK=false;
        }

        boolean exists=true;
        //check username
        if(registationOK) {
            try {

                exists = dao.existsUser(username);
            } catch (SQLException e) {
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database checking username");
            }
            //there is an other user with same username
            if(exists){
                ctx.setVariable("usernameError", "username already in use");
                registationOK=false;
            }
        }
        //check email
        if(registationOK){
            try {
                exists = dao.existsEmail(email);
            } catch (SQLException e) {
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database checking email");
            }
            //there is an other user with same username
            if(exists){
                ctx.setVariable("emailError", "email already in use");
                registationOK=false;
            }
        }
        if(registationOK){
            //insert in database and return to login
            try{
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setName(name);
                dao.createUser(user,password1);
            }catch(SQLException e){
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database subscribing");
                return;
            }
            path = "/loginPage.html";
            ctx.setVariable("registationOK", "REGISTRATION COMPLETED");
        }else{
            //return to subscribePage
            path = "/subscribePage.html";
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
