package com.itheima.pinda;

import com.itheima.pinda.utils.DateUtils;
import org.junit.Test;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @Author：freeLee
 * @Date：2022/7/4 23:09
 * @Description：
 */
public class TestDateDirectory {

    /**
     * 当前日期格式化为目录
     */
    @Test
    public void dataDirectory(){
        String s = Paths.get(LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_DAY_FORMAT_SLASH))).toString();
        System.out.println(s);
    }
}
