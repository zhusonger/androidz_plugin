package cn.com.lasong.inject;

import org.gradle.api.NamedDomainObjectContainer;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/14
 * Description: 需要处理的分组
 */
public class InjectExtension {

    private final String name;

    public InjectExtension(String name) {
        this.name = name;
    }

    // 是否是debug, 会输出信息
    public boolean isDebug = false;
    // 分组名
    // 主要减少性能消耗, 有针对性的进行处理
    // 1.implementation方式的使用[具体的引入库], 如 cn.com.lasong:widget:0.0.2
    // 2.本地的jar或者aar使用[文件名, 不含后缀], 如 agora-rtc-sdk
    // 3.项目源码使用[:项目名称:子项目名称1:子项目名称2],
    //  3.1.如应用模块叫app, 就使用:app
    //  3.2.app模块下的子模块sub, 就使用 :app:sub
    public String group;

    // 需要注入的项
    public NamedDomainObjectContainer<Inject> injects;
}
