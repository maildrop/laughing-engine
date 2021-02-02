package net.iogilab;

import java.lang.*;
import java.util.logging.Logger;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet( name="helloServlet",
             value="/servlet/hello" )
public final class HelloWorld extends HttpServlet{
    private static final Logger logger = Logger.getLogger("net.iogilab.HelloWorld");
    public HelloWorld(){
    }
    @Override 
    public void doGet(final HttpServletRequest request , final HttpServletResponse response )
        throws IOException {
        logger.info( "hello world" );
        
        return;
    }
}
