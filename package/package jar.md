## 打jar包
>### 普通jar包
1. javac 要编译的文件 -d 目标位置
2. jar -cvfm 命名 MENIFEST文件 要打包的文件1 要打包的文件2
*可表示所有文件
主清单属性 `META-INF\MENIFEST.MF  Main-Class: Hello`
```
javac Hello.java -d target
jar -cvfm hello.jar META-INF\MENIFEST.MF *
```
* https://www.cnblogs.com/flashsun/p/7246260.html
>### jar包中含有jar包
1. `java -Xbootclasspath/a: tom.jar: -jar hello.jar`[可能会影响其他应用]
2. `META-INF\MENIFEST.MF  Class-Path: lib/some.jar lib/some2.jar`
3. [自定义Classloader来加载](http://cuixiaodong214.blog.163.com/blog/static/951639820099135859761)
* https://www.cnblogs.com/adolfmc/archive/2012/10/07/2713562.html

