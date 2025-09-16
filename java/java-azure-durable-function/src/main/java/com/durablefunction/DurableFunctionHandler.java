package com.durablefunction;

import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;

public class DurableFunctionHandler {

    // Trigger HTTP para iniciar la orquestación
    @FunctionName("HttpStart")
    public HttpResponseMessage httpStart(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<String> request,
            final ExecutionContext context,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext) {

        context.getLogger().info("HTTP starter recibido");
        String input = request.getBody();
         DurableTaskClient client = durableContext.getClient();
        String instanceId = client.scheduleNewOrchestrationInstance("MainOrchestrator", input);
        context.getLogger().info("Orquestación iniciada con ID: " + instanceId);

        return request.createResponseBuilder(HttpStatus.OK)
                .body("Orquestación iniciada con ID: " + instanceId)
                .build();
    }

    // Orquestador principal: Normaliza, invoca Python y almacena
    @FunctionName("MainOrchestrator")
    public String mainOrchestrator(
            @DurableOrchestrationTrigger(name = "orchestrationContext") TaskOrchestrationContext ctx) {

        String input = ctx.getInput(String.class);

        // 1. Normalizar datos
        String normalized = ctx.callActivity("NormalizeDataActivity", input, String.class).await();

        // 2. Invocar API Python
        String pythonResult = ctx.callActivity("InvokePythonActivity", normalized, String.class).await();

        // 3. Guardar datos
        String storeResult = ctx.callActivity("StoreDataActivity", pythonResult, String.class).await();

        return storeResult;
    }

    @FunctionName("NormalizeDataActivity")
    public String normalizeDataActivity(
            @DurableActivityTrigger(name = "activityContext") String input,
            final ExecutionContext executionContext) {
        executionContext.getLogger().info("Normalizando datos: " + input);
        return input == null ? "" : input.trim().toUpperCase();
    }

    @FunctionName("InvokePythonActivity")
    public String invokePythonActivity(
            @DurableActivityTrigger(name = "activityContext") String input,
            final ExecutionContext executionContext) {
        executionContext.getLogger().info("Invocando API Python con: " + input);

        String pythonApiUrl = "https://modelo-analitico-fn.azurewebsites.net/api/process";
        String responseBody = "";

        try {
            java.net.URI uri = java.net.URI.create(pythonApiUrl);
            java.net.URL url = uri.toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"input\": \"" + input + "\"}";
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] inputBytes = jsonInputString.getBytes("utf-8");
                os.write(inputBytes, 0, inputBytes.length);
            }

            int code = conn.getResponseCode();
            executionContext.getLogger().info("Código de respuesta: " + code);

            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseBody = response.toString();
            }
        } catch (Exception e) {
            executionContext.getLogger().severe("Error al invocar el API Python: " + e.getMessage());
            responseBody = "{\"error\": \"" + e.getMessage() + "\"}";
        }

        return responseBody;
    }

    @FunctionName("StoreDataActivity")
    public String storeDataActivity(
            @DurableActivityTrigger(name = "activityContext") String input,
            final ExecutionContext executionContext) {
        executionContext.getLogger().info("Guardando datos: " + input);
        // Aquí podrías almacenar el resultado en una base de datos o similar
        return "Datos almacenados: " + input;
    }
}