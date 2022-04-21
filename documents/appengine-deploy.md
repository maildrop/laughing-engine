# mavenから Google AppEngine に deploy する

gloud で認証情報やプロジェクトID を設定した後、ディレクトリ appengine-java11 で
```
mvn appengine:deploy
```
を実行する

gcloud 自体が無い時にはとりあえず ```mvn appengine:deploy``` で gloud 自体をダウンロードして、
```~/.cache/google-cloud-tools-java/managed-cloud-sdk/LATEST/google-cloud-sdk/bin/gcloud``` にある gcloudを使う事も出来る。

## projectId について
適当にprojectIdを作成した。

Google AppEngine には、人に表示するラベルとしての プロジェクト名 と 機械が識別する プロジェクトID があります。
maven appengine-maven-plugin の configuration projectId が必要とするのは後者の プロジェクトID の方です。

[プロジェクトのサイト](https://elite-firefly-302904.an.r.appspot.com/)


