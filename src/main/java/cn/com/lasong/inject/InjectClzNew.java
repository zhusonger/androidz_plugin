package cn.com.lasong.inject;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/18
 * Description: 创建新的类
 */
public class InjectClzNew {

    // 需要注入的类名
    // 普通类 cn.com.lasong.base.AppManager
    // 内部类 cn.com.lasong.base.AppManager$Inner
    // 匿名类 cn.com.lasong.base.AppManager$1
    public String className;


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"className\":\"")
                .append(className).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
