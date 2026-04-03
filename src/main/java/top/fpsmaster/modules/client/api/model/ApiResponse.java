package top.fpsmaster.modules.client.api.model;

import com.google.gson.JsonObject;

public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message, String error) {
        return new ApiResponse<>(false, message, null);
    }

    public static <T> ApiResponse<T> fromJson(JsonObject json, Class<T> dataClass) {
        if (json == null) {
            return error(-1, "Invalid response", "Response is null");
        }

        boolean success = json.has("success") && json.get("success").getAsBoolean();
        String message = json.has("message") ? json.get("message").getAsString() : "";

        T data = null;
        if (json.has("data") && !json.get("data").isJsonNull()) {
            try {
                if (dataClass != null && dataClass != Void.class) {
                    data = FPSMasterGson.getInstance().fromJson(json.get("data"), dataClass);
                }
            } catch (Exception e) {
                return error(-1, message, "Failed to parse data: " + e.getMessage());
            }
        }

        return new ApiResponse<>(success, message, data);
    }

    public static ApiResponse<Void> fromJson(JsonObject json) {
        return fromJson(json, Void.class);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
