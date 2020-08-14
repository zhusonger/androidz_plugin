package cn.com.lasong.inject;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/14
 * Description: 需要注入的方法
 */
public class InjectMethod {

    // 需要注入的类名
    public String className;
    // 需要注入的方法名
    public String methodName;
    // 方法参数签名
    public String signature;

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

    @Override
    public String toString() {
        return "Inject{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
