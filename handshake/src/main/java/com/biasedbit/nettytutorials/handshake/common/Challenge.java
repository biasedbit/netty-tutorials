package com.biasedbit.nettytutorials.handshake.common;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class Challenge {

    // public static methods --------------------------------------------------

    public static String generateChallenge() {
        return "challenge?";
    }

    public static boolean isValidChallenge(String challenge) {
        return "challenge?".equals(challenge);
    }

    public static String generateResponse(String challenge) {
        if ("challenge?".equals(challenge)) {
            return "response!";
        } else {
            return "invalidResponse!";
        }
    }

    public static boolean isValidResponse(String response, String challenge) {
        return "response!".equals(response) && "challenge?".equals(challenge);
    }
}
