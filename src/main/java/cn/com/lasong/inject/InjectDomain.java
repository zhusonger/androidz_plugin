package cn.com.lasong.inject;

import org.gradle.api.Action;

import java.util.Arrays;
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
    public String group;

    // 新增字节文件目录
    public String clzNewDir;

    // 修改字节文件列表
    public List<InjectClzModify> clzModify;

    public void group(Action<String> action) {
        action.execute(group);
    }

    public void clzModify(Action<List<InjectClzModify>> action) {
        action.execute(clzModify);
    }

    public void clzNewDir(Action<String> action) {
        action.execute(clzNewDir);
    }

    public void group(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public List<InjectClzModify> getClzModify() {
        return clzModify;
    }

    public void setClzModify(InjectClzModify[] clzModify) {
        this.clzModify = Arrays.asList(clzModify);
    }

    public String getClzNewDir() {
        return clzNewDir;
    }

    public void clzNewDir(String clzNewDir) {
        this.clzNewDir = clzNewDir;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"")
                .append(name).append('\"');
        sb.append(",\"group\":\"")
                .append(group).append('\"');
        sb.append(",\"clzModify\":")
                .append(clzModify);
        sb.append(",\"clzNewDir\":")
                .append(clzNewDir);
        sb.append('}');
        return sb.toString();
    }
}
