package cn.com.lasong.inject;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.BaseExtension;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import cn.com.lasong.utils.PluginHelper;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;

public class InjectHelper {

    //初始化类池
    protected static ClassPool pool;
    // 加入类路径的记录, 方便修改
    protected final static Map<String, ClassPath> clzPaths = new HashMap<>();
    // 记录加入的包名
    protected final static Set<String> pkgNames = new HashSet<>();

    /**
     * 添加javassist类搜索路径
     *
     * @param path
     * @return
     */
    public synchronized static void appendClassPath(String tag, String path) throws RuntimeException {
        if (null == pool) {
            pool = new ClassPool(true);
        }
        removeClassPath(tag);
        try {
            ClassPath classPath = pool.appendClassPath(path);
            clzPaths.put(tag, classPath);
            String lower = path.toLowerCase();
            // jar包
            if (lower.endsWith(".jar") || lower.endsWith(".zip")) {
                JarFile jarFile = new JarFile(path);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry srcEntry = entries.nextElement();
                    String entryName = srcEntry.getName();
                    importPackageNameByEntry(entryName);
                }
                jarFile.close();
            } else {
                File clzDir = new File(path);
                String clzDirPath = clzDir.getAbsolutePath() + File.separator;
                List<File> clzFile = listClasses(clzDir);
                if (!clzFile.isEmpty()) {
                    File file = clzFile.get(0);
                    String entryName = file.getAbsolutePath().replace(clzDirPath, "");
                    importPackageNameByEntry(entryName);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 移除上一次的classpath
     */
    public synchronized static void clearClassPath() {
        // 结束后移除这个classpool, 用同一个会有问题
        pool = null;
        clzPaths.clear();
        pkgNames.clear();
    }


    /**
     * 导入包名
     */
    private static void importPackageNameByEntry(String entryName) {
        if (entryName == null || !entryName.endsWith(".class")) {
            return;
        }
        int end = entryName.lastIndexOf("/");
        if (end <= 0) {
            return;
        }
        String packageName = entryName.substring(0, end).replace("/", ".");
        importPackageName(packageName);
    }
    private static void importPackageName(String packageName) {
        if (!pkgNames.contains(packageName)) {
            pool.importPackage(packageName);
            pkgNames.add(packageName);
        }
    }

    /**
     * 移除tag对应的classpath
     *
     * @param tag
     */
    public static void removeClassPath(String tag) {
        ClassPath cachePath = clzPaths.get(tag);
        if (null != cachePath) {
            pool.removeClassPath(cachePath);
        }
    }


    /**
     * 准备环境
     * 先把所有jar包和源码都加入到
     */
    public static void prepareEnv(String group, BaseExtension android, Collection<TransformInput> inputs) throws RuntimeException {
        // 清楚缓存
        clearJarFactoryCache();

        List<File> boot = android.getBootClasspath();
        if (null != boot && !boot.isEmpty()) {
            for (File jarFile : boot) {
                String name = jarFile.getName();
                // 加入相关类，不然找不到相关的所有类
                InjectHelper.appendClassPath(name, jarFile.getAbsolutePath());
            }
        }

        for (TransformInput input : inputs) {
            // 遍历输入，分别遍历其中的jar以及directory
            for (JarInput jarInput : input.getJarInputs()) {
                String name = jarInput.getName();
                String[] nameArr = name.split(":");
                String artifact = nameArr[1];
                boolean isLocalJar = artifact.endsWith(".jar");
                String tag = isLocalJar ? artifact : name;

                String path = jarInput.getFile().getAbsolutePath();
                // 添加jar包路径
                appendClassPath(tag, path);
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                String path = directoryInput.getFile().getAbsolutePath();
                // 添加源码路径
                appendClassPath(group, path);
            }
        }
    }

    /**
     * 处理三方的库
     *
     * @param group
     * @param input
     * @param extension
     * @param outputProvider
     * @param proDir
     * @throws IOException
     */
    public static void transformJar(String group, JarInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {
        //对jar文件进行处理
        // android.local.jars:agora-rtc-sdk.jar:d6d6fbc1426a002d1e92ffd161e4257c12816d32
        // android.local.jars:SenseArSourceManager-release-runtime.jar:12a9d3c4f421868d738d34abf42bbe7e83e4ff1c
        // cn.com.lasong:base:0.0.2
        String name = input.getName();
        String[] nameArr = name.split(":");
        String artifact = nameArr[1];
        boolean isLocalJar = artifact.endsWith(".jar");
        String tag = isLocalJar ? artifact : name;
        // build/tmp/xxx(任务名)
        File tmpDir = context.getTemporaryDir();

        File dstFile = outputProvider.getContentLocation(input.getName(), input.getContentTypes(),
                input.getScopes(), Format.JAR);

        File srcFile = input.getFile();


        if (null != extension) {
            Set<InjectDomain> set = extension.injectDomains;
            InjectDomain injectDomain = null;
            for (InjectDomain domain : set) {
                if (domain.group == null || domain.group.isEmpty()) {
                    continue;
                }
                // 需要注入的jarInput
                // 1. 非本地库 group完全匹配
                // 2. 本地库 group跟
                if ((!isLocalJar && name.equals(domain.group))
                        || (isLocalJar && name.contains(domain.group))) {
                    injectDomain = domain;
                    break;
                }
            }

            // 注入
            if (null != injectDomain) {

                // 添加jar包路径
//                appendClassPath(tag, srcFile.getAbsolutePath());
                File srcTmpFile = new File(tmpDir, srcFile.getName());

                if (extension.injectDebug) {
                    PluginHelper.println(group, "========> Inject Begin transformJar [" + tag + "] <========");
                    PluginHelper.println(group, "name = " + name);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                }

                JarFile srcJar = new JarFile(srcFile);
                JarOutputStream srcJarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(srcTmpFile)));
                try {
                    // 1. create class
                    String clzNewDir = injectDomain.clzNewDir;
                    if (null != clzNewDir && !clzNewDir.isEmpty()) {
                        File clzDir = new File(proDir, clzNewDir);
                        File dstDir = new File(tmpDir, clzNewDir);
                        // 先复制到临时目录
                        FileUtils.copyDirectory(clzDir, dstDir);
                        // 添加临时路径
                        appendClassPath(tag + "_clzNewDir", dstDir.getAbsolutePath());
                        // 移除缓存文件
                        cleanJavacCache(dstDir, srcFile);
                        // 修改字节码
                        writeClassToSource(group, injectDomain, extension.injectDebug, dstDir);
                        // 修改后的字节码加入到jar包
                        // 保留clzNewDir到classpath, 这样后面的修改就可以基于这个修改过的字节码
                        List<File> clzList = listClasses(dstDir);
                        String clzDirPath = dstDir.getAbsolutePath() + File.separator;
                        for (File file : clzList) {
                            String entryName = file.getAbsolutePath().replace(clzDirPath, "");
                            JarEntry dstEntry = new JarEntry(entryName);
                            srcJarOutput.putNextEntry(dstEntry);
                            byte[] buffer = IOUtils.toByteArray(new FileInputStream(file));
                            srcJarOutput.write(buffer);
                            srcJarOutput.closeEntry();
                        }
                    }

                    // 2. modify class
                    Enumeration<JarEntry> entries = srcJar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry srcEntry = entries.nextElement();
                        String entryName = srcEntry.getName();

                        JarEntry dstEntry = new JarEntry(entryName);
                        srcJarOutput.putNextEntry(dstEntry);
                        // 修改
                        byte[] buffer = injectClass(group, extension.injectDebug, injectDomain, entryName);

                        // 如果没有修改, 使用原来的
                        if (null == buffer) {
                            buffer = IOUtils.toByteArray(srcJar.getInputStream(srcEntry));
                        }

                        srcJarOutput.write(buffer);
                        srcJarOutput.closeEntry();
                    }

                    // 成功之后更新复制的源jar包
                    srcFile = srcTmpFile;

                    // 移除临时路径, 已经加入到jar包中
                    removeClassPath(tag + "_clzNewDir");
                    removeClassPath(tag);

                    if (extension.injectDebug) {
                        PluginHelper.println(group, "");
                        PluginHelper.println(group, "name = " + name);
                        PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                        PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                        PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                        PluginHelper.println(group, "========> Inject End transformJar [" + tag + "] <========");
                        PluginHelper.println(group, "");
                    }


                } finally {
                    srcJarOutput.close();
                    srcJar.close();
                }
            }
            // 否则没有需要注入的, 就直接复制, 不进行修改
        }
        // 写入到目标文件
        FileUtils.copyFile(srcFile, dstFile);

        // 更换jar包路径为最终修改后的jar包路径
        appendClassPath(tag, dstFile.getAbsolutePath());
    }

    /**
     * 处理自己项目的源码
     *
     * @param group
     * @param input
     * @param extension
     * @param outputProvider
     * @param context
     * @param proDir
     * @throws IOException
     */
    public static void transformCode(String group, DirectoryInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {

        //对源码文件进行处理
        String name = input.getName();
        // build/tmp/xxx(任务名)
        File tmpDir = context.getTemporaryDir();
        File dstFile = outputProvider.getContentLocation(input.getName(), input.getContentTypes(),
                input.getScopes(), Format.DIRECTORY);

        // 源码class目录
        File srcFile = input.getFile();

        if (null != extension) {
            Set<InjectDomain> set = extension.injectDomains;
            InjectDomain injectDomain = null;
            for (InjectDomain domain : set) {
                if (domain.group == null || domain.group.isEmpty()) {
                    continue;
                }
                // 当前库与注入目标库相同
                if (domain.group.equals(group)) {
                    injectDomain = domain;
                    break;
                }
            }

            if (null != injectDomain) {
                // 添加源码路径
//                appendClassPath(group, srcFile.getAbsolutePath());

                if (extension.injectDebug) {
                    PluginHelper.println(group, "========> Inject Begin transformCode [" + group + "] <========");
                    PluginHelper.println(group, "group = " + group);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                }

                // 1. create class
                String clzNewDir = injectDomain.clzNewDir;
                File clzNewDstDir = null;
                if (null != clzNewDir && !clzNewDir.isEmpty()) {
                    File clzDir = new File(proDir, clzNewDir);
                    File dstDir = new File(tmpDir, clzNewDir);
                    // 先复制到临时目录
                    FileUtils.copyDirectory(clzDir, dstDir);
                    // 添加临时路径
                    appendClassPath(group + "_clzNewDir", dstDir.getAbsolutePath());
                    // 移除缓存文件
                    cleanJavacCache(dstDir, srcFile);
                    // 修改字节码
                    writeClassToSource(group, injectDomain, extension.injectDebug, dstDir);

                    clzNewDstDir = dstDir;
                }

                // 2. modify class
                List<File> clzList = listClasses(srcFile);
                String clzDirPath = srcFile.getAbsolutePath() + File.separator;
                for (File file : clzList) {
                    String entryName = file.getAbsolutePath().replace(clzDirPath, "");

                    // 修改
                    byte[] buffer = injectClass(group, extension.injectDebug, injectDomain, entryName);

                    // 覆盖class文件
                    if (null != buffer) {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(buffer);
                        fos.close();
                    }
                }

                // 复制到最终路径
                if (null != clzNewDstDir) {
                    FileUtils.copyDirectory(clzNewDstDir, srcFile);
                }
                // 移除临时路径, 已经拷贝的srcFile路径
                removeClassPath(group + "_clzNewDir");
                // 移除之前的源码路径
                removeClassPath(group);

                if (extension.injectDebug) {
                    PluginHelper.println(group, "");
                    PluginHelper.println(group, "group = " + group);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "========> Inject End transformCode [" + group + "] <========");
                    PluginHelper.println(group, "");
                }
            }
        }

        // 写入到目标文件
        FileUtils.copyDirectory(srcFile, dstFile);

        // 更换最终的源码路径
        appendClassPath(group, dstFile.getAbsolutePath());
    }

    /**
     * 移除javac中的缓存文件
     *
     * @param clzDir  修改的字节码文件夹
     * @param srcFile 缓存的javac文件夹
     */
    private static void cleanJavacCache(File clzDir, File srcFile) {
        if (clzDir == null || null == srcFile) {
            return;
        }

        String clzDirPath = clzDir.getAbsolutePath() + File.separator;
        List<File> clzList = listClasses(clzDir);
        for (File file : clzList) {
            String entryName = file.getAbsolutePath().replace(clzDirPath, "");

            File srcCacheFile = new File(srcFile, entryName);

            // 移除之前缓存的文件
            if (srcCacheFile.exists()) {
                srcCacheFile.delete();
            }
        }

    }

    /**
     * 写入字节码到源码编译结果中
     *
     * @param group
     * @param injectDomain
     * @param injectDebug
     * @param clzDir
     * @throws IOException
     */
    private static void writeClassToSource(String group, InjectDomain injectDomain, boolean injectDebug, File clzDir) throws IOException {
        if (clzDir == null) {
            return;
        }

        String clzDirPath = clzDir.getAbsolutePath() + File.separator;
        List<File> clzList = listClasses(clzDir);
        for (File file : clzList) {
            String entryName = file.getAbsolutePath().replace(clzDirPath, "");

            // 修改
            byte[] buffer = injectClass(group, injectDebug, injectDomain, entryName);

            // 没有修改使用原始文件
            if (null == buffer) {
                buffer = IOUtils.toByteArray(new FileInputStream(file));
            }

            // 写入class文件
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer);
            fos.close();
        }
    }

    /**
     * 注入类
     *
     * @param group
     * @param injectDebug
     * @param injectDomain
     * @param entryName
     * @return
     */
    public static byte[] injectClass(String group, boolean injectDebug, InjectDomain injectDomain, String entryName) throws IOException {
        InjectClzModify clzModify = null;
        if (null != injectDomain.clzModify && !injectDomain.clzModify.isEmpty()) {
            for (InjectClzModify modify : injectDomain.clzModify) {
                // 不注入就跳过
                if (!modify.isInject) {
                    continue;
                }
                String className = modify.className;

                String clzEntryName = modify.getEntryName();

                if (entryName.equals(clzEntryName)) {
                    URL url = pool.find(className);
                    if (null != url) {
                        // 找到修改的类
                        clzModify = modify;
                    }
                    break;
                }
            }
        }

        // 需要修改
        if (null != clzModify) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String className = clzModify.className;
            CtClass ctClass = null;

            try {
                ctClass = pool.get(className);
                // 解冻
                if (ctClass.isFrozen()) {
                    ctClass.defrost();
                }
                // 导入关联类
                List<String> importPackages = clzModify.importPackages;
                if (null != importPackages && !importPackages.isEmpty()) {
                    if (injectDebug)
                        PluginHelper.println(group, "");
                    for (String packageName : importPackages) {
                        importPackageName(packageName);
                        if (injectDebug)
                            PluginHelper.println(group, "importPackage [" + packageName + "]");
                    }
                }

                // 新增属性
                List<String> addFields = clzModify.addFields;
                if (null != addFields && !addFields.isEmpty()) {
                    if (injectDebug)
                        PluginHelper.println(group, "");
                    for (String field : addFields) {
                        try {
                            CtField ctField = CtField.make(field, ctClass);
                            ctClass.addField(ctField);
                            if (injectDebug)
                                PluginHelper.println(group, "addField [" + field + "]");
                        } catch (Exception e) {
                            PluginHelper.printlnErr(group, "[" + entryName + "] addField " + field + "Failure!");
                            PluginHelper.printlnErr(group, "ctClass = " + ctClass);
                            e.printStackTrace();
                            throw new IOException(e);
                        }
                    }
                }

                // 新增方法
                List<String> addMethods = clzModify.addMethods;
                if (null != addMethods && !addMethods.isEmpty()) {
                    if (injectDebug)
                        PluginHelper.println(group, "");
                    for (String method : addMethods) {
                        try {
                            CtMethod ctMethod = CtMethod.make(method, ctClass);
                            ctClass.addMethod(ctMethod);
                            if (injectDebug)
                                PluginHelper.println(group, "addMethod [" + method + "]");
                        } catch (Exception e) {
                            PluginHelper.printlnErr(group, "[" + entryName + "] addMethod " + method + "Failure!");
                            e.printStackTrace();
                            throw new IOException(e);
                        }
                    }
                }

                // 修改方法
                List<InjectModifyMethod> modifyMethods = clzModify.modifyMethods;
                if (null != modifyMethods && !modifyMethods.isEmpty()) {
                    if (injectDebug)
                        PluginHelper.println(group, "");
                    for (InjectModifyMethod method : modifyMethods) {
                        String name = method.name;
                        if (null == name || name.length() == 0) {
                            continue;
                        }
                        CtClass[] params = parseCtClass(method.params);

                        CtMethod ctMethod;
                        if (null != params) {
                            ctMethod = ctClass.getDeclaredMethod(name, params);
                        } else {
                            ctMethod = ctClass.getDeclaredMethod(name);
                        }

                        if (null == ctMethod) {
                            throw new IOException("modifyMethod [" + name + "] name is null !");
                        }

                        String type = method.type;
                        if (null == type || type.length() == 0) {
                            throw new IOException("modifyMethod [" + name + "] type is null !");
                        }
                        String content = method.content;
                        if (null == content || content.length() == 0) {
                            throw new IOException("modifyMethod [" + name + "] content is null !");
                        }

                        if (type.equalsIgnoreCase("insertAt") && method.lineNum < 0) {
                            throw new IOException("modifyMethod [" + name + "] lineNum[for insertAt] can't empty !");
                        }

                        if (type.equalsIgnoreCase("insertBefore")) {
                            ctMethod.insertBefore(content);
                        } else if (type.equalsIgnoreCase("insertAfter")) {
                            ctMethod.insertAfter(content);
                        } else if (type.equalsIgnoreCase("insertAt")) {
                            ctMethod.insertAt(method.lineNum, content);
                        } else if (type.equalsIgnoreCase("setBody")) {
                            ctMethod.setBody(content);
                        }

                        if (null != method.modifiers && method.modifiers.trim().length() > 0) {
                            int modifiers;
                            if (method.modifiers.contains("public")) {
                                modifiers = AccessFlag.PUBLIC;
                            } else if (method.modifiers.contains("private")) {
                                modifiers = AccessFlag.PRIVATE;
                            } else {
                                modifiers = AccessFlag.PROTECTED;
                            }
                            if (method.modifiers.contains("final")) {
                                modifiers |= AccessFlag.FINAL;
                            }
                            if (method.modifiers.contains("static")) {
                                modifiers |= AccessFlag.STATIC;
                            }
                            if (method.modifiers.contains("synchronized")) {
                                modifiers |= AccessFlag.SYNCHRONIZED;
                            }
                            ctMethod.setModifiers(modifiers);
                        }

                        if (injectDebug) {
                            PluginHelper.println(group, "modifyMethod [" + name + "] " + type);
                            PluginHelper.println(group, content);
                        }
                    }
                }
                bos.write(ctClass.toBytecode());
                if (injectDebug)
                    PluginHelper.println(group, "modify [" + className + "] Done!");

                return bos.toByteArray();
            } catch (Exception e) {
                PluginHelper.printlnErr(group, "modify [" + className + "] Failure!");
                e.printStackTrace();
                throw new IOException(e);
            } finally {
                if (null != ctClass) {
                    ctClass.detach();
                }

                // 移除导入的包
                List<String> importPackages = clzModify.importPackages;
                if (null != importPackages && !importPackages.isEmpty()) {
                    if (injectDebug)
                        PluginHelper.println(group, "");
                    Set<String> pkgSet = new HashSet<>(importPackages);
                    Iterator<String> iterator = pool.getImportedPackages();
                    while (iterator.hasNext()) {
                        String pkg = iterator.next();
                        if (pkgSet.contains(pkg)) {
                            iterator.remove();
                            if (injectDebug)
                                PluginHelper.println(group, "removePackage [" + pkg + "]");
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 解析方法签名到CtClass参数
     *
     * @param params
     * @return
     */
    private static CtClass[] parseCtClass(String params) {
        CtClass[] ret = null;
        if (null != params && params.length() > 0) {
            try {
                ret = Descriptor.getParameterTypes(params, pool);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * 获取所有class文件
     *
     * @param dir
     * @return
     */
    private static List<File> listClasses(File dir) {
        File[] files = dir.listFiles();
        List<File> clzList = new ArrayList<>();
        if (null == files || files.length == 0) {
            return clzList;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                clzList.addAll(listClasses(file));
            } else {
                clzList.add(file);
            }
        }
        return clzList;
    }

    /**
     * 清理缓存, 否则容易出异常
     * Caused by: javassist.NotFoundException: broken jar file?
     */
    private static void clearJarFactoryCache() {
        try {
            Class clazz = Class.forName("sun.net.www.protocol.jar.JarFileFactory");
            Field fileCacheField = clazz.getDeclaredField("fileCache");
            Field urlCacheField = clazz.getDeclaredField("urlCache");
            fileCacheField.setAccessible(true);
            urlCacheField.setAccessible(true);
            Map fileCache = (Map) fileCacheField.get(null);
            Map urlCache = (Map) urlCacheField.get(null);
            fileCache.clear();
            urlCache.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
