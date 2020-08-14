package cn.com.lasong.time;

import org.gradle.BuildAdapter;
import org.gradle.BuildResult;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionAdapter;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.com.lasong.utils.PluginHelper;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description: 打印任务耗时, 排查打包耗时的任务
 */
public class TaskTimePlugin implements Plugin<Project> {

    Map<String, TaskTime> timeMap = new LinkedHashMap<>();

    @Override
    public void apply(@NotNull Project project) {
        // 只处理应用和库模块
        if (!PluginHelper.isAppOrLibrary(project)) {
            return;
        }

        // 拼装项目名称
        StringBuilder builder = new StringBuilder();
        builder.append(":").append(project.getName());
        Project parent = project.getParent();
        while (PluginHelper.isAppOrLibrary(parent)) {
            builder.insert(0, parent.getName()+":");
            parent = parent.getParent();
        }
        String name = builder.toString();


        Gradle gradle = project.getGradle();

        //监听每个task的执行
        gradle.addListener(new TaskExecutionAdapter() {
            @Override
            public void beforeExecute(Task task) {
                TaskTime time = new TaskTime();
                time.startMs = System.currentTimeMillis();
                time.name = task.getPath();
                timeMap.put(time.name, time);
            }

            @Override
            public void afterExecute(Task task, TaskState taskState) {
                String name = task.getPath();
                if (!timeMap.containsKey(name)) {
                    return;
                }
                TaskTime time = timeMap.get(name);
                time.endMs = System.currentTimeMillis();
                time.costMs = time.endMs - time.startMs;
            }
        });
        // build 结束, 打印结果
        gradle.addBuildListener(new BuildAdapter() {
            @Override
            public void buildFinished(BuildResult buildResult) {
                if (timeMap.isEmpty()) {
                    return;
                }
                PluginHelper.println(name, "============================");
                PluginHelper.println(name, "========> TimeCost <========");
                Collection<TaskTime> values = timeMap.values();
                List<TaskTime> list = new LinkedList<>(values);
                Collections.sort(list, (taskL, taskR) -> (int) -(taskL.costMs - taskR.costMs));
                for (TaskTime taskTime : list) {
                    PluginHelper.println(name, taskTime.name+" ["+taskTime.costMs+"ms]");
                }
                PluginHelper.println(name, "============================");
            }
        });
    }
}
