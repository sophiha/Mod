package com.sophia.easyforum.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ForumApiService {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private ForumApiService() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void sendReport(ICommandSender sender, String n, String m, String p) {
        CompletableFuture.runAsync(() -> processRequest(sender, n, m, p), EXECUTOR);
    }

    private static void processRequest(ICommandSender sender, String n, String m, String p) {
        HttpURLConnection connection = null;
        try {
            System.setProperty("https.protocols", "TLSv1.2");

            String u = Minecraft.getMinecraft().thePlayer.getUniqueID().toString();
            String jsonPayload = buildJsonPayload(n, m, p, u);

            connection = createConnection();
            sendRequest(connection, jsonPayload);

            int statusCode = connection.getResponseCode();

            String responseBody = readResponse(connection, statusCode);
            String message = parseResponseMessage(responseBody);

            Minecraft.getMinecraft().addScheduledTask(() -> handleApiResponse(sender, statusCode, message, responseBody));
        } catch (Exception e) {
            Minecraft.getMinecraft().addScheduledTask(() -> sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "A API do EasyForum parece estar offline. Tente novamente mais tarde.")));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection createConnection() throws Exception {
        URL url = new URL("http://localhost/create-topic");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("User-Agent", "EasyForumMod/1.0");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);

        return connection;
    }

    private static void sendRequest(HttpURLConnection connection, String jsonPayload) throws Exception {
        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] body = jsonPayload.getBytes(StandardCharsets.UTF_8);
            outputStream.write(body, 0, body.length);
        }
    }

    private static String readResponse(HttpURLConnection connection, int statusCode) throws Exception {
        InputStream inputStream = (statusCode >= 200 && statusCode < 300) ? connection.getInputStream() : connection.getErrorStream();

        return readInputStream(inputStream);
    }

    private static void handleApiResponse(ICommandSender sender, int statusCode, String message, String rawBody) {
        if (message == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Resposta inesperada do servidor (Status: " + statusCode + ")"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Corpo da Resposta: " + rawBody));
            return;
        }

        ResponseType responseType = ResponseType.fromMessage(message);
        responseType.handleResponse(sender, statusCode);
    }

    private static String buildJsonPayload(String n, String m, String p, String u) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("n", n);
        jsonObject.addProperty("m", m);
        jsonObject.addProperty("p", p);
        jsonObject.addProperty("c", "CHAVE_AQUI");
        jsonObject.addProperty("u", u);

        return jsonObject.toString();
    }

    private static String readInputStream(InputStream inputStream) throws Exception {
        if (inputStream == null) return "";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];

        int length;

        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }

        return outputStream.toString(StandardCharsets.UTF_8.name());
    }

    private static String parseResponseMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return null;

        try {
            JsonObject jsonObject = new JsonParser().parse(responseBody).getAsJsonObject();
            JsonElement messageElement = jsonObject.get("message");

            if (messageElement != null && !messageElement.isJsonNull()) {
                return messageElement.getAsString();
            }

            JsonElement errorElement = jsonObject.get("error");

            if (errorElement != null && !errorElement.isJsonNull()) {
                return errorElement.getAsString();
            }
        } catch (Exception ignored) {}

        return null;
    }

    private enum ResponseType {
        SUCCESS {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Sua denúncia foi criada no fórum com sucesso!"));
            }
        },
        UNAUTHORISED {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Sessão do fórum inativa. Por favor, autentique-se novamente no site do EasyForum."));
            }
        },
        INCOMPLETE {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Dados incompletos foram enviados. Tente novamente."));
            }
        },
        MISMATCH {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "A conta de Minecraft que você está usando não está vinculada à sua chave no site."));
            }
        },
        EXPIRED {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Sua sessão no fórum expirou. Autentique-se novamente no site do EasyForum."));
            }
        },
        NOT_FOUND {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Chave de licença inválida ou não encontrada."));
            }
        },
        OUTDATED {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Sua versão do EasyForum está desatualizada. Atualize para a versão mais recente."));
            }
        },
        PENDING {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Você precisa vincular uma conta do Minecraft no site antes de denunciar."));
            }
        },
        REJECTED {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Falha ao enviar a imagem da prova. Tente novamente."));
            }
        },
        FAILED {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Falha ao criar o tópico. O fórum pode estar offline ou sobrecarregado."));
            }
        },
        SERVER {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Ocorreu um erro interno no servidor do EasyForum."));
            }
        },
        UNKNOWN {
            @Override
            void handleResponse(ICommandSender sender, int statusCode) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Recebida uma resposta desconhecida do servidor."));
            }
        };

        abstract void handleResponse(ICommandSender sender, int statusCode);

        static ResponseType fromMessage(String message) {
            if (message == null) return UNKNOWN;

            switch (message.toLowerCase()) {
                case "success":
                    return SUCCESS;
                case "unauthorised":
                    return UNAUTHORISED;
                case "incomplete":
                    return INCOMPLETE;
                case "mismatch":
                    return MISMATCH;
                case "expired":
                    return EXPIRED;
                case "notfound":
                    return NOT_FOUND;
                case "outdated":
                    return OUTDATED;
                case "pending":
                    return PENDING;
                case "rejected":
                    return REJECTED;
                case "failed":
                    return FAILED;
                case "server":
                    return SERVER;
                default:
                    return UNKNOWN;
            }
        }
    }
}