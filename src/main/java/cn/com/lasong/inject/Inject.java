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

    private final String name;

    public Inject(String name) {
        this.name = name;
    }
    // 需要注入的名称
    public String clzName;

    public String methodName;

    @Override
    public String toString() {
        return "Inject{" +
                "name='" + name + '\'' +
                ", clzName='" + clzName + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
