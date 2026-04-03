package top.fpsmaster.modules.client.api.model;

public class LoginResponse {
    private String token;
    private CurrentUserView user;

    public LoginResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CurrentUserView getCurrentUserView() {
        return user;
    }

    public void setUser(CurrentUserView user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "token='" + token + '\'' +
                ", user=" + user +
                '}';
    }
}
