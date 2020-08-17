package cn.com.lasong.inject;

import org.gradle.api.Action;

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

    public void className(Action<String> action) {
        action.execute(className);
    }
    public void methodName(Action<String> action) {
        action.execute(methodName);
    }
    public void signature(Action<String> action) {
        action.execute(signature);
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
