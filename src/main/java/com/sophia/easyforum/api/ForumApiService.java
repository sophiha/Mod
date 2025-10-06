package com.sophia.easyforum.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ForumApiService {
    private static final Logger LOGGER = LogManager.getLogger("EasyForum");

    private ForumApiService() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void sendReport(ICommandSender sender, String n, String m, String p) {
        new Thread(() -> {
            try {
                processRequest(sender, n, m, p);
            } catch (Exception e) {
                LOGGER.error("[EasyForum] Erro crítico na thread de requisição: {}", e.getMessage());

                try {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Erro crítico ao processar requisição. Verifique os logs."));
                } catch (Exception ex) {
                    LOGGER.error("[EasyForum] Não foi possível enviar mensagem de erro ao chat.");
                }
            }
        }, "EasyForum-API-Thread").start();
    }

    private static void processRequest(ICommandSender sender, String n, String m, String p) {
        HttpURLConnection connection = null;

        try {
            LOGGER.info("[EasyForum] Iniciando requisição a API...");
            System.setProperty("https.protocols", "TLSv1.2");

            String u = Minecraft.getMinecraft().thePlayer.getUniqueID().toString();
            String jsonPayload = buildJsonPayload(n, m, p, u);

            LOGGER.info("[EasyForum] Criando conexão HTTP...");
            connection = createConnection();

            LOGGER.info("[EasyForum] Enviando dados...");
            sendRequest(connection, jsonPayload);

            LOGGER.info("[EasyForum] Aguardando resposta...");
            int statusCode = connection.getResponseCode();
            LOGGER.info("[EasyForum] Status HTTP: {}", statusCode);

            String responseBody = readResponse(connection, statusCode);
            LOGGER.info("[EasyForum] Resposta recebida: {}", responseBody);

            String message = parseResponseMessage(responseBody);
            LOGGER.info("[EasyForum] Mensagem parseada: {}", message);

            LOGGER.info("[EasyForum] Enviando resposta ao jogador...");
            handleApiResponse(sender, statusCode, message, responseBody);
            LOGGER.info("[EasyForum] Resposta enviada com sucesso!");

        } catch (SocketTimeoutException e) {
            LOGGER.error("[EasyForum] Timeout na conexão: {}", e.getMessage());

            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "A API do EasyForum demorou muito para responder. Tente novamente mais tarde."));
        } catch (UnknownHostException e) {
            LOGGER.error("[EasyForum] Host não encontrado: {}", e.getMessage());

            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Não foi possível conectar à API do EasyForum. Verifique sua conexão com a internet."));
        } catch (IOException e) {
            LOGGER.error("[EasyForum] Erro de I/O: {}", e.getMessage());

            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Erro de comunicação com a API do EasyForum: " + e.getMessage() + ". Tente novamente mais tarde."));
        } catch (Exception e) {
            LOGGER.error("[EasyForum] Erro inesperado: {}", e.getMessage());

            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Ocorreu um erro inesperado: " + e.getClass().getSimpleName() + ". Por favor, contate o suporte."));
        } finally {
            if (connection != null) {
                connection.disconnect();

                LOGGER.info("[EasyForum] Conexão encerrada.");
            }
        }
    }

    private static HttpURLConnection createConnection() throws Exception {
        URL url = new URL("https://easyforum.fun/create-topic");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("User-Agent", "EasyForumMod/3.0.0");
        connection.setConnectTimeout(300000);
        connection.setReadTimeout(600000);
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
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Resposta inesperada do servidor (Status: " + statusCode + ")"));

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
        } catch (Exception e) {
            LOGGER.error("[EasyForum] Erro ao parsear resposta JSON: {}", e.getMessage());
        }

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
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Sua versão do EasyForum está desatualizada. Para continuar, baixe a versão mais recente no site."));
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
