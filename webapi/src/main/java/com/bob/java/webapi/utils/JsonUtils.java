package com.bob.java.webapi.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JsonUtils {
    private static SerializeConfig mapping = new SerializeConfig();
    private static String dateFormat = "yyyy-MM-dd HH:mm:ss";

    public static String toJSON(Object jsonText) {
        return JSON.toJSONString(jsonText,
                SerializerFeature.WriteDateUseDateFormat);
    }

    public static String toJSON(String dateFormat, Object jsonText) {
        mapping.put(Date.class, new SimpleDateFormatSerializer(dateFormat));
        return JSON.toJSONString(jsonText, mapping);
    }

    public static String toRealJSON(Object jsonText){
        return JSON.toJSONString(jsonText,SerializerFeature.DisableCircularReferenceDetect);
    }

    public static Map str2Map(String value){
        HashMap map = new HashMap();
        String[] k_vs = value.split("\\#");
        for(int i=0;i<k_vs.length;i++){
            String[] k_v = k_vs[i].split("\\=");
            if(k_v.length==1){
                map.put(k_v[0], "");
            }else{
                map.put(k_v[0], k_v[1]);
            }
        }
        return map;
    }

}