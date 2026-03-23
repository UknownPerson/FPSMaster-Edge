package top.fpsmaster.ui.screens.oobe;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.interfaces.CPSDisplay;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.interfaces.ComboDisplay;
import top.fpsmaster.features.impl.interfaces.CoordsDisplay;
import top.fpsmaster.features.impl.interfaces.DirectionDisplay;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.features.impl.interfaces.InventoryDisplay;
import top.fpsmaster.features.impl.interfaces.Keystrokes;
import top.fpsmaster.features.impl.interfaces.PingDisplay;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.features.impl.render.ItemPhysics;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.system.OSUtil;

import java.awt.Desktop;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;

public class OobeScreen extends ScaledGuiScreen {
    private static final int PAGE_COUNT = 8;
    private static final String[] SCALE_LABELS = new String[]{"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x", "2.5x", "3.0x"};
    private static final String[] GREETINGS = new String[]{"Hello, welcome.", "你好，欢迎使用。", "こんにちは、ようこそ。"};
    private static final ResourceLocation PREVIEW_IMAGE = new ResourceLocation("client/background/panorama_1/panorama_0.png");
    private static final ResourceLocation PANORAMA_THREE = new ResourceLocation("client/background/panorama_3/panorama_0.png");
    private static final long TUTORIAL_SLIDE_DURATION_MS = 3000L;
    private static final long OOBE_INTRO_DURATION_MS = 3000L;
    private static final long GREETING_ROTATE_DURATION_MS = 2200L;
    private static boolean sessionStateInitialized;
    private static int savedPage;
    private static int savedLanguageValue;
    private static int savedTutorialIndex;
    private static long savedTutorialStartedAt = System.currentTimeMillis();
    private static boolean savedAntiCheatEnabled;
    private static boolean savedAnonymousDataEnabled;
    private static boolean savedEnterGuide = true;
    private static int savedQaStep;
    private static final int[] savedQaAnswers = new int[]{-1, -1, -1};
    private static String savedBackgroundChoice;
    private static boolean savedLoginSkipped = true;
    private static int savedFeatureCount = 5;
    private static String savedAccountText = "";
    private static String savedPasswordText = "";

    private final AnimClock animClock = new AnimClock();
    private final float[] featureExpand = new float[]{0f, 0f, 0f, 0f};
    private final OobeDropdown scaleDropdown = new OobeDropdown();
    private final float[] qaOptionHover = new float[]{0f, 0f, 0f};
    private final float[] qaOptionPress = new float[]{0f, 0f, 0f};

    private int page;
    private int languageValue;
    private boolean fixedScaleEnabled;
    private int fixedScaleIndex;
    private int tutorialIndex;
    private int hoveredFeature = -1;
    private boolean antiCheatEnabled;
    private boolean anonymousDataEnabled;
    private boolean enterGuide = true;
    private int qaStep;
    private final int[] qaAnswers = new int[]{-1, -1, -1};
    private String backgroundChoice;
    private boolean loginSkipped = true;
    private float pageMotion;
    private int pageMotionDirection = 1;
    private int featureCount = 5;
    private String hoveredBackgroundPreview;
    private String pendingBackgroundChoice;
    private boolean shaderWarningDialogVisible;
    private boolean shaderUnsupportedDialogVisible;
    private double shaderBenchmarkScore;
    private float forgotHoverAnim;
    private float registerHoverAnim;
    private boolean tutorialPlaybackComplete;
    private float tutorialSlideTransition;
    private int tutorialPrevSlide;
    private long introStartedAt;
    private float introProgress = 1f;
    private float greetingTransition = 1f;
    private String greetingCurrentText = "";
    private String greetingPreviousText = "";
    private int greetingIndex;

    private TextField accountField;
    private TextField passwordField;
    private OobeButton backButton;
    private OobeButton nextButton;
    private OobeButton tutorialPrevButton;
    private OobeButton tutorialNextButton;
    private OobeButton loginButton;
    private OobeButton skipLoginButton;
    private OobeButton shaderContinueButton;
    private OobeButton shaderCancelButton;
    private OobeButton shaderUnsupportedOkButton;

    @Override
    public void initGui() {
        super.initGui();
        animClock.reset();
        introStartedAt = System.currentTimeMillis();
        introProgress = 0f;
        greetingIndex = (int) ((System.currentTimeMillis() / GREETING_ROTATE_DURATION_MS) % GREETINGS.length);

        backButton = new OobeButton("Back", false, () -> {
            if (page > 0) {
                page--;
                pageMotion = 1f;
                pageMotionDirection = -1;
            }
        });
        nextButton = new OobeButton("Next", true, this::onNext);
        tutorialPrevButton = new OobeButton("Prev", false, () -> tutorialIndex = (tutorialIndex + 2) % 3);
        tutorialNextButton = new OobeButton("Next", true, () -> tutorialIndex = (tutorialIndex + 1) % 3);
        loginButton = new OobeButton("Sign in", false, () -> loginSkipped = false);
        skipLoginButton = new OobeButton("Skip", true, () -> loginSkipped = true);
        shaderContinueButton = new OobeButton("Continue", true, this::confirmShaderBackgroundSelection);
        shaderCancelButton = new OobeButton("Cancel", false, this::cancelShaderBackgroundSelection);
        shaderUnsupportedOkButton = new OobeButton("OK", true, () -> shaderUnsupportedDialogVisible = false);

        initSessionStateIfNeeded();
        restoreSessionState();

        setPreviewLanguage(languageValue);
        greetingCurrentText = animatedGreeting();
        greetingPreviousText = greetingCurrentText;
        greetingTransition = 1f;
        scaleDropdown.setItems(SCALE_LABELS).setSelectedIndex(fixedScaleIndex).setEnabled(fixedScaleEnabled);

        accountField = new TextField(FPSMaster.fontManager.s18, key("oobe.login.account.placeholder"),
                new Color(255, 255, 255, 0).getRGB(), new Color(36, 44, 60).getRGB(), 32);
        passwordField = new TextField(FPSMaster.fontManager.s18, true, key("oobe.login.password.placeholder"),
                new Color(255, 255, 255, 0).getRGB(), new Color(36, 44, 60).getRGB(), 32);
        accountField.setText(savedAccountText);
        passwordField.setText(savedPasswordText);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        float dt = (float) animClock.tick();
        updateAnimations(dt);
        updateTutorialAutoplay();

        renderBackground();
        float introEased = easeOutCubic(introProgress);
        float introOffsetY = (1f - introEased) * 22f;

        GL11.glPushMatrix();
        GL11.glTranslatef(0f, introOffsetY, 0f);
        renderTopbar();

        GL11.glPushMatrix();
        GL11.glTranslatef(pageMotionDirection * pageMotion * 34f, 0f, 0f);
        switch (page) {
            case 0:
                renderLanguagePage();
                break;
            case 1:
                renderScalePage(mouseX, mouseY);
                break;
            case 2:
                renderTutorialPage(mouseX, mouseY);
                break;
            case 3:
                renderFeaturesPage();
                break;
            case 4:
                renderLoginPage(mouseX, mouseY);
                break;
            case 5:
                renderOptionsPage();
                break;
            case 6:
                renderGuideEntryPage();
                break;
            case 7:
                renderQaPage();
                break;
            default:
                break;
        }
        GL11.glPopMatrix();

        renderFooter(mouseX, mouseY);
        GL11.glPopMatrix();
        renderShaderDialogs(mouseX, mouseY);
        renderIntroOverlay();
        syncSessionState();
    }

    private void updateAnimations(float dt) {
        float speed = Math.min(1f, dt * 6f);
        for (int i = 0; i < featureExpand.length; i++) {
            float target = hoveredFeature == i ? 1f : 0f;
            featureExpand[i] += (target - featureExpand[i]) * speed;
        }
        for (int i = 0; i < qaOptionHover.length; i++) {
            qaOptionPress[i] = (float) AnimMath.base(qaOptionPress[i], 0.0, 0.25);
        }
        forgotHoverAnim = (float) AnimMath.base(forgotHoverAnim, 0.0, 0.25);
        registerHoverAnim = (float) AnimMath.base(registerHoverAnim, 0.0, 0.25);
        pageMotion += (0f - pageMotion) * Math.min(1f, dt * 8.5f);
        tutorialSlideTransition += (1f - tutorialSlideTransition) * Math.min(1f, dt * 8f);
        float introTarget = Math.min(1f, Math.max(0f, (System.currentTimeMillis() - introStartedAt) / (float) OOBE_INTRO_DURATION_MS));
        introProgress += (introTarget - introProgress) * Math.min(1f, dt * 4f);
        int nextGreetingIndex = (int) ((System.currentTimeMillis() / GREETING_ROTATE_DURATION_MS) % GREETINGS.length);
        if (nextGreetingIndex != greetingIndex) {
            greetingPreviousText = greetingCurrentText;
            greetingIndex = nextGreetingIndex;
            greetingCurrentText = animatedGreeting();
            greetingTransition = 0f;
        }
        if (greetingTransition < 1f) {
            greetingTransition += (1f - greetingTransition) * Math.min(1f, dt * 7f);
            if (greetingTransition > 0.995f) {
                greetingTransition = 1f;
                greetingPreviousText = greetingCurrentText;
            }
        }
        accountField.updateCursorCounter();
        passwordField.updateCursorCounter();
    }

    private void renderBackground() {
        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(244, 247, 255, 255));
    }

    private void renderTopbar() {
        // Intentionally left blank: OOBE no longer uses a top header area.
    }

    private void renderIntroOverlay() {
        float eased = easeOutCubic(introProgress);
        int overlayAlpha = Math.min(255, Math.max(0, Math.round((1f - eased) * 255f)));
        if (overlayAlpha > 0) {
            Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(244, 247, 255, overlayAlpha));
        }
    }

    private void renderLanguagePage() {
        float titleWidth = clamp(contentWidth() * 0.6f, 280f, 660f);
        float x = centeredColumnX(titleWidth);
        float y = contentTop() + 22f;

        renderStepCounter(x, y - 30f);
        renderAnimatedGreeting(x, y - 2f);
        drawResponsiveTitle(key("oobe.language.title"), x, y + 28f);
        drawBodyText(key("oobe.language.desc"), x, y + 76f, titleWidth);

        float chipY = y + 108f;
        float chipW = compactLayout() ? 112f : 122f;
        float chipH = 30f;
        renderChip(x, chipY, chipW, chipH, languageValue == 1, key("oobe.language.zh"));
        renderChip(x + chipW + 12f, chipY, chipW, chipH, languageValue == 0, "English");

        if (consumePressInBounds(x, chipY, chipW, chipH, 0) != null) {
            switchLanguage(1);
        }
        if (consumePressInBounds(x + chipW + 12f, chipY, chipW, chipH, 0) != null) {
            switchLanguage(0);
        }
    }

    private void renderScalePage(int mouseX, int mouseY) {
        float layoutWidth = clamp(contentWidth() * 0.72f, 300f, contentWidth());
        float x = centeredColumnX(layoutWidth);
        float topY = contentTop();

        renderStepCounter(x, topY - 18f);
        drawResponsiveTitle(key("oobe.scale.title"), x, topY + 12f);
        drawBodyText(key("oobe.scale.desc"), x, topY + 58f, layoutWidth - 18f);

        float pillY = topY + 86f;
        float pillW = clamp((layoutWidth - 12f) * 0.44f, 138f, 176f);
        renderPill(x, pillY, pillW, 34f, fixedScaleEnabled, key("oobe.scale.fixed"));
        renderPill(x + pillW + 12f, pillY, pillW + 12f, 34f, !fixedScaleEnabled, key("oobe.scale.follow"));

        if (consumePressInBounds(x, pillY, pillW, 34f, 0) != null) {
            fixedScaleEnabled = true;
            applyLiveScaleSettings();
        }
        if (consumePressInBounds(x + pillW + 12f, pillY, pillW + 12f, 34f, 0) != null) {
            fixedScaleEnabled = false;
            applyLiveScaleSettings();
        }

        float dropdownY = pillY + 44f;
        scaleDropdown.setLabel(key("oobe.scale.label")).setItems(SCALE_LABELS).setSelectedIndex(fixedScaleIndex).setEnabled(fixedScaleEnabled);
        scaleDropdown.renderInScreen(this, x, dropdownY, clamp(layoutWidth * 0.40f, 172f, 220f), 32f, mouseX, mouseY);
        fixedScaleIndex = scaleDropdown.getSelectedIndex();
        if (scaleDropdown.consumeSelectionChanged()) {
            applyLiveScaleSettings();
        }
    }

    private void renderTutorialPage(int mouseX, int mouseY) {
        float width = clamp(contentWidth() * 0.62f, 300f, 760f);
        float x = centeredColumnX(width);
        float y = contentTop() - 2f;
        float cardHeight = clamp(width * 0.28f, 160f, 200f);
        boolean tightHeight = availableContentHeight() < 300f;
        String[][] slides = new String[][]{
                {key("oobe.tutorial.1.title"), key("oobe.tutorial.1.desc")},
                {key("oobe.tutorial.2.title"), key("oobe.tutorial.2.desc")},
                {key("oobe.tutorial.3.title"), key("oobe.tutorial.3.desc")}
        };

        renderStepCounter(x, y - 10f);
        drawResponsiveTitle(key("oobe.tutorial.title"), x, y + 18f);

        float cardY = y + (tightHeight ? 72f : 86f);
        drawGlassCard(x, cardY, width, cardHeight, 24f, new Color(255, 255, 255, 232), new Color(229, 235, 247, 210));
        FPSMaster.fontManager.s16.drawString((tutorialIndex + 1) + " / " + slides.length, x + 24f, cardY + 20f, mutedText().getRGB());

        int alpha = Math.min(255, (int)(tutorialSlideTransition * 255f));
        Color titleColor = new Color(24, 32, 54, alpha);
        Color descColor = new Color(92, 101, 118, alpha);

        GL11.glPushMatrix();
        GL11.glTranslatef(0f, (1f - tutorialSlideTransition) * 8f, 0f);
        FPSMaster.fontManager.s18.drawString(slides[tutorialIndex][0], x + 24f, cardY + 54f, titleColor.getRGB());
        drawMultilineBodyTextWithAlpha(extendTutorialDescription(slides[tutorialIndex][1], tutorialIndex), x + 24f, cardY + 94f, width - 48f, 4, alpha);
        GL11.glPopMatrix();
    }

    private void renderFeaturesPage() {
        float layoutWidth = clamp(contentWidth() * 0.84f, 300f, contentWidth());
        float x = centeredColumnX(layoutWidth);
        float y = contentTop() - 8f;
        float gap = frameWidth() < 400f ? 8f : 12f;
        float cardWidth = (layoutWidth - gap) / 2f;
        float collapsedHeight = 72f;
        float rowSpacing = 18f;

        String[][] cards = new String[][]{
                {key("oobe.features.performance.title"), key("oobe.features.performance.desc"), key("oobe.features.performance.detail")},
                {key("oobe.features.animations.title"), key("oobe.features.animations.desc"), key("oobe.features.animations.detail")},
                {key("oobe.features.hud.title"), key("oobe.features.hud.desc"), key("oobe.features.hud.detail")},
                {key("oobe.features.background.title"), key("oobe.features.background.desc"), key("oobe.features.background.detail")}
        };

        renderStepCounter(x, y - 8f);
        drawResponsiveTitle(key("oobe.features.title"), x, y + 20f);
        drawBodyText(key("oobe.features.desc"), x, y + 66f, layoutWidth * 0.72f);

        hoveredFeature = -1;
        for (int i = 0; i < cards.length; i++) {
            int row = i / 2;
            int column = i % 2;
            float cardX = x + column * (cardWidth + gap);
            float cardY = y + 94f + row * (collapsedHeight + rowSpacing);
            float detailHeight = clamp(18f * featureExpand[i], 0f, 18f);
            float totalHeight = collapsedHeight + detailHeight;
            boolean hovered = Hover.is(cardX, cardY, cardWidth, totalHeight, getMouseX(), getMouseY());
            if (hovered) {
                hoveredFeature = i;
            }

            drawGlassCard(cardX, cardY, cardWidth, totalHeight, 22f,
                    new Color(255, 255, 255, hovered ? 236 : 228),
                    hovered ? new Color(218, 226, 243, 220) : new Color(229, 235, 247, 210));
            FPSMaster.fontManager.s18.drawString(cards[i][0], cardX + 16f, cardY + 16f, panelTitleText().getRGB());
            drawBodyText(cards[i][1], cardX + 16f, cardY + 40f, cardWidth - 32f);

            if (featureExpand[i] > 0.02f) {
                int alpha = Math.min(255, Math.max(0, (int) (featureExpand[i] * 255f)));
                Rects.fill(cardX + 16f, cardY + 62f, cardWidth - 32f, 1f, new Color(27, 35, 48, Math.max(18, alpha / 8)));
                FPSMaster.fontManager.s16.drawString(cards[i][2], cardX + 16f, cardY + 66f,
                        new Color(102, 111, 128, alpha).getRGB());
            }
        }
    }

    private void renderLoginPage(int mouseX, int mouseY) {
        float width = clamp(contentWidth() * 0.44f, 280f, 360f);
        float x = centeredColumnX(width);
        float y = contentTop() + 8f;
        boolean tightHeight = availableContentHeight() < 270f;

        renderStepCounter(x, y - 14f);
        drawResponsiveTitle(key("oobe.login.title"), x, y + 14f);
        drawBodyText(key("oobe.login.desc"), x, y + 60f, width);

        float fieldY = y + (tightHeight ? 86f : 96f);
        drawTextField(accountField, x, fieldY, width, 34f);
        drawTextField(passwordField, x, fieldY + 46f, width, 34f);

        String forgot = key("oobe.login.forgot");
        String register = key("oobe.login.register");
        float forgotX = x;
        float registerX = x + 132f;
        float linksY = fieldY + 96f;
        boolean forgotHovered = Hover.is(forgotX, linksY - 2f, FPSMaster.fontManager.s18.getStringWidth(forgot), 18f, mouseX, mouseY);
        boolean registerHovered = Hover.is(registerX, linksY - 2f, FPSMaster.fontManager.s18.getStringWidth(register), 18f, mouseX, mouseY);
        forgotHoverAnim = (float) AnimMath.base(forgotHoverAnim, forgotHovered ? 1.0 : 0.0, 0.24);
        registerHoverAnim = (float) AnimMath.base(registerHoverAnim, registerHovered ? 1.0 : 0.0, 0.24);
        FPSMaster.fontManager.s18.drawString(forgot, forgotX, linksY,
                blendColor(accentText(), new Color(82, 100, 142), forgotHoverAnim).getRGB());
        FPSMaster.fontManager.s18.drawString(register, registerX, linksY,
                blendColor(accentText(), new Color(82, 100, 142), registerHoverAnim).getRGB());
        if (!hasActiveModal() && consumePressInBounds(forgotX, linksY - 2f, FPSMaster.fontManager.s18.getStringWidth(forgot), 18f, 0) != null) {
            openLink("https://fpsmaster.top/forgot");
        }
        if (!hasActiveModal() && consumePressInBounds(registerX, linksY - 2f, FPSMaster.fontManager.s18.getStringWidth(register), 18f, 0) != null) {
            openLink("https://fpsmaster.top/login");
        }

        float buttonY = fieldY + 130f;
        loginButton.setText(key("oobe.login.submit")).setPrimary(!loginSkipped).renderInScreen(this, x, buttonY, 82f, 28f, mouseX, mouseY);
        skipLoginButton.setText(key("oobe.login.skip")).setPrimary(loginSkipped).renderInScreen(this, x + 92f, buttonY, 82f, 28f, mouseX, mouseY);
    }

    private void renderOptionsPage() {
        float mainWidth = clamp(contentWidth() * 0.86f, 300f, contentWidth());
        float x = centeredColumnX(mainWidth);
        float y = contentTop() - 8f;
        renderStepCounter(x, y - 8f);
        drawResponsiveTitle(key("oobe.options.title"), x, y + 20f);
        drawBodyText(key("oobe.options.desc"), x, y + 66f, mainWidth);

        float cardY = y + 106f;
        float gap = 16f;
        float cardWidth = (mainWidth - gap) / 2f;
        float cardHeight = clamp(cardWidth * 0.55f, 160f, 200f);
        renderOptionInfoCard(x, cardY, cardWidth, cardHeight, antiCheatEnabled,
                key("oobe.options.anticheat"),
                isChinese()
                        ? "FPSMaster AntiCheat 帮助您在支持的服务器上获取受信任标识，避免遭受无意义的质疑；开启 FPSMaster AntiCheat 后，您的数据才能计入社区排行榜。如果您无需这两项功能，您可以选择不开启。"
                        : "FPSMaster AntiCheat helps you obtain a trusted mark on supported servers and reduces unnecessary suspicion. Only when it is enabled can your data be counted in community leaderboards.");
        renderOptionInfoCard(x + cardWidth + gap, cardY, cardWidth, cardHeight, anonymousDataEnabled,
                key("oobe.options.anonymous"),
                isChinese()
                        ? "收集匿名信息功能不会上传任何您的敏感信息，您的玩家名、账号凭证、发送的消息等都不会被上传。我们收集的信息主要包括：设备配置、您在公开服务器上的部分游玩行为轨迹。这将用于更好地了解客户端的使用情况，帮助我们改善客户端服务。"
                        : "Anonymous data collection never uploads sensitive information such as your player name, account credentials or chat messages. We only collect device profile and limited gameplay behavior on public servers to help improve the client.");

        if (consumePressInBounds(x, cardY, cardWidth, cardHeight, 0) != null) {
            antiCheatEnabled = !antiCheatEnabled;
        }
        if (consumePressInBounds(x + cardWidth + gap, cardY, cardWidth, cardHeight, 0) != null) {
            anonymousDataEnabled = !anonymousDataEnabled;
        }
    }

    private void renderGuideEntryPage() {
        float mainWidth = clamp(contentWidth() * 0.48f, 280f, 520f);
        float x = centeredColumnX(mainWidth);
        float y = contentTop() + 2f;

        renderStepCounter(x, y - 12f);
        drawResponsiveTitle(key("oobe.guide.title"), x, y + 14f);
        drawBodyText(key("oobe.guide.desc"), x, y + 60f, mainWidth);

        float cardY = y + 92f;
        renderChoiceCard(x, cardY, mainWidth, 40f, enterGuide, key("oobe.guide.enter"));
        renderChoiceCard(x, cardY + 50f, mainWidth, 40f, !enterGuide, key("oobe.guide.skip"));

        if (consumePressInBounds(x, cardY, mainWidth, 40f, 0) != null) {
            enterGuide = true;
        }
        if (consumePressInBounds(x, cardY + 50f, mainWidth, 40f, 0) != null) {
            enterGuide = false;
        }
    }

    private void renderQaPage() {
        float cardWidth = clamp(contentWidth() * 0.52f, 300f, 620f);
        float x = centeredColumnX(cardWidth);
        float y = contentTop() - 2f;

        String[][] questions = new String[][]{
                {key("oobe.qa.1.question"), key("oobe.qa.1.a"), key("oobe.qa.1.b"), key("oobe.qa.1.c")},
                {key("oobe.qa.2.question"), key("oobe.qa.2.a"), key("oobe.qa.2.b"), key("oobe.qa.2.c")},
                {key("oobe.qa.3.question"), key("oobe.qa.3.a"), key("oobe.qa.3.b"), key("oobe.qa.3.c")}
        };

        renderStepCounter(x, y - 12f);
        drawResponsiveTitle(key("oobe.qa.title"), x, y + 14f);
        drawBodyText(key("oobe.qa.desc"), x, y + 60f, cardWidth);

        float cardY = y + 90f;
        float qaHeight = clamp(cardWidth * 0.48f, 210f, 280f);
        drawGlassCard(x, cardY, cardWidth, qaHeight, 22f, new Color(255, 255, 255, 232), new Color(229, 235, 247, 210));
        FPSMaster.fontManager.s16.drawString((qaStep + 1) + " / " + questions.length, x + 18f, cardY + 16f, mutedText().getRGB());
        drawPanelTitle(questions[qaStep][0], x + 18f, cardY + 44f);

        if (qaStep == 1) {
            renderBackgroundPreviewChoices(x + 18f, cardY + 76f, cardWidth - 36f, questions[qaStep]);
        } else {
            for (int i = 1; i <= 3; i++) {
                float optionY = cardY + 76f + (i - 1) * 36f;
                boolean selected = qaAnswers[qaStep] == i - 1;
                boolean hovered = Hover.is(x + 18f, optionY, cardWidth - 36f, 30f, getMouseX(), getMouseY());
                qaOptionHover[i - 1] = (float) AnimMath.base(qaOptionHover[i - 1], hovered ? 1.0 : 0.0, 0.22);
                renderQaOption(x + 18f, optionY, cardWidth - 36f, 30f, selected, hovered, qaOptionHover[i - 1], qaOptionPress[i - 1], questions[qaStep][i]);
                if (!hasActiveModal() && consumePressInBounds(x + 18f, optionY, cardWidth - 36f, 30f, 0) != null) {
                    qaOptionPress[i - 1] = 1.0f;
                    qaAnswers[qaStep] = i - 1;
                    applyQaAnswer();
                    if (qaStep < questions.length - 1) {
                        qaStep++;
                    }
                }
            }
        }

    }

    private void renderFooter(int mouseX, int mouseY) {
        float barY = guiHeight - footerHeight();
        Rects.fill(0f, barY, guiWidth, 1f, new Color(226, 232, 246, 112));
        Rects.fill(0f, barY, guiWidth, footerHeight(), new Color(255, 255, 255, 76));

        float progressX = pagePadding();
        float progressY = barY + (footerHeight() - 5f) / 2f;
        FPSMaster.fontManager.s14.drawString((page + 1) + " / " + PAGE_COUNT, progressX, progressY - 1f, new Color(110, 119, 136, 178).getRGB());
        float dotsX = progressX + 44f;
        for (int i = 0; i < PAGE_COUNT; i++) {
            Color color = i == page
                    ? new Color(104, 117, 247, 255)
                    : (i < page ? new Color(104, 117, 247, 96) : new Color(27, 35, 48, 34));
            Rects.rounded(Math.round(dotsX + i * 11f), Math.round(progressY), 5, 5, 3, color.getRGB());
        }

        float nextW = 84f;
        float backW = 84f;
        float gap = 12f;
        float nextX = guiWidth - pagePadding() - nextW;
        float backX = nextX - gap - backW;
        float buttonY = barY + (footerHeight() - 26f) / 2f;
        boolean allowNext = !hasActiveModal() && (page != 2 || tutorialPlaybackComplete);
        backButton.setText(key("oobe.back")).setEnabled(page > 0 && !hasActiveModal()).setPrimary(false).renderInScreen(this, backX, buttonY, backW, 26f, mouseX, mouseY);
        nextButton.setText(getNextLabel()).setEnabled(allowNext).setPrimary(true).renderInScreen(this, nextX, buttonY, nextW, 26f, mouseX, mouseY);
    }

    private void onNext() {
        if (page == 6 && !enterGuide) {
            finishOobe();
            return;
        }
        if (page >= PAGE_COUNT - 1) {
            finishOobe();
            return;
        }
        page++;
        pageMotion = 1f;
        pageMotionDirection = 1;
    }

    private void finishOobe() {
        applySelections();
        mc.displayGuiScreen(new MainMenu());
    }

    private void applySelections() {
        ClientSettings.language.setValue(languageValue);
        ClientSettings.fixedScaleEnabled.setValue(fixedScaleEnabled);
        ClientSettings.fixedScale.setValue(fixedScaleIndex);

        FPSMaster.configManager.configure.background = backgroundChoice;
        FPSMaster.configManager.configure.antiCheatEnabled = antiCheatEnabled;
        FPSMaster.configManager.configure.anonymousDataEnabled = anonymousDataEnabled;
        FPSMaster.configManager.configure.oobeCompleted = true;
        System.out.println("[OOBE] Set oobeCompleted = true");

        applyDefaultModules();
        if (enterGuide) {
            applyQaModules();
        }

        try {
            FPSMaster.configManager.saveConfig("default");
        } catch (FileException e) {
            e.printStackTrace();
            System.err.println("Failed to save OOBE configuration: " + e.getMessage());
        }
    }

    private void applyDefaultModules() {
        setModuleEnabled(Performance.class, true);
        setModuleEnabled(OldAnimations.class, true);
        setModuleEnabled(ItemPhysics.class, true);
        setModuleEnabled(FPSDisplay.class, true);
        setModuleEnabled(Keystrokes.class, true);
        setModuleEnabled(CPSDisplay.class, true);
        setModuleEnabled(ComboDisplay.class, false);
        setModuleEnabled(PingDisplay.class, false);
        setModuleEnabled(DirectionDisplay.class, false);
        setModuleEnabled(CoordsDisplay.class, false);
        setModuleEnabled(InventoryDisplay.class, false);
    }

    private void applyQaModules() {
        if (qaAnswers[0] == 2) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(DirectionDisplay.class, true);
        }

        if (qaAnswers[1] == 0) {
            backgroundChoice = "classic";
        } else if (qaAnswers[1] == 1) {
            backgroundChoice = "shader";
        } else if (qaAnswers[1] == 2) {
            backgroundChoice = "panorama_3";
        }

        if (qaAnswers[2] == 0) {
            setModuleEnabled(FPSDisplay.class, true);
            setModuleEnabled(Keystrokes.class, true);
            setModuleEnabled(CPSDisplay.class, true);
            setModuleEnabled(ComboDisplay.class, true);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(CoordsDisplay.class, false);
            setModuleEnabled(InventoryDisplay.class, false);
        } else if (qaAnswers[2] == 1) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, true);
            setModuleEnabled(CoordsDisplay.class, true);
            setModuleEnabled(InventoryDisplay.class, true);
        } else if (qaAnswers[2] == 2) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(CoordsDisplay.class, false);
            setModuleEnabled(InventoryDisplay.class, false);
            setModuleEnabled(DirectionDisplay.class, false);
        }
    }

    private void setModuleEnabled(Class<?> type, boolean enabled) {
        try {
            Module module = FPSMaster.moduleManager.getModule(type, true);
            module.set(enabled);
        } catch (IllegalStateException ignored) {
        }
    }

    private void applyQaAnswer() {
        if (qaStep == 0) {
            if (qaAnswers[0] == 0) {
                setFeatureCount(4);
            } else if (qaAnswers[0] == 1) {
                setFeatureCount(5);
            } else if (qaAnswers[0] == 2) {
                setFeatureCount(3);
            }
        }
        if (qaStep == 1) {
            if (qaAnswers[1] == 0) {
                backgroundChoice = "classic";
            } else if (qaAnswers[1] == 1) {
                backgroundChoice = "shader";
            } else if (qaAnswers[1] == 2) {
                backgroundChoice = "panorama_3";
            }
        }
    }

    private void setFeatureCount(int count) {
        featureCount = count;
    }

    private String getFeatureCountLabel() {
        return isChinese() ? featureCount + " " + key("oobe.features.count.unit") : String.valueOf(featureCount);
    }

    private String backgroundLabel() {
        if ("classic".equals(backgroundChoice)) {
            return key("oobe.background.classic");
        }
        if ("shader".equals(backgroundChoice)) {
            return key("oobe.background.shader");
        }
        if ("panorama_2".equals(backgroundChoice)) {
            return key("oobe.background.panorama2");
        }
        if ("panorama_3".equals(backgroundChoice)) {
            return key("oobe.background.panorama3");
        }
        return key("oobe.background.panorama1");
    }

    private String animatedGreeting() {
        return GREETINGS[greetingIndex];
    }

    private void renderAnimatedGreeting(float x, float y) {
        float eased = easeOutCubic(greetingTransition);
        int currentAlpha = Math.min(255, Math.max(0, Math.round(eased * 255f)));
        Color currentColor = new Color(accentText().getRed(), accentText().getGreen(), accentText().getBlue(), currentAlpha);

        if (greetingTransition < 1f && greetingPreviousText != null && !greetingPreviousText.isEmpty()) {
            float previousProgress = 1f - eased;
            int previousAlpha = Math.min(255, Math.max(0, Math.round(previousProgress * 255f)));
            Color previousColor = new Color(accentText().getRed(), accentText().getGreen(), accentText().getBlue(), previousAlpha);
            FPSMaster.fontManager.s18.drawString(greetingPreviousText, x, y - eased * 8f, previousColor.getRGB());
        }

        FPSMaster.fontManager.s18.drawString(greetingCurrentText, x, y + (1f - eased) * 8f, currentColor.getRGB());
    }

    private void switchLanguage(int newLanguage) {
        if (languageValue == newLanguage) {
            return;
        }
        languageValue = newLanguage;
        setPreviewLanguage(languageValue);
        updateTextFieldPlaceholders();
    }

    private String getNextLabel() {
        if (page == PAGE_COUNT - 1 || (page == 6 && !enterGuide)) {
            return key("oobe.finish");
        }
        return key("oobe.next");
    }

    private void renderPill(float x, float y, float width, float height, boolean active, String label) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 18,
                active ? new Color(104, 117, 247, 236) : new Color(255, 255, 255, 242));
        FPSMaster.fontManager.s18.drawCenteredString(label, x + width / 2f, y + height / 2f - 5f,
                active ? Color.WHITE.getRGB() : new Color(54, 65, 89).getRGB());
    }

    private void renderChip(float x, float y, float width, float height, boolean active, String label) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 17,
                active ? new Color(104, 117, 247, 236) : new Color(255, 255, 255, 170));
        FPSMaster.fontManager.s16.drawCenteredString(label, x + width / 2f, y + height / 2f - 4f,
                active ? Color.WHITE.getRGB() : new Color(88, 97, 114).getRGB());
    }

    private void renderChoiceCard(float x, float y, float width, float height, boolean selected, String label) {
        drawGlassCard(x, y, width, height, 18f,
                selected ? new Color(118, 133, 255, 226) : new Color(255, 255, 255, 234),
                selected ? new Color(164, 176, 255, 170) : new Color(229, 235, 247, 210));
        FPSMaster.fontManager.s16.drawString(label, x + 18f, y + height / 2f - 5f,
                selected ? Color.WHITE.getRGB() : new Color(42, 52, 78).getRGB());
    }

    private void renderOptionInfoCard(float x, float y, float width, float height, boolean enabled, String title, String description) {
        boolean hovered = Hover.is(x, y, width, height, getMouseX(), getMouseY());
        drawGlassCard(x, y, width, height, 20f,
                new Color(255, 255, 255, hovered ? 242 : 236),
                hovered ? new Color(212, 221, 242, 224) : new Color(229, 235, 247, 210));
        FPSMaster.fontManager.s18.drawString(title, x + 18f, y + 18f, panelTitleText().getRGB());
        drawMultilineBodyText(description, x + 18f, y + 46f, width - 92f, 7);
        renderSmallSwitch(x + width - 52f, y + 18f, enabled, hovered);
    }

    private void renderSmallSwitch(float x, float y, boolean enabled, boolean hovered) {
        Rects.rounded(Math.round(x), Math.round(y), 34, 18, 9,
                enabled
                        ? new Color(104, 117, 247, hovered ? 246 : 236)
                        : new Color(214, 220, 232, hovered ? 255 : 246));
        Rects.rounded(Math.round(x + (enabled ? 17f : 2f)), Math.round(y + 2f), 14, 14, 7, Color.WHITE);
    }

    private void renderQaOption(float x, float y, float width, float height, boolean selected, boolean hovered, float hoverAnim, float pressAnim, String label) {
        float inset = pressAnim * 1.2f;
        float drawX = x + inset;
        float drawY = y + inset;
        float drawWidth = width - inset * 2f;
        float drawHeight = height - inset * 2f;
        drawGlassCard(x, y, width, height, 18f,
                new Color(255, 255, 255, 0),
                new Color(255, 255, 255, 0));
        Color fill = selected
                ? new Color(122, 139, 255, 220)
                : (hovered ? new Color(240, 244, 255, 248) : new Color(247, 249, 255, 246));
        Color border = selected
                ? new Color(164, 176, 255, 170)
                : new Color(229, 235, 247, 210 + (int) (hoverAnim * 20f));
        Rects.rounded(Math.round(drawX), Math.round(drawY), Math.round(drawWidth), Math.round(drawHeight), 18, fill.getRGB());
        Rects.roundedBorder(Math.round(drawX), Math.round(drawY), Math.round(drawWidth), Math.round(drawHeight), 18, 1f, border.getRGB(), border.getRGB());
        FPSMaster.fontManager.s18.drawString(label, x + 14f, y + 6f + pressAnim * 0.6f, selected ? Color.WHITE.getRGB() : panelTitleText().getRGB());
    }

    private void drawPreviewSurface(float x, float y, float width, float height) {
        drawGlassCard(x, y, width, height, 18f, new Color(24, 35, 59, 236), new Color(255, 255, 255, 56));
        Images.draw(PREVIEW_IMAGE, x, y, width, height, -1);
        Rects.fill(x, y, width, height, new Color(8, 12, 20, 72));
        Rects.fill(x, y + height * 0.58f, width, height * 0.42f, new Color(10, 14, 24, 64));
        float previewScale = Math.min(width / 220f, height / 160f);
        renderKeystrokesPreview(x + 18f * previewScale, y + 18f * previewScale, previewScale);
    }

    private void renderKeystrokesPreview(float x, float y, float previewScale) {
        float scale = (0.72f + fixedScaleIndex * 0.07f) * previewScale;
        float size = 24f * scale;
        float gap = 5f * scale;
        renderKey(x + size + gap, y, size, "W");
        renderKey(x, y + size + gap, size, "A");
        renderKey(x + size + gap, y + size + gap, size, "S");
        renderKey(x + (size + gap) * 2f, y + size + gap, size, "D");
    }

    private void renderKey(float x, float y, float size, String text) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(size), Math.round(size), 9, new Color(18, 24, 36, 214));
        Rects.roundedBorder(Math.round(x), Math.round(y), Math.round(size), Math.round(size), 9, 1f,
                new Color(255, 255, 255, 58).getRGB(), new Color(255, 255, 255, 58).getRGB());
        FPSMaster.fontManager.s18.drawCenteredString(text, x + size / 2f, y + size / 2f - 4f, Color.WHITE.getRGB());
    }

    private void renderBackgroundPreviewChoices(float x, float y, float width, String[] question) {
        String[] previewIds = new String[]{"classic", "shader", "panorama_3"};
        for (int i = 0; i < 3; i++) {
            float optionY = y + i * 36f;
            boolean selected = qaAnswers[qaStep] == i;
            boolean hovered = Hover.is(x, optionY, width, 30f, getMouseX(), getMouseY());
            qaOptionHover[i] = (float) AnimMath.base(qaOptionHover[i], hovered ? 1.0 : 0.0, 0.22);
            renderQaOption(x, optionY, width, 30f, selected, hovered, qaOptionHover[i], qaOptionPress[i], question[i + 1]);
            renderMiniBackgroundPreview(x + 8f, optionY + 4f, 42f, 20f, previewIds[i]);
            if ("shader".equals(previewIds[i]) && !isShaderBackgroundSupported()) {
                FPSMaster.fontManager.s16.drawString(isChinese() ? "当前设备不支持" : "Unsupported", x + width - 118f, optionY + 6f, new Color(167, 92, 92).getRGB());
                continue;
            }
            if (!hasActiveModal() && consumePressInBounds(x, optionY, width, 30f, 0) != null) {
                qaOptionPress[i] = 1.0f;
                if ("shader".equals(previewIds[i]) && !ensureShaderBackgroundConfirmed()) {
                    return;
                }
                qaAnswers[qaStep] = i;
                applyQaAnswer();
                if (qaStep < 2) {
                    qaStep++;
                }
            }
        }
    }

    private boolean ensureShaderBackgroundConfirmed() {
        if (!isShaderBackgroundSupported()) {
            shaderUnsupportedDialogVisible = true;
            pendingBackgroundChoice = null;
            return false;
        }
        double score = runShaderBenchmark();
        shaderBenchmarkScore = score;
        if (score < 18.0) {
            pendingBackgroundChoice = "shader";
            shaderWarningDialogVisible = true;
            return false;
        }
        return true;
    }

    private double runShaderBenchmark() {
        long start = System.nanoTime();
        float width = clamp(guiWidth * 0.26f, 180f, 280f);
        float height = clamp(guiHeight * 0.18f, 120f, 180f);
        float x = guiWidth - width - 26f;
        float y = 18f;
        for (int i = 0; i < 180; i++) {
            int alpha = 12 + (i % 6) * 6;
            Rects.fill(x, y, width, height, new Color(44 + i % 24, 76 + i % 36, 128 + i % 40, alpha));
        }
        GL11.glFlush();
        long elapsed = Math.max(1L, System.nanoTime() - start);
        return 1800000.0 / elapsed;
    }

    private boolean isShaderBackgroundSupported() {
        return OSUtil.supportShader() && OpenGlHelper.shadersSupported && OpenGlHelper.framebufferSupported;
    }

    private void confirmShaderBackgroundSelection() {
        shaderWarningDialogVisible = false;
        pendingBackgroundChoice = null;
    }

    private void cancelShaderBackgroundSelection() {
        shaderWarningDialogVisible = false;
        pendingBackgroundChoice = null;
    }

    private boolean hasActiveModal() {
        return shaderWarningDialogVisible || shaderUnsupportedDialogVisible;
    }

    private void renderShaderDialogs(int mouseX, int mouseY) {
        if (!hasActiveModal()) {
            return;
        }
        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(18, 22, 32, 92));
        float width = clamp(guiWidth * 0.34f, 320f, 420f);
        float height = shaderUnsupportedDialogVisible ? 154f : 176f;
        float x = (guiWidth - width) / 2f;
        float y = (guiHeight - height) / 2f;
        drawGlassCard(x, y, width, height, 22f, new Color(255, 255, 255, 244), new Color(229, 235, 247, 220));
        String title = shaderUnsupportedDialogVisible
                ? (isChinese() ? "Shader 背景不可用" : "Shader background unsupported")
                : (isChinese() ? "是否继续使用？" : "Continue anyway?");
        String body = shaderUnsupportedDialogVisible
                ? (isChinese() ? "检测到当前设备不支持 shader 背景，已禁用该选项。" : "Your device does not support shader backgrounds. This option has been disabled.")
                : (isChinese()
                    ? "您的 GPU 性能较低，使用本背景样式可能引起卡顿，是否继续？\n基准分数: " + formatBenchmarkScore(shaderBenchmarkScore)
                    : "Your GPU appears to be low performance. This background may cause stutter.\nBenchmark score: " + formatBenchmarkScore(shaderBenchmarkScore));
        drawPanelTitle(title, x + 20f, y + 24f);
        drawBodyText(body, x + 20f, y + 60f, width - 40f);
        if (shaderUnsupportedDialogVisible) {
            shaderUnsupportedOkButton.setText(isChinese() ? "知道了" : "OK").renderInScreen(this, x + width - 108f, y + height - 44f, 88f, 30f, mouseX, mouseY);
        } else {
            shaderCancelButton.setText(isChinese() ? "取消" : "Cancel").renderInScreen(this, x + width - 210f, y + height - 44f, 88f, 30f, mouseX, mouseY);
            shaderContinueButton.setText(isChinese() ? "继续" : "Continue").renderInScreen(this, x + width - 110f, y + height - 44f, 88f, 30f, mouseX, mouseY);
        }
    }

    private String formatBenchmarkScore(double score) {
        int integer = (int) Math.round(score * 10.0);
        return String.valueOf(integer / 10.0);
    }

    private String resolveHoveredBackgroundPreview(int mouseX, int mouseY) {
        if (page != 7 || qaStep != 1 || hasActiveModal()) {
            return null;
        }
        float cardWidth = clamp(contentWidth() * 0.52f, 420f, 620f);
        float x = centeredColumnX(cardWidth);
        float y = contentTop() - 2f;
        float cardY = y + 90f;
        float optionsX = x + 18f;
        float optionsY = cardY + 76f;
        float optionsWidth = cardWidth - 36f;
        String[] previewIds = new String[]{"classic", "shader", "panorama_3"};
        for (int i = 0; i < previewIds.length; i++) {
            float optionY = optionsY + i * 36f;
            if (Hover.is(optionsX, optionY, optionsWidth, 30f, mouseX, mouseY)) {
                return previewIds[i];
            }
        }
        return null;
    }

    private void renderHoveredBackgroundPreview() {
        if (hoveredBackgroundPreview == null) {
            return;
        }
        if ("classic".equals(hoveredBackgroundPreview)) {
            Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(28, 34, 45, 76));
        } else if ("shader".equals(hoveredBackgroundPreview)) {
            Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(28, 48, 92, 40));
            Rects.fill(0f, 0f, guiWidth, guiHeight * 0.55f, new Color(76, 116, 196, 28));
            Rects.fill(0f, guiHeight * 0.32f, guiWidth, guiHeight * 0.68f, new Color(38, 66, 138, 34));
        } else if ("panorama_3".equals(hoveredBackgroundPreview)) {
            Images.draw(PANORAMA_THREE, 0f, 0f, guiWidth, guiHeight, new Color(255, 255, 255, 110));
            Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(22, 26, 34, 58));
        }
    }

    private void renderMiniBackgroundPreview(float x, float y, float width, float height, String id) {
        if ("classic".equals(id)) {
            Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 8, new Color(43, 50, 65).getRGB());
            return;
        }
        if ("shader".equals(id)) {
            Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 8, new Color(46, 71, 173).getRGB());
            Rects.fill(x, y, width, height, new Color(143, 160, 255, 72));
            return;
        }
        Images.draw(PANORAMA_THREE, x, y, width, height, -1);
    }

    private void drawCoverCard(float x, float y, float width, float height) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 24, new Color(34, 55, 131, 242));
        Rects.fill(x, y, width, height, new Color(108, 133, 248, 98));
        Rects.fill(x, y + height * 0.44f, width, height * 0.56f, new Color(12, 18, 31, 28));
        FPSMaster.fontManager.s18.drawString(pageStepLabel(), x + 18f, y + 19f, new Color(245, 248, 255, 220).getRGB());
        FPSMaster.fontManager.s24.drawString(key("oobe.options.cover.title"), x + 18f, y + 52f, inverseTitleText().getRGB());
        drawInverseBodyText(key("oobe.options.cover.desc"), x + 18f, y + 84f, width - 36f);
    }

    private void drawGlassCard(float x, float y, float width, float height, float radius, Color fill, Color border) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), Math.round(radius), fill.getRGB());
        Rects.roundedBorder(Math.round(x), Math.round(y), Math.round(width), Math.round(height), Math.round(radius), 1f, border.getRGB(), border.getRGB());
    }

    private void drawTextField(TextField field, float x, float y, float width, float height) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 16, new Color(255, 255, 255, 244).getRGB());
        field.drawTextBox(x, y, width, height);
        PointerEvent outsideClick = peekAnyPress();
        if (outsideClick != null && !Hover.is(x, y, width, height, outsideClick.x, outsideClick.y)) {
            field.setFocused(false);
        }
        PointerEvent click = consumePressInBounds(x, y, width, height, 0);
        if (click != null) {
            field.mouseClicked(click.x, click.y, click.button);
        }
    }

    private void drawResponsiveTitle(String text, float x, float y) {
        if (contentWidth() < 520f) {
            FPSMaster.fontManager.s24.drawString(text, x, y, titleText().getRGB());
        } else if (contentWidth() < 700f) {
            FPSMaster.fontManager.s28.drawString(text, x, y, titleText().getRGB());
        } else {
            FPSMaster.fontManager.s36.drawString(text, x, y, titleText().getRGB());
        }
    }

    private void drawPanelTitle(String text, float x, float y) {
        if (contentWidth() < 520f) {
            FPSMaster.fontManager.s24.drawString(text, x, y, panelTitleText().getRGB());
        } else if (contentWidth() < 700f) {
            FPSMaster.fontManager.s28.drawString(text, x, y, panelTitleText().getRGB());
        } else {
            FPSMaster.fontManager.s36.drawString(text, x, y, panelTitleText().getRGB());
        }
    }

    private void drawBodyText(String text, float x, float y, float width) {
        boolean useSmallFont = frameWidth() < 500f;
        if (width < 300f) {
            if (useSmallFont) {
                FPSMaster.fontManager.s16.drawString(text, x, y, bodyText().getRGB());
            } else {
                FPSMaster.fontManager.s18.drawString(text, x, y, bodyText().getRGB());
            }
        } else {
            if (useSmallFont) {
                FPSMaster.fontManager.s16.drawString(text, x, y, new Color(92, 101, 118).getRGB());
            } else {
                FPSMaster.fontManager.s18.drawString(text, x, y, new Color(92, 101, 118).getRGB());
            }
        }
    }

    private void drawMultilineBodyText(String text, float x, float y, float width, int maxLines) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String[] segments = text.split("(?<=[，。：；！？ ])|(?= )");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (int i = 0; i < segments.length && lineIndex < maxLines; i++) {
            String candidate = line.toString() + segments[i];
            if (FPSMaster.fontManager.s16.getStringWidth(candidate) > width && line.length() > 0) {
                FPSMaster.fontManager.s16.drawString(line.toString(), x, y + lineIndex * 16f, new Color(92, 101, 118).getRGB());
                line = new StringBuilder(segments[i]);
                lineIndex++;
            } else {
                line.append(segments[i]);
            }
        }
        if (lineIndex < maxLines && line.length() > 0) {
            FPSMaster.fontManager.s16.drawString(line.toString(), x, y + lineIndex * 16f, new Color(92, 101, 118).getRGB());
        }
    }

    private void drawMultilineBodyTextWithAlpha(String text, float x, float y, float width, int maxLines, int alpha) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (int i = 0; i < words.length && lineIndex < maxLines; i++) {
            String candidate = line.length() == 0 ? words[i] : line + " " + words[i];
            if (FPSMaster.fontManager.s16.getStringWidth(candidate) > width && line.length() > 0) {
                FPSMaster.fontManager.s16.drawString(line.toString(), x, y + lineIndex * 16f, new Color(92, 101, 118, alpha).getRGB());
                line = new StringBuilder(words[i]);
                lineIndex++;
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (lineIndex < maxLines && line.length() > 0) {
            FPSMaster.fontManager.s16.drawString(line.toString(), x, y + lineIndex * 16f, new Color(92, 101, 118, alpha).getRGB());
        }
    }

    private void drawInverseBodyText(String text, float x, float y, float width) {
        if (width < 280f) {
            FPSMaster.fontManager.s18.drawString(text, x, y, inverseBodyText().getRGB());
        } else {
            FPSMaster.fontManager.s18.drawString(text, x, y, new Color(245, 248, 255, 224).getRGB());
        }
    }

    private void renderStepCounter(float x, float y) {
        FPSMaster.fontManager.s16.drawString(pageStepLabel(), x, y, mutedText().getRGB());
    }

    private float centeredColumnX(float width) {
        return (guiWidth - width) / 2f;
    }

    private void updateTutorialAutoplay() {
        if (page != 2) {
            tutorialPlaybackComplete = true;
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - savedTutorialStartedAt);
        int slide = (int) ((elapsed / TUTORIAL_SLIDE_DURATION_MS) % 3L);
        if (slide != tutorialIndex) {
            tutorialPrevSlide = tutorialIndex;
            tutorialSlideTransition = 0f;
        }
        tutorialIndex = slide;
        if (elapsed >= TUTORIAL_SLIDE_DURATION_MS * 3L) {
            tutorialPlaybackComplete = true;
        }
    }

    private String extendTutorialDescription(String base, int index) {
        if (isChinese()) {
            if (index == 0) {
                return base + " 你可以在这里开启、关闭或调整客户端的大部分功能，并快速完成最常用的配置。";
            }
            if (index == 1) {
                return base + " 常见 HUD 组件都可以自由摆放到更适合自己的位置，打造更顺手的游戏界面。";
            }
            return base + " 调整到舒服的尺寸后，游戏内阅读信息会更自然，也能减少不必要的视觉干扰。";
        }
        if (index == 0) {
            return base + " Most client features can be enabled, disabled or configured from there with only a few clicks.";
        }
        if (index == 1) {
            return base + " Common HUD widgets can be moved into positions that better match your play style.";
        }
        return base + " Once resized to a comfortable scale, the HUD will be easier to read and less distracting in game.";
    }

    private String pageStepLabel() {
        return isChinese() ? "第 " + (page + 1) + " 步" : "Step " + (page + 1);
    }

    private void updateTextFieldPlaceholders() {
        accountField.placeHolder = key("oobe.login.account.placeholder");
        passwordField.placeHolder = key("oobe.login.password.placeholder");
    }

    private void applyLiveScaleSettings() {
        ClientSettings.fixedScaleEnabled.setValue(fixedScaleEnabled);
        ClientSettings.fixedScale.setValue(fixedScaleIndex);
    }

    private void initSessionStateIfNeeded() {
        if (sessionStateInitialized) {
            return;
        }
        savedPage = 0;
        savedLanguageValue = ClientSettings.language.getValue();
        savedTutorialIndex = 0;
        savedAntiCheatEnabled = FPSMaster.configManager.configure.antiCheatEnabled;
        savedAnonymousDataEnabled = FPSMaster.configManager.configure.anonymousDataEnabled;
        savedEnterGuide = true;
        savedQaStep = 0;
        savedQaAnswers[0] = -1;
        savedQaAnswers[1] = -1;
        savedQaAnswers[2] = -1;
        savedBackgroundChoice = FPSMaster.configManager.configure.background == null ? "panorama_1" : FPSMaster.configManager.configure.background;
        savedLoginSkipped = true;
        savedFeatureCount = 5;
        savedAccountText = "";
        savedPasswordText = "";
        sessionStateInitialized = true;
    }

    private void restoreSessionState() {
        page = savedPage;
        languageValue = savedLanguageValue;
        fixedScaleEnabled = ClientSettings.fixedScaleEnabled.getValue();
        fixedScaleIndex = ClientSettings.fixedScale.getValue();
        tutorialIndex = savedTutorialIndex;
        antiCheatEnabled = savedAntiCheatEnabled;
        anonymousDataEnabled = savedAnonymousDataEnabled;
        enterGuide = savedEnterGuide;
        qaStep = savedQaStep;
        qaAnswers[0] = savedQaAnswers[0];
        qaAnswers[1] = savedQaAnswers[1];
        qaAnswers[2] = savedQaAnswers[2];
        backgroundChoice = savedBackgroundChoice;
        loginSkipped = savedLoginSkipped;
        featureCount = savedFeatureCount;
    }

    private void syncSessionState() {
        savedPage = page;
        savedLanguageValue = languageValue;
        savedTutorialIndex = tutorialIndex;
        savedAntiCheatEnabled = antiCheatEnabled;
        savedAnonymousDataEnabled = anonymousDataEnabled;
        savedEnterGuide = enterGuide;
        savedQaStep = qaStep;
        savedQaAnswers[0] = qaAnswers[0];
        savedQaAnswers[1] = qaAnswers[1];
        savedQaAnswers[2] = qaAnswers[2];
        savedBackgroundChoice = backgroundChoice;
        savedLoginSkipped = loginSkipped;
        savedFeatureCount = featureCount;
        if (accountField != null) {
            savedAccountText = accountField.getText();
        }
        if (passwordField != null) {
            savedPasswordText = passwordField.getText();
        }
    }

    private Color blendColor(Color from, Color to, float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int a = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(r, g, b, a);
    }

    private float easeOutCubic(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        float inverse = 1f - clamped;
        return 1f - inverse * inverse * inverse;
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    private void setPreviewLanguage(int value) {
        try {
            FPSMaster.i18n.read(value == 1 ? "zh_cn" : "en_us");
        } catch (FileException ignored) {
        }
    }

    private String key(String key) {
        return FPSMaster.i18n.get(key);
    }

    private boolean isChinese() {
        return languageValue == 1;
    }

    private float frameLeft() {
        return 12f;
    }

    private float frameTop() {
        return 12f;
    }

    private float frameWidth() {
        return guiWidth - 24f;
    }

    private float frameHeight() {
        return guiHeight - 24f;
    }

    private float frameRight() {
        return frameLeft() + frameWidth();
    }

    private float frameBottom() {
        return frameTop() + frameHeight();
    }

    private float topBarHeight() {
        return 0f;
    }

    private float footerHeight() {
        return 38f;
    }

    private float pagePadding() {
        float width = frameWidth();
        if (width < 400f) {
            return clamp(width * 0.035f, 12f, 18f);
        } else if (width < 600f) {
            return clamp(width * 0.038f, 16f, 24f);
        } else {
            return clamp(width * 0.042f, 22f, 52f);
        }
    }

    private float responsiveSpacing(float baseSpacing) {
        float width = frameWidth();
        if (width < 400f) {
            return baseSpacing * 0.7f;
        } else if (width < 600f) {
            return baseSpacing * 0.85f;
        } else {
            return baseSpacing;
        }
    }

    private float cardPadding() {
        float width = frameWidth();
        if (width < 400f) {
            return 12f;
        } else if (width < 600f) {
            return 14f;
        } else {
            return 16f;
        }
    }

    private float contentInsetX() {
        return clamp(frameWidth() * 0.01f, 4f, 12f);
    }

    private float pageLeft() {
        return frameLeft() + pagePadding();
    }

    private float pageRight() {
        return frameRight() - pagePadding();
    }

    private float contentTop() {
        float availableHeight = guiHeight - footerHeight();
        if (availableHeight < 400f) {
            return 20f;
        } else if (availableHeight < 500f) {
            return availableHeight * 0.12f;
        } else {
            return availableHeight * 0.22f;
        }
    }

    private float contentBottom() {
        return guiHeight - footerHeight() - 10f;
    }

    private float contentWidth() {
        return pageRight() - pageLeft();
    }

    private float availableContentHeight() {
        return contentBottom() - contentTop();
    }

    private float columnGap() {
        return clamp(frameWidth() * 0.018f, 8f, 18f);
    }

    private boolean compactLayout() {
        return frameWidth() < 720f;
    }

    private boolean isSplitStacked() {
        return false;
    }

    private Color titleText() {
        return new Color(27, 35, 48);
    }

    private Color bodyText() {
        return new Color(110, 119, 136);
    }

    private Color mutedText() {
        return new Color(110, 119, 136);
    }

    private Color panelTitleText() {
        return new Color(24, 32, 54);
    }

    private Color accentText() {
        return new Color(104, 117, 247);
    }

    private Color inverseTitleText() {
        return new Color(245, 248, 255);
    }

    private Color inverseBodyText() {
        return new Color(245, 248, 255, 220);
    }

    private Color inverseMutedText() {
        return new Color(245, 248, 255, 190);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            return;
        }
        accountField.textboxKeyTyped(typedChar, keyCode);
        passwordField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
