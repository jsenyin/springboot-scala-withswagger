package com.bob.java.webapi.utils;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 *
 * @Description: TODO
 * @ClassName: BeanUtilEx
 * @author: Mr.yinji
 */
public final class BeanUtilEx extends BeanUtils {

    private static Log logger = LogFactory.getFactory().getInstance(BeanUtilEx.class);

    private BeanUtilEx() {
    }

    static {
//        ConvertUtils.register(new DateConvert(), java.util.Date.class);
//        ConvertUtils.register(new DateConvert(), String.class);
    }

    public static void copyProperties(Object target, Object source) {
        // 支持对日期copy
        try {
            BeanUtils.copyProperties(target, source);
        } catch (Exception e) {
            logger.error("扩展BeanUtils.copyProperties支持data类型:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class DateConvert implements Converter {

        @Override
        public Object convert(Class class1, Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Date) {
                return value;
            }
            if (value instanceof Long) {
                Long longValue = (Long) value;
                return new Date(longValue.longValue());
            }
            if (value instanceof String) {
                String dateStr = value.toString();
                Date endTime = null;
                try {
                    String regexp1 = "^\\d{4}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D*$";
                    String regexp2 = "([0-9]{4})-([0-1][0-9])-([0-3][0-9])";
                    if (dateStr.matches(regexp1)) {
                        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = sdf.parse(dateStr);
                        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
                        System.out.println(sqlDate);
                        return sqlDate;
                    } else if (dateStr.matches(regexp2)) {
                        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        endTime = (Date) sdf.parse(dateStr);
                        return endTime;
                    } else {
                        return value;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return value;
        }

    }



}
