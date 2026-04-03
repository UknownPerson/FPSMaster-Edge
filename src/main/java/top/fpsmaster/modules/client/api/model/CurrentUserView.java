package top.fpsmaster.modules.client.api.model;

public class CurrentUserView {
    private String id;
    private String username;
    private String email;
    private String role;
    private boolean emailVerified;
    private boolean banned;
    private String walletBalance;
    private int level;
    private int experience;
    private int nextLevelNeed;
    private boolean checkedInToday;
    private String lastCheckInDate;
    private String customTitle;
    private boolean sponsorTitleClaimed;
    private boolean novaBetaEligible;
    private String avatarUrl;
    private String membershipExpiresAt;

    public CurrentUserView() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public String getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(String walletBalance) {
        this.walletBalance = walletBalance;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getNextLevelNeed() {
        return nextLevelNeed;
    }

    public void setNextLevelNeed(int nextLevelNeed) {
        this.nextLevelNeed = nextLevelNeed;
    }

    public boolean isCheckedInToday() {
        return checkedInToday;
    }

    public void setCheckedInToday(boolean checkedInToday) {
        this.checkedInToday = checkedInToday;
    }

    public String getLastCheckInDate() {
        return lastCheckInDate;
    }

    public void setLastCheckInDate(String lastCheckInDate) {
        this.lastCheckInDate = lastCheckInDate;
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }

    public boolean isSponsorTitleClaimed() {
        return sponsorTitleClaimed;
    }

    public void setSponsorTitleClaimed(boolean sponsorTitleClaimed) {
        this.sponsorTitleClaimed = sponsorTitleClaimed;
    }

    public boolean isNovaBetaEligible() {
        return novaBetaEligible;
    }

    public void setNovaBetaEligible(boolean novaBetaEligible) {
        this.novaBetaEligible = novaBetaEligible;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(String membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    @Override
    public String toString() {
        return "CurrentUserView{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", emailVerified=" + emailVerified +
                ", banned=" + banned +
                ", level=" + level +
                ", experience=" + experience +
                ", avatarUrl='" + avatarUrl + '\'' +
                '}';
    }
}
