package cn.com.lasong.inject;

import org.gradle.api.Action;

import java.io.File;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/14
 * Description: 需要注入的方法
 */
public class InjectClzModify {

    // 需要注入的类名
    // 普通类 cn.com.lasong.base.AppManager
    // 内部类 cn.com.lasong.base.AppManager$Inner
    // 匿名类 cn.com.lasong.base.AppManager$1
    public String className;
    // 需要注入的方法名
    public String methodName;
    // 方法参数签名
    public String signature;
    // 是否注入
    public boolean isInject = true;


    public void className(Action<String> action) {
        action.execute(className);
    }
    public void methodName(Action<String> action) {
        action.execute(methodName);
    }
    public void signature(Action<String> action) {
        action.execute(signature);
    }
    public void isInject(Action<Boolean> action) {
        action.execute(isInject);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isInject() {
        return isInject;
    }

    public void setInject(boolean inject) {
        isInject = inject;
    }

    public String getEntryName() {
        if (null != className) {
            return className.replace(".", File.separator) +".class";
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"className\":\"")
                .append(className).append('\"');
        sb.append(",\"methodName\":\"")
                .append(methodName).append('\"');
        sb.append(",\"signature\":\"")
                .append(signature).append('\"');
        sb.append(",\"isInject\":")
                .append(isInject);
        sb.append('}');
        return sb.toString();
    }
}
