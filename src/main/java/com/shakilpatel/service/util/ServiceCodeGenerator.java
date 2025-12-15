package com.shakilpatel.service.util;

import java.security.SecureRandom;

public class ServiceCodeGenerator {

    private static final String PREFIX = "SRV_";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 12;
    private static final SecureRandom random = new SecureRandom();

    public static String generate() {
        StringBuilder code = new StringBuilder(PREFIX);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}

//package com.shakilpatel.service.util;
//
//import java.security.SecureRandom;
//
//public class ServiceCodeGenerator {
//
//    private static final String PREFIX = "SRV_";
//    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//    private static final int CODE_LENGTH = 12;
//    private static final SecureRandom random = new SecureRandom();
//
//    public static String generate() {
//        return generate(PREFIX, CODE_LENGTH);
//    }
//
//    public static String generate(String prefix, int length) {
//        StringBuilder code = new StringBuilder(prefix);
//        for (int i = 0; i < length; i++) {
//            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
//        }
//        return code.toString();
//    }
//}
