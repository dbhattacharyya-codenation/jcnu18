package simplefixer.constant;

import okhttp3.MediaType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String CODEGEN_POST_URL = "http://codegen-cnu.ey.devfactory.com/api/codegen/";
    public static final String CODEGEN_GET_URL = "http://codegen-cnu.ey.devfactory.com/api/codegen/status/";
    public static final String USERNAME = "neo4j";
    public static final String PASSWORD = "password";
    public static List<String> MODIFIERS_LIST;

    static {
        String[] tmp = {
                "public",
                "private",
                "protected",
                "abstract",
                "static",
                "final",
                "transient",
                "volatile",
                "synchronized",
                "native",
                "strictfp"
        };
        MODIFIERS_LIST = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(tmp)));
    }
}
