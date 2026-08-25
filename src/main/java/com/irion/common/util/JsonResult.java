package com.irion.common.util;

public class JsonResult {

    private boolean success;
    private String message;
    private Object data;

    public JsonResult() {
    }

    public JsonResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public JsonResult(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static JsonResult success() {
        return new JsonResult(true, "성공");
    }

    public static JsonResult success(String message) {
        return new JsonResult(true, message);
    }

    public static JsonResult success(String message, Object data) {
        return new JsonResult(true, message, data);
    }

    public static JsonResult fail(String message) {
        return new JsonResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}