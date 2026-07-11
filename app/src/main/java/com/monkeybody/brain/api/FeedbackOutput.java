package com.monkeybody.brain.api;

/** 文案与音效反馈出口；soundKey 由宿主模块映射到具体资源。 */
public interface FeedbackOutput {
    void showMessage(String message);
    void playSound(String soundKey);
    void playAnimation(String animationKey);
    void setMonkeyVisible(boolean visible);
    void hideMessage();
}
