# Plugin
插件库

# 0. 引入

```
// 根目录build.gradle
buildscript {
    dependencies {
        classpath "cn.com.lasong:plugin:latest_version"
    }
}
```

# 1. 功能插件

## 1.1 Inject插件

### 功能

在生成最终应用包前可以对本地jar/aar包、implementation方式引入的三方库、源码进行字节码的修改

包括新增方法、新增属性、修改方法。

### 依赖
本插件基于[javassist](https://www.javassist.org/)以及Transform实现


### CHANGELOG

* 0.0.2

默认自动导入所有的包名, 避免繁复的加入包名的问题

* 0.0.3

去除 __addFields__ 和 __addMethods__ 属性, 统一都在modifyMethods数组中。

新增action配置, 值如下, 默认值是 __修改(MODIFY)__ 行为

```
public static final String ACTION_MODIFY = "MODIFY";
public static final String ACTION_ADD_FIELD = "ADD_FIELD";
public static final String ACTION_ADD_METHOD = "ADD_METHOD";
public static final String ACTION_DEFAULT = ACTION_MODIFY;
```

主要是考虑修改是一环扣一环的, 如果分开可能无法实现后面的修改依赖之前的修改。

按照数组的顺序执行, 可以实现后面的代码应用之前的修改。

* 0.0.4

修改类的修饰符

### 使用

```groovy
// 应用插件
apply plugin: 'cn.com.lasong.inject'

allInjects {
    // 是否开启debug 目前主要打印日志
    injectDebug true
    // 注入的节点, 区分注入的对象
    injectDomains {
        // 远程库
        base {
            // 注入的库名
            // 主要减少性能消耗, 有针对性的进行处理
            // 1.implementation方式的使用[具体的引入库], 如 cn.com.lasong:widget:0.0.2
            // 2.本地的jar或者aar使用[文件名, 不用后缀], 如 agora-rtc-sdk
            // 3.项目源码使用[:项目名称:子项目名称1:子项目名称2],
            //  3.1.如应用模块叫app, 就使用:app
            //  3.2.app模块下的子模块sub, 就使用 :app:sub
            // 具体使用参考下面的
            group "cn.com.lasong:base:0.0.2"
            // 新增字节码文件夹路径
            clzNewDir "classes/base"
        }

        // 本地jar包
        agora {
            group "agora-rtc-sdk"
            clzNewDir "classes/agora"
            // 需要修改的字节码包
            clzModify = [
                    [
                            // 注入的类, 具体到内部类的话需要加上完整包名
                            // 如 io.agora.rtc.video.CameraHelper$Capability
                            className     : 'io.agora.rtc.internal.RtcEngineImpl',
                            // 是否注入, 默认true
                            isInject : false,
                            // 修改类的修饰符
                            modifiers: "public",
                            // 代码关联的类需要导入的包, 默认引入的库都会导入, 可根据需要再添加
                            importPackages: [
                                    "java.io",
                                    "android.util.Log",
                                    "cn.com.lasong",
                                    "io.agora.rtc.internal"
                            ]
                            // 修改方法
                            modifyMethods : [
                                    [
                                            // 方法名
                                            name   : "checkVoipPermissions",
                                            // 方法参数签名
                                            params : "(Landroid.content.Context;I)",
                                            // 方法的内容, 参数的使用跟javassist一致
                                            content: """Log.e("Test", "checkVoipPermissions");""",
                                            // 这里的类型方法跟javassist一致
                                            // insertBefore : 在方法的起始位置插入代码；
                                            // insertAfter : 在方法的所有 return 语句前插入代码以确保语句能够被执行，除非遇到exception；
                                            // insertAt : 在指定的位置插入代码；
                                            // setBody : 将方法的内容设置为要写入的代码，当方法被 abstract修饰时，该修饰符被移除；
                                            type   : "insertBefore",
                                            // lineNum只有在insertAt时生效
                                            lineNum : 1
                                    ],
                                    [
                                            name     : "pullPlaybackAudioFrame",
                                            params   : "([BI)",
                                            modifiers: "private",
                                            content  :
                                                    """
                                                    Log.e("Test", "pullPlaybackAudioFrame"+ \$2);
                                                    """,
                                            type     : "insertBefore"
                                    ],
                                    [
                                            action : "ADD_METHOD",
                                            """
                                            public RtcEngineImpl addMethod() {
                                                RtcEngineImpl aaa = null;
                                                intValue = 1;
                                                booleanValue = true;
                                                stringValue = "addMethod";
                                                System.out.println("addMethod");
                                                return aaa;
                                            }
                                            """
                                    ],
                                     [
                                             action : "ADD_FIELD",
                                             "public String stringValue;"
                                     ]

                            ]
                    ]
            ]
        }

        // 本地项目, 只能注入到当前应用插件的项目
        // 比如这里设置依赖的本地库 :base, 这里是不会处理的
        // 优先级是被依赖的项目先进行注入, 再注入当前的项目
        app {
            group ":app"
            clzNewDir "classes/app"
            clzModify = [
                    [
                            className     : 'cn.com.lasong.MainActivity',
                            importPackages: [
                                    "java.io",
                                    "android.util.Log",
                                    "cn.com.lasong"
                            ],
                            modifyMethods : [
                                    [
                                            name   : "onBackPressed",
                                            params : "()",
                                            content: """Log.e("Test", "onBackPressed"); addMethod(); addMethod_jhdfsadf();""",
                                            type   : "insertBefore"
                                    ]
                            ]

                    ]
            ]
        }
    }
}
```


## 1.2 Time插件

### 功能

统计gradle每个任务的耗时情况, 并按照耗时从长到短降序排列

为了找出打包慢的主要原因


### 使用

```groovy
// 应用插件
apply plugin: 'cn.com.lasong.time'
```

# FAQ:

## Q:

setBody设置的方法类型丢失, 比如写的代码是

```
Log.d("Test", "getImageItem");
FrameLayout parentLayout = containLayoutArray.get(\$1);
if (parentLayout != null && parentLayout.getChildAt(0) instanceof TransferImage) {
    return ((TransferImage) parentLayout.getChildAt(0));
}
return null;
```

但是最后编译之后的代码

```
Log.d("Test", "getImageItem");
Object object = this.containLayoutArray.get(paramInt);
return (object == null || !(object.getChildAt(0) instanceof TransferImage)) ? null : (TransferImage)object.getChildAt(0);
}
```

FrameLayout类型丢失了。

## A:

<https://www.javassist.org/tutorial/tutorial3.html>

Note that no type parameters are necessary. Since get returns an Object,
__an explicit type cast__ is needed at the caller site if the source code is compiled by Javassist.
For example, if the type parameter T is String,
then (String) must be inserted as follows:

```
  Wrapper w = ...
  String s = (String)w.get();
```

The type cast is not needed if the source code is compiled by a normal Java compiler because it will automatically insert a type cast.

意思就是源码通过普通java编译器会自动转换, 但是通过 Javassist 只能显式的强制转换
添加上强制转换的代码即可。

```
Log.d("Test", "getImageItem");
FrameLayout parentLayout = (FrameLayout) containLayoutArray.get(\$1);
if (parentLayout != null && parentLayout.getChildAt(0) instanceof TransferImage) {
    return ((TransferImage) parentLayout.getChildAt(0));
}
return null;
```

## Q:

出现IncompatibleClassChangeError错误, 信息关键字是

__was expected to be of type direct but instead was found to be of type virtual__

## A:

这可能是因为修改了原来的方法, 但是方法的范围修改了

比如下面的错误是我把 __newParentLayout__ 的修饰符从原来的 __private__ 改为了 __public__

java.lang.IncompatibleClassChangeError: The method 'android.widget.FrameLayout com.hitomi.tilibrary.transfer.TransferAdapter.newParentLayout(android.view.ViewGroup, int)'
was expected to be of type direct but instead was found to be of type virtual (declaration of 'com.hitomi.tilibrary.transfer.TransferAdapter'
appears in /data/app/cn.com.lasong-f585zSw-N26AiOoC6yrWkA==/base.apk)

## Q:

在修改的过程中, 出现奇怪的缺失 )、;等符号

## A:

因为javassist内的java编译器的限制, 会导致有些代码编译不过。

In the current implementation, the Java compiler included in Javassist has several limitations with respect to the language that the compiler can accept. Those limitations are:

1. The new syntax introduced by J2SE 5.0 (including enums and generics) has not been supported. Annotations are supported by the low level API of Javassist. See the javassist.bytecode.annotation package (and also getAnnotations() in CtClass and CtBehavior). Generics are also only partly supported. See the latter section for more details.

    > 不支持J2SE 5.0引入的新语法（包括枚举和泛型）。
    >
    > 泛型部分支持, 泛型编译完之后实际上是具体类型。
    >
    > 具体说明 : <https://www.javassist.org/tutorial/tutorial3.html#generics>


2. Array initializers, a comma-separated list of expressions enclosed by braces { and }, are not available unless the array dimension is one.

    > 用大括号实现的数组初始化不支持, 除非只有一个元素

3. Inner classes or anonymous classes are not supported. Note that this is a limitation of the compiler only. It cannot compile source code including an anonymous-class declaration. Javassist can read and modify a class file of inner/anonymous class.

    > 不支持内部类或匿名类。
    >
    > 请注意，这仅是编译器的限制。
    >
    > 它不能编译包括匿名类声明的源代码。
    >
    > Javassist可以读取和修改内部/匿名类的类文件。

4. Labeled continue and break statements are not supported.

    > continue和break不支持

5. The compiler does not correctly implement the Java method dispatch algorithm. The compiler may confuse if methods defined in a class have the same name but take different parameter lists.
For example,

    ```java
    class A {}
    class B extends A {}
    class C extends B {}

    class X {
        void foo(A a) { .. }
        void foo(B b) { .. }
    }
    ```
If the compiled expression is x.foo(new C()), where x is an instance of X, the compiler may produce a call to foo(A) although the compiler can correctly compile foo((B)new C()).


    > 编译器没有正确地实现Java方法分派算法。
    >
    > 如果类中定义的方法具有相同的名称但具有不同的参数列表，编译器可能会混淆。
    >
    > 如果编译表达式是x.foo(new C())，其中x是x的一个实例，
    >
    > 尽管编译器可以正确地编译foo((B)new C())，
    >
    > 但是编译器可能会产生一个对foo(A)的调用。

6. The users are recommended to use # as the separator between a class name and a static method or field name. For example, in regular Java,
javassist.CtClass.intType.getName()
calls a method getName() on the object indicated by the static field intType in javassist.CtClass. In Javassist, the users can write the expression shown above but they are recommended to write:

    javassist.CtClass#intType.getName()
    so that the compiler can quickly parse the expression.

    > 建议在静态方法/变量之前使用 __#__ 修饰, 这样编译器可以更快的解析表达式。