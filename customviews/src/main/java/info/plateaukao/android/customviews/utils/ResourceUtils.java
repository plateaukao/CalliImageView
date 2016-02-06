package info.plateaukao.android.customviews.utils;

import android.graphics.Typeface;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ResourceUtils {
    private static Map<String, Typeface> typefaceList = new HashMap<String, Typeface>();

    public static Typeface getTypeface(String fontPath){
        if(null == fontPath)
            return null;
        
        if(null != typefaceList.get(fontPath)){
            return typefaceList.get(fontPath);
        } else {
            Typeface typeface = Typeface.createFromFile(new File(fontPath));
            typefaceList.put(fontPath, typeface);
            return typeface;
        }
    }
}
