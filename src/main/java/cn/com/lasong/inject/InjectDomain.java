package cn.com.lasong.inject;

import org.gradle.api.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/14
 * Description: 注入插件分组
 */
public class InjectDomain {

    private final String name;
    // Type must have a public constructor that takes the element name as a parameter
    public InjectDomain(String name) {
        this.name = name;
    }

    // 分组名
    // 主要减少性能消耗, 有针对性的进行处理
    // 1.implementation方式的使用[具体的引入库], 如 cn.com.lasong:widget:0.0.2
    // 2.本地的jar或者aar使用[文件名, 不用后缀], 如 agora-rtc-sdk
    // 3.项目源码使用[:项目名称:子项目名称1:子项目名称2],
    //  3.1.如应用模块叫app, 就使用:app
    //  3.2.app模块下的子模块sub, 就使用 :app:sub

    // android.local.jars:agora-rtc-sdk.jar:d6d6fbc1426a002d1e92ffd161e4257c12816d32
    // android.local.jars:SenseArSourceManager-release-runtime.jar:12a9d3c4f421868d738d34abf42bbe7e83e4ff1c
    // cn.com.lasong:base:0.0.2
    public String group;

    // 需要注入的项
//    private List<Map<String, String>> injects;
    public List<InjectMethod> injects = new ArrayList<>();

    public void group(Action<String> action) {
        action.execute(group);
    }

    public void injects(Action<List<InjectMethod>> action) {
        action.execute(injects);
    }

    public void group(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    /**
     * 获取封装好的注入方法
     *
     * @return
     */
    @Override
    public String toString() {
        return "InjectDomain{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", injects=" + injects +
                '}';
    }
}
