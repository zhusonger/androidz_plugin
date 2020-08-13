package cn.com.lasong.utils;

import com.android.build.api.transform.QualifiedContent;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description: 固定常量
 */
public class Constants {
    public static final Set<QualifiedContent.ContentType> CLASSES = new HashSet<>();
    public static final Set<QualifiedContent.ContentType> RESOURCES = new HashSet<>();
    static {
        CLASSES.add(QualifiedContent.DefaultContentType.CLASSES);
        RESOURCES.add(QualifiedContent.DefaultContentType.RESOURCES);
    }
}
