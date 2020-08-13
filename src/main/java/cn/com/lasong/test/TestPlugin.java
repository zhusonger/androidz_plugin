package cn.com.lasong.test;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description: 注入插件
 */
public class TestPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("TestPlugin");
    }
}
