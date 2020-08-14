package cn.com.lasong.inject;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/14
 * Description: 需要注入的每一项
 */
public class Inject {

    // injectName: cn.com.lasong:widget:0.0.2/ agora-rtc-sdk/ SenseArSourceManager-release
    // injectClz: com.hitomi.tilibrary.transfer.TransferAdapter
    // injectMethod:

    // 需要注入的类名
    public String className;
    // 需要注入的方法名
    public String methodName;
    // 方法参数签名
    public String signature;

    @Override
    public String toString() {
        return "Inject{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
