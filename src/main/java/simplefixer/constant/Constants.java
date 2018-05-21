package simplefixer.constant;

import okhttp3.MediaType;

public final class Constants {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String CODEGEN_POST_URL = "http://codegen-cnu.ey.devfactory.com/api/codegen/";
    public static final String CODEGEN_GET_URL = "http://codegen-cnu.ey.devfactory.com/api/codegen/status/";
    public static final String USERNAME = "neo4j";
    public static final String PASSWORD = "password";
}
