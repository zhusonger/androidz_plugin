package cn.com.lasong.inject;

import java.util.List;

public class InjectModifyMethod {

    // 修饰符
    public String modifiers;

    // 方法名
    public String name;

    // 方法参数
    public String params;

    // 插入的代码
    public String content;

    // 插入类型
//    insertBefore : 在方法的起始位置插入代码；
//    insertAfter : 在方法的所有 return 语句前插入代码以确保语句能够被执行，除非遇到exception；
//    insertAt : 在指定的位置插入代码；
//    setBody : 将方法的内容设置为要写入的代码，当方法被 abstract修饰时，该修饰符被移除；
    public String type;

    // 行号, insertAt用到
    public int lineNum = -1;

    public String getModifiers() {
        return modifiers;
    }

    public void setModifiers(String modifiers) {
        this.modifiers = modifiers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"")
                .append(name).append('\"');
        sb.append(",\"params\":\"")
                .append(params).append('\"');
        sb.append(",\"content\":\"")
                .append(content).append('\"');
        sb.append(",\"type\":\"")
                .append(type).append('\"');
        sb.append(",\"lineNum\":")
                .append(lineNum);
        sb.append('}');
        return sb.toString();
    }
}
