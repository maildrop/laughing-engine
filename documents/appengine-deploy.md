# mavenから Google AppEngine に deploy する

```
mvn appengine:deploy
```

## projectId について
適当にprojectIdを作成した。

Google AppEngine には、人に表示するラベルとしての プロジェクト名 と 機械が識別する プロジェクトID があります。
maven appengine-maven-plugin の configuration projectId が必要とするのは後者の プロジェクトID の方です。

[プロジェクトのサイト](https://elite-firefly-302904.an.r.appspot.com/)


