package cn.com.lasong.inject;

import org.apache.commons.beanutils.BeanUtils;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.util.ConfigureUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.com.lasong.utils.PluginHelper;
import groovy.lang.Closure;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/14
 * Description: 注入插件分组
 */
public class InjectExtension {

    private final String name;
    // Type must have a public constructor that takes the element name as a parameter
    public InjectExtension(String name) {
        this.name = name;
    }

    // 分组名
    // 主要减少性能消耗, 有针对性的进行处理
    // 1.implementation方式的使用[具体的引入库], 如 cn.com.lasong:widget:0.0.2
    // 2.本地的jar或者aar使用[文件名], 如 agora-rtc-sdk.jar
    // 3.项目源码使用[:项目名称:子项目名称1:子项目名称2],
    //  3.1.如应用模块叫app, 就使用:app
    //  3.2.app模块下的子模块sub, 就使用 :app:sub
    private String group;

    // 需要注入的项
    private List<Map<String, String>> injects;
    private List<InjectMethod> injectMethods = new ArrayList<>();

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<Map<String, String>> getInjects() {
        return injects;
    }

    public void setInjects(List<Map<String, String>> injects) {
        this.injects = injects;
    }

    /**
     * 获取封装好的注入方法
     * @return
     */
    public List<InjectMethod> injectMethods() {
        if (null == injects) {
            return null;
        }
        if (!injectMethods.isEmpty()) {
            return injectMethods;
        }

        try {
            for (Map<String, String> item : injects) {
                InjectMethod method = new InjectMethod();
                BeanUtils.populate(method, item);
                injectMethods.add(method);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return injectMethods;
    }

    @Override
    public String toString() {
        return "InjectExtension{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", injectMethods=" + injectMethods +
                '}';
    }
}
