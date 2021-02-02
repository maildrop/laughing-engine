# laughing-engine
Example of Jetty 11 on google AppEngine Java11 environment with Servlet5.0 JSP3.0 JSTL1.2 

# はじめに
Google App Engine - Java 11 スタンダード環境は、それまでの Java 8 ランタイムとは異なった仕組みで動作するようになり、
[移行のためのドキュメント](https://cloud.google.com/appengine/docs/standard/java11/java-differences)
が公開されている。

2021年1月現在のJetty最新バージョン系列Jetty 11である。しかしながら、当該のドキュメントは、Jetty 9を利用した方法であるので
Jetty 11系列を使用した、環境を構築したい。

## 問題点
- Servlet,JSP API のパッケージ名の変更

Servlet API と JSP API は、パッケージ名が、javax.servlet から jakarta.servlet に変更になっている。
このため、JSTL のバージョンは 1.2 のままで変更が無いが、Servlet 5.0 , JSP 3.0 環境に対応する、JSTL実装が必要である。
（便宜上、Servlet 5.0 , JSP 3.0 , javax.servlet から jakaruta.servlet へ変更になった JSTL を JSTL 2.0 と呼称することにする。
glassfish の実装バージョン番号が 2.0 になっているため）

- Google の custom-entrypoint の コードが Jetty 9 用であり、 Jetty 11ではコンパイルが出来ない。

組み込み用Jetty なので、バージョンによる非互換性の部分
[App.java](https://github.com/maildrop/laughing-engine/blob/main/appengine-java11-container/src/main/java/net/iogilab/appengine11/App.java) を作成したので、そちらを確認。

## maven の pom.xml の依存環境に関する調査
以下はEndpointのコンテナの話である。
Servlet APIに関しては、
```
    <dependency>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>jetty-servlet</artifactId>
      <version>${org.eclipse.jetty.version}</version>
    </dependency>
```
が保持している。

Jetty の JSP 部分に関しては apache-jsp が使用されているので
```
    <!-- jetty の JSP 実装を apache-jsp に任せる -->
    <!-- extra explicit dependency needed because there is a JSP in the sample-->
    <dependency>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>apache-jsp</artifactId>
      <version>${org.eclipse.jetty.version}</version>
    </dependency>
```
が対応している。JSTL の API は
```
<!-- javax. から、 jakarta に名前空間が変わったので、JSTL に関しては、Servlet5.0/JSP 3.0 以後はこちらを使う -->
    <!-- これは JSTL の API jarパッケージ -->
    <!-- https://mvnrepository.com/artifact/jakarta.servlet.jsp.jstl/jakarta.servlet.jsp.jstl-api -->
    <dependency>
      <groupId>jakarta.servlet.jsp.jstl</groupId>
      <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
      <version>2.0.0</version>
    </dependency>
```
JSTL の 実装は
```
    <!-- これは JSTL の implementation jar パッケージ -->
    <!-- https://mvnrepository.com/artifact/org.glassfish.web/jakarta.servlet.jsp.jstl -->
    <dependency>
      <groupId>org.glassfish.web</groupId>
      <artifactId>jakarta.servlet.jsp.jstl</artifactId>
      <version>2.0.0</version>
    </dependency>
```

webapp や、servlet の実装に関してはこれまで通りベアな servlet-api , jsp-api を provide で使用すればよい。
ただし、package名が、jakarta.servletに変更になっているのは注意が必要である。

## [App.java](https://github.com/maildrop/laughing-engine/blob/main/appengine-java11-container/src/main/java/net/iogilab/appengine11/App.java)

### 概要

主な変更点は ClassList の部分であるが、これは本体に吸収されているので削除された。

他方 (JSTLを、コンテナにもたせるために必要な）コンテナのTLDファイルの読み込みが必要なファイルの正規表現パターンである、 org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern には、
```".*/jetty-jakarta-servlet-api-[^/]*\\.jar|.*/jakarta.servlet.jsp.jstl-[^/]*\\.jar"``` を指定している。
この変数 Jetty のドキュメントではデフォルト値があるように書かれているが、Embeded Jetty では、そのようなデフォルト値はどうも設定されていない模様である。
また、JSP 3.0 への移行に当たって apache taglibs 1.2 は、javax.servlet の参照を持っているために動作しない。今回は glassfish の実装である
```
    <dependency>
      <groupId>org.glassfish.web</groupId>
      <artifactId>jakarta.servlet.jsp.jstl</artifactId>
      <version>2.0.0</version>
    </dependency>
```
を dependencies に加えて、利用するようにした。これ自体は、JSTL 1.2 の実装を Servlet5.0 ,JSP 3.0 上で動作するようにしたもののようである。
なお、この変更を行ってglassfish の jakarta.servlet.jsp.jstl 2.0を使用するようにしても、当該の実装そのものは JSTL 1.2 であるので、
JSPファイルの taglib uri属性の変更は不要である。（はず）

### デバッグ（ローカル実行）環境での停止問題
デバッグ環境で停止させるため`Ctrl-C` SIGINT で割り込みをかけると、即時サーバが停止されて tmpディレクトリに、warファイルの展開とjspファイルのコンパイルのための一時ファイルがのこってしまう。
このために、java.lang.Runtime#addShutdownHook() で、server.stop() を呼びだして、サーバが停止するのを join() で待機するようにコードを追加した。
この変更で、デバッグ環境を Ctrl-C で停止させても、一時ディレクトリに置かれたファイルのクリーンアップがなされる。



