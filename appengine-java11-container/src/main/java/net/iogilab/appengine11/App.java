package net.iogilab.appengine11;
/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// execute example
// java -Dorg.eclipse.jetty.LEVEL=INFO -cp target\appengine-staging\* net.iogilab.appengine11.App target/appengine-staging/webapp-1.0-SNAPSHOT.war 
//

// [START gae_java11_server]
import java.util.logging.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.webapp.WebAppContext;
//import org.eclipse.jetty.webapp.Configuration.ClassList;

// net.iogilab.appengine11.App

public final class App{
    private static final Logger logger = Logger.getLogger("net.iogilab.appengine11");
    public static void main( final String args[] ){
        System.setProperty("org.eclipse.jetty.util.log.class", 
                           System.getProperty("org.eclipse.jetty.util.log.class",
                                              "org.eclipse.jetty.util.log.StrErrLog"));
        System.setProperty("org.eclipse.jetty.LEVEL",
                           System.getProperty("org.eclipse.jetty.LEVEL",
                                              "INFO"));
        if (args.length != 1) {
            logger.severe( "Usage: need a relative path to the war file to execute" );
            return;
        }

        final org.eclipse.jetty.util.thread.QueuedThreadPool threadPool =
            new org.eclipse.jetty.util.thread.QueuedThreadPool();
        threadPool.setName("jettyServer");
        final Server server = new Server(threadPool);
        final int acceptors = -1; // default value
        final int selectors = -1; // default value

        // Create a basic Jetty server object that will listen on port defined by
        // the PORT environment variable when present, otherwise on 8080.
        final int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        final org.eclipse.jetty.server.HttpConfiguration httpConfiguration
            = new org.eclipse.jetty.server.HttpConfiguration ();
        
        final org.eclipse.jetty.server.ServerConnector serverConnector =
            new org.eclipse.jetty.server.ServerConnector( server , acceptors , selectors,
                                                          new org.eclipse.jetty.server.HttpConnectionFactory(httpConfiguration));
        serverConnector.setPort( port );
        server.addConnector( serverConnector );

        // The WebAppContext is the interface to provide configuration for a web
        // application. In this example, the context path is being set to "/" so
        // it is suitable for serving root context requests.
        final WebAppContext webapp = new WebAppContext();

        // jetty の WebAppContext を確認したところ
        // @see https://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-webapp/src/main/java/org/eclipse/jetty/webapp/WebAppContext.java
        // JettyWebXmlConfiguration と AnnotationConfiguration は既に追加されている模様なので、このコードは多分不要であると思われる。
        /*
        final ClassList classlist = ClassList.setServerDefault(server);

        // Enable Annotation Scanning.
        classlist.addBefore( "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                             "org.eclipse.jetty.annotations.AnnotationConfiguration");
        */
        
        // ドキュメントを見ると、 org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern には
        // デフォルト値が ./jetty-jakarta-servlet-api-[/]\.jar$|.jakarta.servlet.jsp.jstl-[/]\.jar|.taglibs-standard-impl-.\.jar
        // となるとか書いてあるが、これは嘘くさい （ソースコード上でそのような挙動は見つけられなかった）
        
        // apache-jstl の JspConfig.java では init() で 
        // ".*/jetty-servlet-api-[^/]*\\.jar$|.*javax.servlet.jsp.jstl-[^/]*\\.jar|.*taglibs-standard-impl-.*\\.jar"
        // が設定される模様である。
        // @see https://github.com/eclipse/jetty.project/blob/e81c847998fd99fd9a46e2bc15f191e38c2cc33d/apache-jstl/src/test/java/org/eclipse/jetty/jstl/JspConfig.java
        // が、しかし今回の Jetty11 + JSP3.0 + apache-jsp + JSTL 2.0 (JSTL1.2 on JSP3.0) の環境では、
        // 明示的に設定する必要がある。
        
        // そして、現在 JSTL 2.0(JSTL 1.2 on JSP 3.0) の実装は glassfish の jakarta.servlet.jsp.jstl-2.0.0.jar のみ
        // (apache taglibs は、 JSTL 1.2 on JSP 2.0 で互換性がない、 JSP 2.0 は javax.servlet.jsp だが 3.0 は jakarta に名前が変わっているため）

        // JSP の内容自体は、JSP3.0 で JSTL 1.2 なので、これはそのまま動くはずである。
        
        // Set the ContainerIncludeJarPattern so that jetty examines these
        // container-path jars for tlds, web-fragments etc.
        // If you omit the jar that contains the jstl .tlds, the jsp engine will
        // scan for them instead.
        webapp.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                            ".*/jetty-jakarta-servlet-api-[^/]*\\.jar|.*/jakarta.servlet.jsp.jstl-[^/]*\\.jar" );
        //".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$" );
        
        webapp.setContextPath("/");
        webapp.setWar(args[0]);

        // Set the the WebAppContext as the ContextHandler for the server.
        server.setHandler(webapp);


        /*
          Ctrl-C で SIGINT がおきて、サーバが終了使用とするときに、 Runtime() の shutdownHook が呼ばれる
          @see https://docs.oracle.com/javase/jp/7/api/java/lang/Runtime.html#addShutdownHook(java.lang.Thread)

          このシャットダウンフックで、server.stop() を呼び出さないと、
          一時ディレクトリのクリーンアップルーチンが呼ばれずそのまま残る事になる。
         */
        // Start the server! By using the server.join() the server thread will
        // join with the current thread. See
        // "http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Thread.html#join()"
        // for more details.
        try{
            class ShutdownMonitor{ // 古式の条件変数でシャットダウンを要請する。
                public boolean isContinue;
                ShutdownMonitor(){
                    this.isContinue = true;
                }
            };
            final ShutdownMonitor shutdownMonitor = new ShutdownMonitor();
            final Thread shutdownHook = new Thread( new Runnable(){
                    @Override
                    public final void run(){
                        try{
                            synchronized( shutdownMonitor ){
                                shutdownMonitor.isContinue = false;
                                shutdownMonitor.notify();
                            }
                            server.join(); // このメソッドが終了すると、JavaVM は exit() して終わってしまうので、 サーバのシャットダウンを待機する。
                        }catch( final java.lang.Exception e ){
                            logger.severe( String.valueOf( e ));
                    }
                    }
                });
            try{
                java.lang.Runtime.getRuntime().addShutdownHook( shutdownHook );
                server.start(); 
            }catch( final java.lang.Exception except ){
                // start()に失敗した場合には、例外が飛ぶので、 removeShutdownHook でシャットダウンフックを取り上げる。
                java.lang.Runtime.getRuntime().removeShutdownHook( shutdownHook );
                throw except;
            }
            synchronized( shutdownMonitor ){
                while( shutdownMonitor.isContinue ){
                    shutdownMonitor.wait();
                }
            }
            server.stop();
            server.join();
        }catch( final java.lang.InterruptedException ie ){
            logger.severe( String.valueOf( ie ));
        }catch( final java.lang.Exception except ){
            logger.severe( except.toString() );
        }
        return;
    }
}
