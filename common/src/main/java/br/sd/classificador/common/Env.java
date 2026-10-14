package br.sd.classificador.common;

/**
 * Leitura de configuração a partir de variáveis de ambiente, com valores padrão.
 * Nenhuma credencial fica fixa no código: tudo vem do ambiente (docker-compose / .env).
 */
public final class Env {

    private Env() {
    }

    public static String get(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    public static int getInt(String name, int defaultValue) {
        String value = get(name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Variável " + name + " deve ser um inteiro, recebido: " + value, e);
        }
    }

    public static long getLong(String name, long defaultValue) {
        String value = get(name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Variável " + name + " deve ser um inteiro, recebido: " + value, e);
        }
    }

    public static double getDouble(String name, double defaultValue) {
        String value = get(name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Variável " + name + " deve ser um número, recebido: " + value, e);
        }
    }

    public static boolean getBoolean(String name, boolean defaultValue) {
        String value = get(name, null);
        if (value == null) {
            return defaultValue;
        }
        return value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("sim");
    }
}
