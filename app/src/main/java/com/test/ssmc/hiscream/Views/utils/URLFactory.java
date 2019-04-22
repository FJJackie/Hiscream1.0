package com.test.ssmc.hiscream.Views.utils;


public class URLFactory {
    public static String handleURL(String url){
        if(url.matches("^(?=^.{3,255}$)(http(s)?:\\/\\/)?(www\\.)?[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+(:\\d+)*(\\/\\w+\\.\\w+)*$")){
            if(url.contains("http://")||url.contains("https://"))
                return url;
            else
                return "http://"+url;
        }else{
            return "https://cn.bing.com/search?q="+url;
        }

    }
}
